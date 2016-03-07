package pl.touk.widerest.api.customers;

import org.broadleafcommerce.profile.core.domain.Customer;
import org.broadleafcommerce.profile.core.domain.CustomerImpl;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Component;
import pl.touk.widerest.api.Converter;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Component
public class CustomerConverter implements Converter<Customer, CustomerDto> {

    @Resource
    private CustomerAddressConverter customerAddressConverter;

    @Override
    public CustomerDto createDto(final Customer customer, final boolean embed) {
        final CustomerDto customerDto = CustomerDto.builder()
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .deactivated(customer.isDeactivated())
                .username(customer.getUsername())
                .registered(customer.isRegistered())
                .email(customer.getEmailAddress())
                .addresses(
                        Optional.ofNullable(customer.getCustomerAddresses()).orElse(Collections.emptyList()).stream()
                            .map(customerAddress -> customerAddressConverter.createDto(customerAddress, false))
                            .collect(toList())
                )
                .build();

        customerDto.add(ControllerLinkBuilder.linkTo(methodOn(CustomerController.class).readOneCustomer(null, customer.getId().toString())).withSelfRel());

        customerDto.add(linkTo(methodOn(CustomerController.class).createAuthorizationCode(null, customer.getId().toString())).withRel("authorization"));

        return customerDto;
    }

    @Override
    public Customer createEntity(final CustomerDto customerDto) {
        return updateEntity(new CustomerImpl(), customerDto);
    }

    @Override
    public Customer updateEntity(final Customer customer, final CustomerDto customerDto) {
        customer.setFirstName(customerDto.getFirstName());
        customer.setLastName(customerDto.getLastName());
        customer.setRegistered(customerDto.getRegistered());
        customer.setUsername(customerDto.getUsername());
        customer.setEmailAddress(customerDto.getEmail());

        customer.setCustomerAddresses(
                Optional.ofNullable(customerDto.getAddresses()).orElse(Collections.emptyList()).stream()
                        .map(customerAddressConverter::createEntity)
                        .collect(toList())
        );

        return customer;
    }
}
