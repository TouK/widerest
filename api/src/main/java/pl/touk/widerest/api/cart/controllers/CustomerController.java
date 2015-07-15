package pl.touk.widerest.api.cart.controllers;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.broadleafcommerce.core.order.service.OrderService;
import org.broadleafcommerce.profile.core.service.CustomerService;
import org.broadleafcommerce.profile.core.service.UserDetailsServiceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import pl.touk.widerest.api.cart.dto.CustomerDto;
import pl.touk.widerest.api.cart.exceptions.CustomerNotFoundException;
import pl.touk.widerest.api.catalog.DtoConverters;

import javax.annotation.Resource;
import java.util.Optional;

/**
 * Created by mst on 07.07.15.
 */
@RestController
@RequestMapping(value = "/catalog/customers")
@Api(value = "customers", description = "Customer management endpoint")
public class CustomerController {

    @Resource(name="blCustomerService")
    private CustomerService customerService;

    @Resource(name="blOrderService")
    private OrderService orderService;


    @Transactional
    //@PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @ApiOperation(value = "Get a single customer details", response = CustomerDto.class)
    public ResponseEntity<CustomerDto> readOneCustomer(@PathVariable(value = "id") Long customerId) {

        CustomerDto customer = Optional.ofNullable(customerService.readCustomerById(customerId))
                           .map(DtoConverters.customerEntityToDto)
                            .orElseThrow(CustomerNotFoundException::new);
        return new ResponseEntity<>(customer, HttpStatus.OK);
    }


}
