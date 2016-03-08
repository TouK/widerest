package pl.touk.widerest.api.orders.fulfillments;

import org.broadleafcommerce.common.money.Money;
import org.broadleafcommerce.common.vendor.service.exception.FulfillmentPriceException;
import org.broadleafcommerce.core.order.domain.DiscreteOrderItem;
import org.broadleafcommerce.core.order.domain.FulfillmentGroup;
import org.broadleafcommerce.core.order.domain.FulfillmentOption;
import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.order.service.FulfillmentGroupService;
import org.broadleafcommerce.core.order.service.FulfillmentOptionService;
import org.broadleafcommerce.core.order.service.OrderService;
import org.broadleafcommerce.core.pricing.service.FulfillmentPricingService;
import org.broadleafcommerce.core.pricing.service.exception.PricingException;
import org.broadleafcommerce.profile.core.domain.Address;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.touk.widerest.api.common.AddressConverter;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service("wdfulfilmentService")
public class FulfilmentServiceProxy {

    @Resource(name = "blFulfillmentOptionService")
    private FulfillmentOptionService fulfillmentOptionService;

    @Resource(name = "blFulfillmentGroupService")
    private FulfillmentGroupService fulfillmentGroupService;

    @Resource(name = "blFulfillmentPricingService")
    private FulfillmentPricingService fulfillmentPricingService;

    @Resource(name = "blOrderService")
    private OrderService orderService;

    @Resource
    private AddressConverter addressConverter;

    public Map<? extends FulfillmentOption, Money> getFulfillmentOptionsWithPricesAvailableForProductsInFulfillmentGroup(FulfillmentGroup fulfillmentGroup) throws FulfillmentPriceException {
        return fulfillmentPricingService.estimateCostForFulfillmentGroup(
                fulfillmentGroup,
                findFulfillmentOptionsForProductsInFulfillmentGroup(fulfillmentGroup)
        ).getFulfillmentOptionPrices();
    }

    public Set<FulfillmentOption> findFulfillmentOptionsForProductsInFulfillmentGroup(FulfillmentGroup fulfillmentGroup) {
        Set<FulfillmentOption> fulfillmentOptions = new HashSet<>(fulfillmentOptionService.readAllFulfillmentOptions());

        for (DiscreteOrderItem item : fulfillmentGroup.getDiscreteOrderItems()) {
            fulfillmentOptions.removeAll(item.getSku().getExcludedFulfillmentOptions());
        }

        return fulfillmentOptions;
    }

    public FulfillmentGroup updateFulfillmentOption(FulfillmentGroup fulfillmentGroup, long fulfillmentOptionId) throws PricingException {
        Set<FulfillmentOption> allowedFulfillmentOptions = findFulfillmentOptionsForProductsInFulfillmentGroup(fulfillmentGroup);
        FulfillmentOption fulfillmentOption = fulfillmentOptionService.readFulfillmentOptionById(fulfillmentOptionId);

        if (fulfillmentOption == null) {
            throw new UnknownFulfillmentOptionException();
        }
        if (!allowedFulfillmentOptions.contains(fulfillmentOption)) {
            throw new FulfillmentOptionNotAllowedException();
        }

        return fulfillmentGroup;
    }

    @Transactional
    public Optional<Address> getFulfillmentAddress(Order order) {
        return Optional.ofNullable(fulfillmentGroupService.getFirstShippableFulfillmentGroup(order))
                .filter(Objects::nonNull)
                .map(FulfillmentGroup::getAddress);
    }

    public Order updateFulfillmentAddress(Order order, Address address) throws PricingException {
        FulfillmentGroup shippableFulfillmentGroup = fulfillmentGroupService.getFirstShippableFulfillmentGroup(order);

        if(shippableFulfillmentGroup != null) {
            shippableFulfillmentGroup.setAddress(address);
            order = orderService.save(order, true);
    		
        } else {
            throw new NotShippableException();
        }
        return order;
    }

}