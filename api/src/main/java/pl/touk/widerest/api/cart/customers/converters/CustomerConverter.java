package pl.touk.widerest.api.cart.customers.converters;

import org.broadleafcommerce.profile.core.domain.Customer;
import org.broadleafcommerce.profile.core.domain.CustomerImpl;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Component;
import pl.touk.widerest.api.Converter;
import pl.touk.widerest.api.cart.customers.CustomerController;
import pl.touk.widerest.api.cart.customers.converters.CustomerAddressConverter;
import pl.touk.widerest.api.cart.customers.dto.CustomerDto;

import javax.annotation.Resource;
import java.util.stream.Collectors;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Component
public class CustomerConverter implements Converter<Customer, CustomerDto> {

    @Resource
    private CustomerAddressConverter customerAddressConverter;

    @Override
    public CustomerDto createDto(final Customer customer, final boolean embed) {
        final CustomerDto customerDto = CustomerDto.builder()
                .customerId(customer.getId())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .deactivated(customer.isDeactivated())
                .addresses(customer.getCustomerAddresses().stream()
                        .map(customerAddress -> customerAddressConverter.createDto(customerAddress, false))
                        .collect(Collectors.toList()))
                .username(customer.getUsername())
                /* disabled due to security reasons */
//                .passwordHash(entity.getPassword())
                .registered(customer.isRegistered())
                .email(customer.getEmailAddress())
                .build();

        customerDto.add(ControllerLinkBuilder.linkTo(methodOn(CustomerController.class).readOneCustomer(null, customer.getId().toString())).withSelfRel());
        customerDto.add(linkTo(methodOn(CustomerController.class).createAuthorizationCode(null, customer.getId().toString())).withRel("authorization"));

        return customerDto;
    }

    @Override
    public Customer createEntity(final CustomerDto customerDto) {
        final Customer customer = new CustomerImpl();
        return updateEntity(customer, customerDto);
    }

    @Override
    public Customer updateEntity(final Customer customer, final CustomerDto customerDto) {
        customer.setId(customerDto.getCustomerId());
        customer.setFirstName(customerDto.getFirstName());
        customer.setLastName(customerDto.getLastName());
        customer.setRegistered(customerDto.getRegistered());
        customer.setUsername(customerDto.getUsername());
        customer.setPassword(customerDto.getPasswordHash());
        customer.setEmailAddress(customerDto.getEmail());
        customer.setCustomerAddresses(customerDto.getAddresses().stream()
                .map(customerAddressConverter::createEntity)
                .collect(Collectors.toList()));

        return customer;
    }

    @Override
    public Customer partialUpdateEntity(final Customer customer, final CustomerDto customerDto) {
        throw new UnsupportedOperationException();
    }
}
