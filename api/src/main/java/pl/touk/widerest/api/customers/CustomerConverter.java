package pl.touk.widerest.api.customers;

import org.broadleafcommerce.profile.core.domain.Customer;
import org.broadleafcommerce.profile.core.domain.CustomerAddress;
import org.broadleafcommerce.profile.core.domain.CustomerAddressImpl;
import org.broadleafcommerce.profile.core.domain.CustomerImpl;
import org.broadleafcommerce.profile.core.service.CustomerAddressService;
import org.broadleafcommerce.profile.core.service.CustomerService;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Component;
import pl.touk.widerest.api.Converter;
import pl.touk.widerest.api.common.AddressConverter;
import pl.touk.widerest.api.common.AddressDto;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Component
public class CustomerConverter implements Converter<Customer, CustomerDto> {

    @Resource
    private AddressConverter addressConverter;

    @Resource
    private CustomerAddressService customerAddressService;

    @Resource
    private CustomerService customerService;

    @Override
    public CustomerDto createDto(final Customer customer, final boolean embed) {

        final CustomerDto customerDto = CustomerDto.builder()
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .username(customer.getUsername())
                .email(customer.getEmailAddress())
                .addresses(
                        customer.getCustomerAddresses().stream()
                                .collect(toMap(
                                        CustomerAddress::getAddressName,
                                        customerAddress -> addressConverter.createDto(customerAddress.getAddress(), embed)
                                ))
                )
                .build();

        customerDto.add(ControllerLinkBuilder.linkTo(methodOn(CustomerController.class).readOneCustomer(null, customer.getId().toString())).withSelfRel());

        customerDto.add(linkTo(methodOn(CustomerController.class).createAuthorizationCode(null, customer.getId().toString())).withRel("authorization"));

        return customerDto;
    }

    @Override
    public Customer createEntity(final CustomerDto customerDto) {
        return updateEntity(customerService.createCustomer(), customerDto);
    }

    @Override
    public Customer updateEntity(final Customer customer, final CustomerDto customerDto) {
        customer.setFirstName(customerDto.getFirstName());
        customer.setLastName(customerDto.getLastName());
        customer.setUsername(customerDto.getUsername());
        customer.setEmailAddress(customerDto.getEmail());

       // customer.setRegistered(customerDto.getRegistered()); // -> false


        customer.setCustomerAddresses(
                Optional.ofNullable(customerDto.getAddresses())
                        .map(addresses -> addresses.entrySet().stream())
                        .map(stream -> stream
                                .map(entry -> {
                                    CustomerAddress customerAddress = customerAddressService.create();
                                    customerAddress.setAddressName(entry.getKey());
                                    customerAddress.setAddress(addressConverter.createEntity(entry.getValue()));
                                    return customerAddress;
                                })
                                .collect(toList())
                        ).orElse(Collections.emptyList())
        );

        return customer;
    }
}
