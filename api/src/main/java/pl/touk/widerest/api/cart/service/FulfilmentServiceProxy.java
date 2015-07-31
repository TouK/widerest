package pl.touk.widerest.api.cart.service;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.broadleafcommerce.common.money.Money;
import org.broadleafcommerce.common.vendor.service.exception.FulfillmentPriceException;
import org.broadleafcommerce.core.order.domain.DiscreteOrderItem;
import org.broadleafcommerce.core.order.domain.FulfillmentGroup;
import org.broadleafcommerce.core.order.domain.FulfillmentOption;
import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.order.service.FulfillmentGroupService;
import org.broadleafcommerce.core.order.service.FulfillmentOptionService;
import org.broadleafcommerce.core.pricing.service.FulfillmentPricingService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by mst on 31.07.15.
 */
@Service("wdfulfilmentService")
public class FulfilmentServiceProxy {

    @Resource(name = "blFulfillmentOptionService")
    private FulfillmentOptionService fulfillmentOptionService;

    @Resource(name = "blFulfillmentGroupService")
    private FulfillmentGroupService fulfillmentGroupService;

    @Resource(name = "blFulfillmentPricingService")
    private FulfillmentPricingService fulfillmentPricingService;

    public Map<? extends FulfillmentOption, Money> getFulfillmentOptionsWithPricesAvailableForProductsInCart(Order cart) throws FulfillmentPriceException {
        FulfillmentGroup fulfillmentGroup = fulfillmentGroupService.getFirstShippableFulfillmentGroup(cart);

        if (fulfillmentGroup == null) {
            return null;
        }

        return fulfillmentPricingService.estimateCostForFulfillmentGroup(fulfillmentGroup, findFulfillmentOptionsForProductsInCart(cart)).getFulfillmentOptionPrices();
    }

    private Set<FulfillmentOption> findFulfillmentOptionsForProductsInCart(Order cart) {
        Set<FulfillmentOption> fulfillmentOptions = new HashSet<>(fulfillmentOptionService.readAllFulfillmentOptions());

        for (DiscreteOrderItem item : cart.getDiscreteOrderItems()) {
            fulfillmentOptions.removeAll(((DiscreteOrderItem) item).getSku().getExcludedFulfillmentOptions());
        }

        /*
        boolean containsHandset = Iterables.any(cart.getDiscreteOrderItems(), new Predicate<DiscreteOrderItem>() {
            @Override
            public boolean apply(@Nullable DiscreteOrderItem input) {
                return Categories.PHONES.getId().equals(input.getCategory().getId());
            }
        });
        if (!containsHandset) {
            ShipmentStore pm1Store = shipmentStoreRepository.findByStore("PM1");
            ShipmentStore pm2Store = shipmentStoreRepository.findByStore("PM2");
            if (pm1Store != null) {
                fulfillmentOptions.remove(pm1Store.getFulfillmentOption());
            }
            if (pm2Store != null) {
                fulfillmentOptions.remove(pm2Store.getFulfillmentOption());
            }
        }
        return fulfillmentOptions;
        */

        return null;
    }
}
