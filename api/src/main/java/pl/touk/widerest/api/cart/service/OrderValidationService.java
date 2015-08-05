package pl.touk.widerest.api.cart.service;

import org.apache.commons.lang.StringUtils;
import org.broadleafcommerce.core.order.domain.FulfillmentGroup;
import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.order.service.FulfillmentGroupService;
import org.broadleafcommerce.profile.core.domain.Address;
import org.springframework.stereotype.Service;
import pl.touk.widerest.api.cart.exceptions.NoFulfillmentOptionException;
import pl.touk.widerest.api.cart.exceptions.NoShippingAddressException;
import pl.touk.widerest.api.cart.exceptions.OrderValidationException;

import javax.annotation.Resource;

/**
 * Created by mst on 05.08.15.
 */
@Service("wdOrderValidationService")
public class OrderValidationService {

    @Resource(name = "blFulfillmentGroupService")
    private FulfillmentGroupService fulfillmentGroupService;

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



    private void validateCustomerDataInAddress(Address address) throws OrderValidationException {
        if(StringUtils.isEmpty(address.getFirstName()) || StringUtils.isEmpty(address.getLastName())) {
            throw new OrderValidationException("Provided address does not contain First and/or Last names");
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
