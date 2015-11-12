package pl.touk.widerest.api.cart.controllers;


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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.touk.widerest.api.DtoConverters;
import pl.touk.widerest.api.cart.dto.CustomerDto;
import pl.touk.widerest.api.cart.exceptions.CustomerNotFoundException;
import pl.touk.widerest.api.cart.service.CustomerServiceProxy;
import pl.touk.widerest.api.catalog.exceptions.ResourceNotFoundException;
import pl.touk.widerest.security.config.ResourceServerConfig;
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
    @PreAuthorize("hasRole('PERMISSION_ALL_CUSTOMER') or #id == 'me'")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @ApiOperation(value = "Get a single customer details", response = CustomerDto.class)
    public ResponseEntity<CustomerDto> readOneCustomer(
            @ApiIgnore @AuthenticationPrincipal UserDetails userDetails,
            @ApiParam(value = "ID of a customer") @PathVariable(value = "id") String customerId
    ) {
        CustomerDto customer = Optional.ofNullable(customerId)
                .map(id -> {
                    try {
                        return ("me".equals(customerId) && userDetails instanceof CustomerUserDetails) ?
                                ((CustomerUserDetails) userDetails).getId()
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
