package pl.touk.widerest.api.cart.controllers;


import com.google.common.collect.ImmutableMap;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.broadleafcommerce.openadmin.server.security.service.AdminUserDetails;
import org.broadleafcommerce.profile.core.domain.Customer;
import org.broadleafcommerce.profile.core.service.CustomerService;
import org.broadleafcommerce.profile.core.service.CustomerUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.common.util.OAuth2Utils;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerEndpointsConfiguration;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.OAuth2RequestFactory;
import org.springframework.security.oauth2.provider.code.AuthorizationCodeServices;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.touk.widerest.api.DtoConverters;
import pl.touk.widerest.api.cart.dto.CustomerDto;
import pl.touk.widerest.api.cart.exceptions.CustomerNotFoundException;
import pl.touk.widerest.api.cart.service.CustomerServiceProxy;
import pl.touk.widerest.api.catalog.exceptions.ResourceNotFoundException;
import pl.touk.widerest.security.authentication.AnonymousUserDetailsService;
import pl.touk.widerest.security.authentication.SiteAuthenticationToken;
import pl.touk.widerest.security.config.ResourceServerConfig;
import pl.touk.widerest.security.oauth2.Scope;
import springfox.documentation.annotations.ApiIgnore;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = ResourceServerConfig.API_PATH + "/customers")
@Api(value = "customers", description = "Customer management endpoint")
public class CustomerController {

    @Resource(name="blCustomerService")
    private CustomerService customerService;

    @Resource(name = "wdCustomerService")
    private CustomerServiceProxy customerServiceProxy;

    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_CUSTOMER') or #id == 'me' or #id == #customerUserDetails.id")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @ApiOperation(value = "Get a single customer details", response = CustomerDto.class)
    public ResponseEntity<CustomerDto> readOneCustomer(
            @ApiIgnore @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
            @ApiParam(value = "ID of a customer") @PathVariable(value = "id") String customerId
    ) {
        CustomerDto customer = Optional.ofNullable(customerId)
                .map(id -> {
                    try {
                        return ("me".equals(customerId) && customerUserDetails != null) ?
                                ((CustomerUserDetails) customerUserDetails).getId()
                                : Long.parseLong(customerId);
                    } catch (NumberFormatException ex) {
                        return null;
                    }
                })
                .map(customerService::readCustomerById)
                .map(DtoConverters.customerEntityToDto)
                .orElseThrow(CustomerNotFoundException::new);

        return new ResponseEntity<>(customer, HttpStatus.OK);
    }

    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_CUSTOMER') or #id == 'me' or #id == #customerUserDetails.id")
    @RequestMapping(value = "/{id}/email", method = RequestMethod.PUT)
    @ApiOperation(value = "Update customer's email")
    public void updateCustomerEmail(
            @ApiIgnore @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
            @ApiParam(value = "ID of a customer") @PathVariable(value = "id") String customerId,
            @RequestBody String email
    ) {
        Optional.ofNullable(customerId)
                .map(id -> {
                    try {
                        return ("me".equals(customerId) && customerUserDetails != null) ?
                                customerUserDetails.getId()
                                : Long.parseLong(customerId);
                    } catch (NumberFormatException ex) {
                        return null;
                    }
                })
                .map(customerService::readCustomerById)
                .map(customer -> { customer.setEmailAddress(email); return customer; })
                .orElseThrow(CustomerNotFoundException::new);
    }

    @PreAuthorize("hasRole('PERMISSION_ALL_CUSTOMER') or #id == 'me' or #id == #customerUserDetails.id")
    @RequestMapping(value = "/{id}/authorization", method = RequestMethod.POST)
    @ApiOperation(value = "Update customer's email")
    public String createAuthorizationCode(
            @ApiIgnore @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
            @ApiParam(value = "ID of a customer") @PathVariable(value = "id") String customerId
    ) {
        String code = Optional.ofNullable(customerId)
                .map(id -> {
                    try {
                        return ("me".equals(customerId) && customerUserDetails != null) ?
                                ((CustomerUserDetails) customerUserDetails).getId()
                                : Long.parseLong(customerId);
                    } catch (NumberFormatException ex) {
                        return null;
                    }
                })
                .map(customerService::readCustomerById)
                .map(this::generateCode)
                .orElseThrow(CustomerNotFoundException::new);
        return code;
    }

    @Resource
    private AuthorizationCodeServices authorizationCodeServices;

    @Resource
    private AnonymousUserDetailsService customerUserDetailsService;

    @Resource
    private AuthorizationServerEndpointsConfiguration authorizationServerEndpointsConfiguration;

    private String generateCode(Customer customer) throws AuthenticationException {

        OAuth2Request oAuth2Request = ((OAuth2Authentication) SecurityContextHolder.getContext().getAuthentication()).getOAuth2Request();

        OAuth2RequestFactory oAuth2RequestFactory =
                authorizationServerEndpointsConfiguration
                        .getEndpointsConfigurer()
                        .getOAuth2RequestFactory();

        OAuth2Request storedOAuth2Request = oAuth2RequestFactory.createOAuth2Request(
                oAuth2RequestFactory.createAuthorizationRequest(
                        ImmutableMap.<String, String>builder()
                                .put(OAuth2Utils.SCOPE, Scope.CUSTOMER.toString())
                                .put(OAuth2Utils.CLIENT_ID, oAuth2Request.getClientId())
                                .build()
                )
        );

        UserDetails customerUserDetails = customerUserDetailsService.createCustomerUserDetails(customer);
        OAuth2Authentication combinedAuth = new OAuth2Authentication(storedOAuth2Request, new SiteAuthenticationToken(
                customerUserDetails, null, customerUserDetails.getAuthorities()
        ));
        String code = authorizationCodeServices.createAuthorizationCode(combinedAuth);

        return code;
    }


    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_CUSTOMER')")
    @RequestMapping(method = RequestMethod.GET)
    @ApiOperation(
            value = "List all customers",
            notes = "Gets a list of all currently active customers",
            response = CustomerDto.class,
            responseContainer = "List"
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of customers list", response = CustomerDto.class, responseContainer = "List")
    })
    public List<CustomerDto> readAllCustomers(
            @ApiIgnore @AuthenticationPrincipal UserDetails userDetails
    ) {
        if(userDetails instanceof AdminUserDetails) {
            return customerServiceProxy.getAllCustomers().stream()
                    .map(DtoConverters.customerEntityToDto)
                    .collect(Collectors.toList());
        } else if(userDetails instanceof CustomerUserDetails) {
            List<CustomerDto> list = new ArrayList<>();
            list.add(DtoConverters.customerEntityToDto.apply(
                    customerServiceProxy.getCustomerById(((CustomerUserDetails) userDetails).getId())
            ));
            return list;
        }

        return null;
    }

    @Transactional
    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public void registerCustomer(
            @ApiIgnore @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String passwordConfirm,
            @RequestParam String email
    )  {
        // Assuming user already has token

        Optional.ofNullable(customerUserDetails.getId())
                .map(customerService::readCustomerById)
                .map(Customer::isRegistered)
                .map(e -> e ? null: e)
                .orElseThrow(() -> new ResourceNotFoundException("User already registered"));

        Optional.of(username)
                .map(customerService::readCustomerByUsername)
                .ifPresent(e -> {
                    throw new ResourceNotFoundException("Username already taken, please try with other");
                });

        Optional.of(email)
                .map(customerService::readCustomerByEmail)
                .ifPresent(e -> {
                    throw new ResourceNotFoundException("Email address already taken, please try with other");
                });

        Customer customer = customerService.readCustomerById(customerUserDetails.getId());
        customer.setUsername(username);
        customer.setEmailAddress(email);

        customerService.registerCustomer(customer, password, passwordConfirm);

    }

    @Transactional
    @RequestMapping(value = "{id}/merge", method = RequestMethod.PUT)
    public void mergeWith(
            @ApiIgnore @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
            String mergedCustomerAccessToken
    ) {

    }
}
