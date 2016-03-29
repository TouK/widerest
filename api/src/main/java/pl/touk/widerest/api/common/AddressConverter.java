package pl.touk.widerest.api.common;


import org.broadleafcommerce.common.i18n.domain.ISOCountry;
import org.broadleafcommerce.common.i18n.service.ISOService;
import org.broadleafcommerce.profile.core.domain.Address;
import org.broadleafcommerce.profile.core.service.AddressService;
import org.springframework.stereotype.Component;
import pl.touk.widerest.api.Converter;

import javax.annotation.Resource;
import java.util.Optional;

@Component
public class AddressConverter implements Converter<Address, AddressDto> {

    @Resource
    protected ISOService isoService;

    @Resource
    protected AddressService addressService;

    @Override
    public AddressDto createDto(final Address address, final boolean embed, final boolean link) {
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
                .email(address.getEmailAddress())
                .build();
    }

    @Override
    public Address createEntity(final AddressDto addressDto) {
        Address address = addressService.create();
        address.setCountry(null);
        return updateEntity(address, addressDto);
    }

    @Override
    public Address updateEntity(final Address address, final AddressDto addressDto) {
        address.setAddressLine1(Optional.ofNullable(addressDto.getAddressLine1()).orElse(""));
        address.setAddressLine2(addressDto.getAddressLine2());
        address.setAddressLine3(addressDto.getAddressLine3());

        address.setFirstName(addressDto.getFirstName());
        address.setLastName(addressDto.getLastName());

        address.setCity(Optional.ofNullable(addressDto.getCity()).orElse(""));
        address.setPostalCode(addressDto.getPostalCode());
        address.setCompanyName(addressDto.getCompanyName());

        address.setCounty(addressDto.getCountrySubdivisionCode());
        address.setIsoCountrySubdivision(addressDto.getCountrySubdivisionCode());

        Optional.ofNullable(addressDto.getCountryCode())
                .ifPresent(countryCode -> address.setIsoCountryAlpha2(isoService.findISOCountryByAlpha2Code(countryCode)));

        address.setEmailAddress(addressDto.getEmail());

        return address;
    }
}
