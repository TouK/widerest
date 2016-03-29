package pl.touk.widerest.api.customers;


import com.google.common.collect.ImmutableMap;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javaslang.control.Match;
import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.order.service.MergeCartService;
import org.broadleafcommerce.core.order.service.OrderService;
import org.broadleafcommerce.core.order.service.exception.RemoveFromCartException;
import org.broadleafcommerce.core.pricing.service.exception.PricingException;
import org.broadleafcommerce.openadmin.server.security.service.AdminUserDetails;
import org.broadleafcommerce.profile.core.domain.Customer;
import org.broadleafcommerce.profile.core.service.CustomerService;
import org.broadleafcommerce.profile.core.service.CustomerUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.common.util.OAuth2Utils;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.OAuth2RequestFactory;
import org.springframework.security.oauth2.provider.code.AuthorizationCodeServices;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.touk.widerest.api.common.ResourceNotFoundException;
import pl.touk.widerest.api.products.ProductDto;
import pl.touk.widerest.security.authentication.AnonymousUserDetailsService;
import pl.touk.widerest.security.authentication.SiteAuthenticationToken;
import pl.touk.widerest.security.oauth2.ResourceServerConfig;
import pl.touk.widerest.security.oauth2.Scope;
import pl.touk.widerest.security.oauth2.oob.OobAuthorizationServerEndpointsConfiguration;
import springfox.documentation.annotations.ApiIgnore;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static com.jasongoodwin.monads.Try.ofFailable;
import static java.lang.Long.parseLong;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;
import static org.springframework.security.core.context.SecurityContextHolder.getContext;

@RestController
@RequestMapping(value = ResourceServerConfig.API_PATH + "/customers", produces = { MediaTypes.HAL_JSON_VALUE })
@Api(value = "customers", description = "Customer management endpoint", produces = MediaTypes.HAL_JSON_VALUE)
public class CustomerController {

    @Resource(name="blCustomerService")
    private CustomerService customerService;

    @Resource(name = "blOrderService")
    private OrderService orderService;

    @Resource(name = "blMergeCartService")
    private MergeCartService mergeCartService;

    @Resource(name = "wdCustomerService")
    private CustomerServiceProxy customerServiceProxy;

    @Resource
    private AuthorizationCodeServices authorizationCodeServices;

    @Resource
    private AnonymousUserDetailsService customerUserDetailsService;

    @Resource
    private OobAuthorizationServerEndpointsConfiguration authorizationServerEndpointsConfiguration;

    @Autowired
    private ResourceServerTokenServices tokenServices;

    @Resource
    private CustomerConverter customerConverter;


    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_CUSTOMER')")
    @RequestMapping(method = RequestMethod.GET)
    @ApiOperation(
            value = "List all customers",
            notes = "Gets a list of all active customers if the currently logged in user has admin rights. Otherwise, it simply" +
                    " returns details of a currently logged in customer",
            response = CustomerDto.class,
            responseContainer = "List"
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of customers list", response = CustomerDto.class, responseContainer = "List")
    })
    public Resources<CustomerDto> readAllCustomers(@ApiIgnore @AuthenticationPrincipal UserDetails userDetails) {
        final List<CustomerDto> allCustomers = Match.of(userDetails)
                .whenType(AdminUserDetails.class).then(() -> customerServiceProxy.getAllCustomers().stream()
                        .map(customer -> customerConverter.createDto(customer))
                        .collect(Collectors.toList()))
                .whenType(CustomerUserDetails.class).then(() -> Optional.ofNullable(customerServiceProxy.getCustomerById(((CustomerUserDetails) userDetails).getId()))
                        //.map(id -> customerEntityToDto.apply(id))
                        .map(id -> customerConverter.createDto(id))
                        .map(Collections::singletonList)
                        .orElse(emptyList()))
                .otherwise(Collections::emptyList)
                .get();

        return new Resources<>(
                allCustomers,
                linkTo(methodOn(getClass()).readAllCustomers(null)).withSelfRel()
        );
    }

    @Transactional
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @PreAuthorize("hasRole('PERMISSION_ALL_CUSTOMER') or #customerId == 'me' or #customerId == T(java.lang.Long).toString(#customerUserDetails.id)")
    @ApiOperation(
            value = "Get single customer details",
            notes = "Retrieves single customer details",
            response = CustomerDto.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of product", response = ProductDto.class),
            @ApiResponse(code = 404, message = "The specified customer does not exist")
    })
    public ResponseEntity<CustomerDto> readOneCustomer(
            @ApiIgnore @AuthenticationPrincipal final CustomerUserDetails customerUserDetails,
            @ApiParam(value = "ID of a customer", required = true)
                @PathVariable(value = "id") final String customerId
    ) {
        return ofNullable(customerId)
                .map(toCustomerId(customerUserDetails, customerId))
                .map(customerService::readCustomerById)
                .map(customer -> customerConverter.createDto(customer))
                .map(dto -> new ResponseEntity<>(dto, HttpStatus.OK))
                .orElseThrow(CustomerNotFoundException::new);
    }

    @Transactional
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    @PreAuthorize("hasRole('PERMISSION_ALL_CUSTOMER') or #customerId == 'me' or #customerId == T(java.lang.Long).toString(#customerUserDetails.id)")
    @ApiOperation(
            value = "Get single customer details",
            notes = "Retrieves single customer details",
            response = CustomerDto.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of product", response = ProductDto.class),
            @ApiResponse(code = 404, message = "The specified customer does not exist")
    })
    public void updateOneCustomer(
            @ApiIgnore @AuthenticationPrincipal final CustomerUserDetails customerUserDetails,
            @ApiParam(value = "ID of a customer", required = true) @PathVariable(value = "id") final String customerId,
            @Valid @RequestBody CustomerDto customerDto
    ) {
        ofNullable(customerId)
                .map(toCustomerId(customerUserDetails, customerId))
                .map(customerService::readCustomerById)
                .map(customer -> customerConverter.updateEntity(customer, customerDto))
                .map(customerService::saveCustomer)
                .orElseThrow(CustomerNotFoundException::new);
    }

    @RequestMapping(value = "/{id}/authorization", method = RequestMethod.POST)
    @PreAuthorize("hasRole('PERMISSION_ALL_CUSTOMER') or #customerId  == 'me' or #customerId == #customerUserDetails.id")
    @ApiOperation(
            value = "Create an authorization code",
            notes = "Creates a new authorization code for a specified customer",
            response = String.class
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Authorization code for the specified customer has been created", response = String.class),
            @ApiResponse(code = 404, message = "The specified customer does not exist")
    })
    public ResponseEntity createAuthorizationCode(
            @ApiIgnore @AuthenticationPrincipal final CustomerUserDetails customerUserDetails,
            @ApiParam(value = "ID of a customer", required = true)
                @PathVariable(value = "id") final String customerId
    ) {
        return ofNullable(customerId)
                .map(toCustomerId(customerUserDetails, customerId))
                .map(customerService::readCustomerById)
                .map(this::generateCode)
                .map(ResponseEntity::ok)
                .orElseThrow(CustomerNotFoundException::new);
    }


    @Transactional
    @RequestMapping(value = "/register", method = RequestMethod.POST)
    @ApiOperation(
            value = "Register a new customer",
            notes = "Registers a new customer with provided credentials",
            response = Void.class
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "New customer successfully registered", response = Void.class),
            @ApiResponse(code = 404, message = "Specified customer already exists or provided credentials (email, username) are already taken")
    })
    public void registerCustomer(
            @ApiIgnore @AuthenticationPrincipal final CustomerUserDetails customerUserDetails,
            @RequestParam final String username,
            @RequestParam final String password,
            @RequestParam final String passwordConfirm,
            @RequestParam final String email
    )  {
        // Assuming user already has token
        ofNullable(customerUserDetails.getId())
                .map(customerService::readCustomerById)
                .filter(c -> !c.isRegistered())
                .orElseThrow(() -> new ResourceNotFoundException("User already registered"));

        Optional.of(username)
                .filter(customerName -> isNull(customerService.readCustomerByUsername(customerName)))
                .orElseThrow(() -> new ResourceNotFoundException("Username already taken, please try with other"));

        Optional.of(email)
                .filter(e -> isNull(customerService.readCustomerByEmail(e)))
                .orElseThrow(() -> new ResourceNotFoundException("Email address already taken, please try with other"));

        final Customer customer = customerService.readCustomerById(customerUserDetails.getId());
        customer.setUsername(username);
        customer.setEmailAddress(email);

        customerService.registerCustomer(customer, password, passwordConfirm);
    }

    @Transactional
    @RequestMapping(value = "/merge", method = RequestMethod.POST)
    @ApiOperation(
            value = "Merge carts",
            notes = "Merges anonymous cart with a logged in customer's cart",
            response = Void.class
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Carts have been successfully merged", response = Void.class)
    })
    public void mergeWithAnonymous(
            @ApiIgnore @AuthenticationPrincipal final CustomerUserDetails userDetails,
            @RequestBody final String anonymousToken) throws RemoveFromCartException, PricingException {

        final Customer loggedUser = customerService.readCustomerById(userDetails.getId());
        final CustomerUserDetails anonymousUserDetails = (CustomerUserDetails) tokenServices.loadAuthentication
                (anonymousToken).getPrincipal();

        final Customer anonymousUser = customerService.readCustomerById(anonymousUserDetails.getId());

        final Order anonymousCart = orderService.findCartForCustomer(anonymousUser);

        mergeCartService.mergeCart(loggedUser, anonymousCart);
    }


    private static Function<String, Long> toCustomerId(CustomerUserDetails customerUserDetails, String customerId) {
        return id -> Optional.ofNullable(customerUserDetails)
                .filter(ud -> "me".equals(customerId))
                .map(CustomerUserDetails::getId)
                .orElse(ofFailable(() -> parseLong(customerId)).orElse(null));
    }

    private static UnaryOperator<Customer> toCustomerWithEmail(final String email) {
        return customer -> {
            customer.setEmailAddress(email);
            return customer;
        };
    }

    private String generateCode(Customer customer) throws AuthenticationException {

        final String clientId = ((OAuth2Authentication) getContext().getAuthentication())
                .getOAuth2Request().getClientId();

        final OAuth2RequestFactory oAuth2RequestFactory =
                authorizationServerEndpointsConfiguration
                        .getEndpointsConfigurer()
                        .getOAuth2RequestFactory();

        final OAuth2Request storedOAuth2Request = oAuth2RequestFactory.createOAuth2Request(
                oAuth2RequestFactory.createAuthorizationRequest(
                        ImmutableMap.<String, String>builder()
                                .put(OAuth2Utils.SCOPE, Scope.CUSTOMER.toString())
                                .put(OAuth2Utils.CLIENT_ID, clientId)
                                .build()
                )
        );

        final UserDetails customerUserDetails = customerUserDetailsService.createCustomerUserDetails(customer);
        final OAuth2Authentication combinedAuth = new OAuth2Authentication(storedOAuth2Request, new
                SiteAuthenticationToken(customerUserDetails, null, customerUserDetails.getAuthorities()
        ));

        return authorizationCodeServices.createAuthorizationCode(combinedAuth);
    }
}
