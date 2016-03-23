package pl.touk.widerest.api.customers;

import org.broadleafcommerce.common.locale.domain.Locale;
import org.broadleafcommerce.profile.core.domain.Customer;
import org.broadleafcommerce.profile.core.domain.CustomerAddress;
import org.broadleafcommerce.profile.core.service.CustomerAddressService;
import org.broadleafcommerce.profile.core.service.CustomerService;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Component;
import pl.touk.widerest.api.Converter;
import pl.touk.widerest.api.common.AddressConverter;

import javax.annotation.Resource;
import java.util.Optional;

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
    public CustomerDto createDto(final Customer customer, final boolean embed, final boolean link) {

        final CustomerDto customerDto = CustomerDto.builder()
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .username(customer.getUsername())
                .email(customer.getEmailAddress())
                .locale(Optional.ofNullable(customer.getCustomerLocale()).map(Locale::getLocaleCode).orElse(null))
                .addresses(
                        customer.getCustomerAddresses().stream()
                                .collect(toMap(
                                        CustomerAddress::getAddressName,
                                        customerAddress -> addressConverter.createDto(customerAddress.getAddress(), embed, link)
                                ))
                )
                .build();

        customerDto.add(ControllerLinkBuilder.linkTo(methodOn(CustomerController.class).readOneCustomer(null, customer.getId().toString())).withSelfRel());

        if (link) {
            customerDto.add(linkTo(methodOn(CustomerController.class).createAuthorizationCode(null, customer.getId().toString())).withRel("authorization"));
        }

        return customerDto;
    }

    @Override
    public Customer createEntity(final CustomerDto customerDto) {
        return updateEntity(customerService.createCustomer(), customerDto);
    }

    @Override
    public Customer updateEntity(final Customer customer, final CustomerDto customerDto) {

        if (customerDto.getUsername() != null) {
            throw new java.lang.IllegalArgumentException("Username may be assigned by registering only");
        }

        customer.setFirstName(customerDto.getFirstName());
        customer.setLastName(customerDto.getLastName());
        customer.setEmailAddress(customerDto.getEmail());

       // customer.setRegistered(customerDto.getRegistered()); // -> false

        customer.getCustomerAddresses().clear();

        Optional.ofNullable(customerDto.getAddresses())
                .map(addresses -> addresses.entrySet().stream())
                .map(stream -> stream
                        .map(entry -> {
                            CustomerAddress customerAddress = customerAddressService.create();
                            customerAddress.setAddressName(entry.getKey());
                            customerAddress.setAddress(addressConverter.createEntity(entry.getValue()));
                            customerAddress.setCustomer(customer);
                            return customerAddress;
                        })
                        .collect(toList())
                )
                .ifPresent(customerAddresses -> customer.getCustomerAddresses().addAll(customerAddresses));

        return customer;
    }
}
