package pl.touk.widerest.api.cart.customers;


import org.broadleafcommerce.common.i18n.service.ISOService;
import org.broadleafcommerce.profile.core.domain.Address;
import org.broadleafcommerce.profile.core.domain.AddressImpl;
import org.springframework.stereotype.Component;
import pl.touk.widerest.api.Converter;
import pl.touk.widerest.api.cart.dto.AddressDto;

import javax.annotation.Resource;

@Component
public class AddressConverter implements Converter<Address, AddressDto> {

    @Resource
    protected ISOService isoService;

    @Override
    public AddressDto createDto(final Address address, final boolean embed) {
        return AddressDto.builder()
                .addressLine1(address.getAddressLine1())
                .addressLine2(address.getAddressLine2())
                .addressLine3(address.getAddressLine3())
                .firstName(address.getFirstName())
                .lastName(address.getLastName())
                .city(address.getCity())
                .postalCode(address.getPostalCode())
                .companyName(address.getCompanyName())
                .countryCode(address.getIsoCountryAlpha2().getAlpha2())
                .countrySubdivisionCode(address.getIsoCountrySubdivision())
                .build();
    }

    @Override
    public Address createEntity(final AddressDto addressDto) {
        final Address addressEntity = new AddressImpl();
        return updateEntity(addressEntity, addressDto);
    }

    @Override
    public Address updateEntity(final Address address, final AddressDto addressDto) {
        address.setAddressLine1(addressDto.getAddressLine1());
        address.setAddressLine2(addressDto.getAddressLine2());
        address.setAddressLine3(addressDto.getAddressLine3());
        address.setFirstName(addressDto.getFirstName());
        address.setLastName(addressDto.getLastName());
        address.setCity(addressDto.getCity());
        address.setPostalCode(addressDto.getPostalCode());
        address.setCompanyName(addressDto.getCompanyName());
        address.setCounty(addressDto.getCountrySubdivisionCode());
        address.setIsoCountryAlpha2(isoService.findISOCountryByAlpha2Code(addressDto.getCountryCode()));
        return address;
    }

    @Override
    public Address partialUpdateEntity(final Address address, final AddressDto addressDto) {
        throw new UnsupportedOperationException();
    }
}
