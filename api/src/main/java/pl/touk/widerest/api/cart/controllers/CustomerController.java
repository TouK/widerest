package pl.touk.widerest.api.cart.controllers;

import com.wordnik.swagger.annotations.ApiOperation;
import org.broadleafcommerce.profile.core.domain.CustomerAddress;
import org.broadleafcommerce.profile.core.service.CustomerService;
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
import pl.touk.widerest.api.catalog.exceptions.ResourceNotFoundException;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by mst on 07.07.15.
 */
@RestController
@RequestMapping(value = "/customers")
public class CustomerController {

    @Resource(name="blCustomerService")
    private CustomerService customerService;


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
