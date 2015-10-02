package pl.touk.widerest.api.cart;

import org.broadleafcommerce.core.order.domain.FulfillmentGroup;
import org.broadleafcommerce.core.order.domain.FulfillmentOption;
import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.order.service.type.FulfillmentType;

import java.util.List;
import java.util.Optional;

/**
 * Created by mst on 31.07.15.
 */
public class CartUtils {

    public static FulfillmentOption getFulfilmentOption(Order order) {
        return Optional.ofNullable(getFirstShippableFulfilmentGroup(order))
                .map(FulfillmentGroup::getFulfillmentOption)
                .orElse(null);

        /*
         FulfillmentGroup fulfillmentGroup = getFirstShippableFulfillmentGroup(order);
        return fulfillmentGroup != null ? fulfillmentGroup.getFulfillmentOption() : null;
         */
    }

    public static FulfillmentGroup getFirstShippableFulfilmentGroup(Order order) {

        /*
        return order.getFulfillmentGroups().stream()
                .map(FulfillmentGroup::getType)
                .filter(CartUtils::isShippable)
                .findFirst()
                .
*/
        List<FulfillmentGroup> fulfillmentGroups = order.getFulfillmentGroups();
        if (fulfillmentGroups != null) {
            for (FulfillmentGroup fulfillmentGroup : fulfillmentGroups) {
                if (isShippable(fulfillmentGroup.getType())) {
                    return fulfillmentGroup;
                }
            }
        }
        return null;

    }

    private static boolean isShippable(FulfillmentType fulfillmentType) {
        if (FulfillmentType.GIFT_CARD.equals(fulfillmentType) || FulfillmentType.DIGITAL.equals(fulfillmentType) ||
                FulfillmentType.PHYSICAL_PICKUP.equals(fulfillmentType)) {
            return false;
        }
        return true;
    }
}

