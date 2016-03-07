package pl.touk.widerest.api.customers;

import org.broadleafcommerce.profile.core.domain.CustomerAddress;
import org.broadleafcommerce.profile.core.domain.CustomerAddressImpl;
import org.springframework.stereotype.Component;
import pl.touk.widerest.api.Converter;
import pl.touk.widerest.api.common.AddressConverter;

import javax.annotation.Resource;

@Component
public class CustomerAddressConverter implements Converter<CustomerAddress, CustomerAddressDto> {

    @Resource
    private AddressConverter addressConverter;

    @Override
    public CustomerAddressDto createDto(final CustomerAddress customerAddress, final boolean embed) {
        return CustomerAddressDto.builder()
                .addressName(customerAddress.getAddressName())
                .addressDto(addressConverter.createDto(customerAddress.getAddress(), false))
                .build();
    }

    @Override
    public CustomerAddress createEntity(final CustomerAddressDto customerAddressDto) {
        return updateEntity(new CustomerAddressImpl(), customerAddressDto);
    }

    @Override
    public CustomerAddress updateEntity(final CustomerAddress customerAddress, final CustomerAddressDto customerAddressDto) {
        customerAddress.setAddress(addressConverter.createEntity(customerAddressDto.getAddressDto()));
        customerAddress.setAddressName(customerAddressDto.getAddressName());
        return customerAddress;
    }
}
