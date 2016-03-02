package pl.touk.widerest.api.orders;

import lombok.experimental.UtilityClass;
import org.broadleafcommerce.core.order.domain.FulfillmentGroup;
import org.broadleafcommerce.core.order.domain.FulfillmentOption;
import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.order.service.type.FulfillmentType;

import java.util.Arrays;
import java.util.Optional;

import static org.broadleafcommerce.core.order.service.type.FulfillmentType.DIGITAL;
import static org.broadleafcommerce.core.order.service.type.FulfillmentType.GIFT_CARD;
import static org.broadleafcommerce.core.order.service.type.FulfillmentType.PHYSICAL_PICKUP;

@UtilityClass
public class CartUtils {

    public static Optional<FulfillmentOption> getFulfilmentOption(Order order) {
        return getFirstShippableFulfilmentGroup(order)
                .map(FulfillmentGroup::getFulfillmentOption);
    }

    public static Optional<FulfillmentGroup> getFirstShippableFulfilmentGroup(Order order) {
        return order.getFulfillmentGroups().stream()
                .filter(group -> isShippable(group.getType()))
                .findFirst();
    }

    private static boolean isShippable(FulfillmentType fulfillmentType) {
        return !Arrays.asList(GIFT_CARD, DIGITAL, PHYSICAL_PICKUP).contains(fulfillmentType);
    }
}

