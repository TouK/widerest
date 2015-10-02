package pl.touk.widerest.api.cart.service;

import org.apache.commons.lang.StringUtils;
import org.broadleafcommerce.common.i18n.service.ISOService;
import org.broadleafcommerce.core.order.domain.FulfillmentGroup;
import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.order.service.FulfillmentGroupService;
import org.broadleafcommerce.profile.core.domain.Address;
import org.broadleafcommerce.profile.core.service.CountryService;
import org.springframework.stereotype.Service;
import pl.touk.widerest.api.cart.dto.AddressDto;
import pl.touk.widerest.api.cart.exceptions.NoFulfillmentOptionException;
import pl.touk.widerest.api.cart.exceptions.NoShippingAddressException;
import pl.touk.widerest.api.cart.exceptions.OrderValidationException;

import javax.annotation.Resource;

@Service("wdOrderValidationService")
public class OrderValidationService {

    @Resource(name = "blFulfillmentGroupService")
    private FulfillmentGroupService fulfillmentGroupService;

    @Resource(name = "blCountryService")
    private CountryService countryService;

    @Resource
    private ISOService isoService;

    public void validateOrderBeforeCheckout(Order order) throws OrderValidationException {

        validateOrderNotEmpty(order);
        validateFulfillmentOption(order);
        validateFulfillmentAddresses(order);

    }


    private void validateOrderNotEmpty(Order order) throws OrderValidationException {
        if(order.getItemCount() <= 0) {
            throw new OrderValidationException("Order with ID: " + order.getId() + " is empty");
        }
    }


    public void validateCustomerDataInAddress(Address address) throws OrderValidationException {
        if(StringUtils.isEmpty(address.getFirstName()) || StringUtils.isEmpty(address.getLastName())) {
            throw new OrderValidationException("Provided address does not contain First and/or Last names");
        }
        if(StringUtils.isEmpty(address.getAddressLine1())){
            throw new OrderValidationException("Provided address does not contain address lines");
        }
        if(StringUtils.isEmpty(address.getPostalCode()) || StringUtils.isEmpty(address.getCity())) {
            throw new OrderValidationException("Provided address does not contain postal code and/or city");
        }
        if(StringUtils.isEmpty(address.getIsoCountrySubdivision())
                || countryService.findCountryByAbbreviation(address.getIsoCountrySubdivision()) == null) {
            throw new OrderValidationException("Provided address does not contain valid country code");
        }
    }

    public void validateAddressDto(AddressDto address) throws OrderValidationException {
        if(StringUtils.isEmpty(address.getFirstName()) || StringUtils.isEmpty(address.getLastName())) {
            throw new OrderValidationException("Provided address does not contain First and/or Last names");
        }
        if(StringUtils.isEmpty(address.getAddressLine1())){
            throw new OrderValidationException("Provided address does not contain address lines");
        }
        if(StringUtils.isEmpty(address.getPostalCode()) || StringUtils.isEmpty(address.getCity())) {
            throw new OrderValidationException("Provided address does not contain postal code and/or city");
        }
        if(StringUtils.isEmpty(address.getCountryAbbreviation())
                || isoService.findISOCountryByAlpha2Code(address.getCountryAbbreviation()) == null) {
            throw new OrderValidationException("Provided address does not contain valid country code");
        }
    }

    private void validateFulfillmentAddresses(Order order) {
        FulfillmentGroup fulfillmentGroup = fulfillmentGroupService.getFirstShippableFulfillmentGroup(order);

        if(fulfillmentGroup != null && fulfillmentGroup.getAddress() == null) {
            throw new NoShippingAddressException("Shipping address for order with ID: " + order.getId() + " has not been provided");
        }

        validateCustomerDataInAddress(fulfillmentGroup.getAddress());
    }


    private void validateFulfillmentOption(Order order) throws OrderValidationException {
        FulfillmentGroup fulfillmentGroup = fulfillmentGroupService.getFirstShippableFulfillmentGroup(order);

        if(fulfillmentGroup != null && fulfillmentGroup.getFulfillmentOption() != null) {
            throw new NoFulfillmentOptionException("FulfillmentOption for order with ID: " + order.getId() + " has not been provided");
        }
    }

}
