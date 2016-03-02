package pl.touk.widerest.api.cart.customers.converters;


import org.broadleafcommerce.common.i18n.domain.ISOCountry;
import org.broadleafcommerce.common.i18n.service.ISOService;
import org.broadleafcommerce.profile.core.domain.Address;
import org.broadleafcommerce.profile.core.domain.AddressImpl;
import org.springframework.stereotype.Component;
import pl.touk.widerest.api.Converter;
import pl.touk.widerest.api.cart.customers.dto.AddressDto;

import javax.annotation.Resource;
import java.util.Optional;

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
                .countryCode(Optional.ofNullable(address.getIsoCountryAlpha2()).map(ISOCountry::getAlpha2).orElse(null))
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
        address.setIsoCountrySubdivision(addressDto.getCountrySubdivisionCode());

        Optional.ofNullable(addressDto.getCountryCode())
                .ifPresent(countryCode -> address.setIsoCountryAlpha2(isoService.findISOCountryByAlpha2Code(countryCode)));

        return address;
    }

    @Override
    public Address partialUpdateEntity(final Address address, final AddressDto addressDto) {
        throw new UnsupportedOperationException();
    }
}
