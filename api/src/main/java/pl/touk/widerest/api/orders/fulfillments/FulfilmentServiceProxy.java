package pl.touk.widerest.api.orders.fulfillments;

import org.broadleafcommerce.common.money.Money;
import org.broadleafcommerce.common.vendor.service.exception.FulfillmentPriceException;
import org.broadleafcommerce.common.web.BroadleafRequestContext;
import org.broadleafcommerce.core.catalog.domain.Category;
import org.broadleafcommerce.core.catalog.domain.Product;
import org.broadleafcommerce.core.catalog.domain.Sku;
import org.broadleafcommerce.core.order.dao.FulfillmentGroupDao;
import org.broadleafcommerce.core.order.dao.FulfillmentGroupItemDao;
import org.broadleafcommerce.core.order.dao.OrderDao;
import org.broadleafcommerce.core.order.dao.OrderItemDao;
import org.broadleafcommerce.core.order.domain.DiscreteOrderItem;
import org.broadleafcommerce.core.order.domain.FulfillmentGroup;
import org.broadleafcommerce.core.order.domain.FulfillmentGroupItem;
import org.broadleafcommerce.core.order.domain.FulfillmentOption;
import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.order.service.FulfillmentGroupService;
import org.broadleafcommerce.core.order.service.FulfillmentOptionService;
import org.broadleafcommerce.core.order.service.type.FulfillmentType;
import org.broadleafcommerce.core.order.service.type.OrderItemType;
import org.broadleafcommerce.core.pricing.service.FulfillmentPricingService;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@CacheConfig(cacheNames = "fulfillmentOptions")
public class FulfilmentServiceProxy {

    @Resource(name = "blFulfillmentOptionService")
    protected FulfillmentOptionService fulfillmentOptionService;

    @Resource(name = "blFulfillmentGroupService")
    protected FulfillmentGroupService fulfillmentGroupService;

    @Resource(name = "blFulfillmentPricingService")
    private FulfillmentPricingService fulfillmentPricingService;

    @Resource(name = "blOrderDao")
    private OrderDao orderDao;

    @Resource(name = "blOrderItemDao")
    private OrderItemDao orderItemDao;

    @Resource(name = "blFulfillmentGroupDao")
    private FulfillmentGroupDao fulfillmentGroupDao;

    @Resource(name = "blFulfillmentGroupItemDao")
    private FulfillmentGroupItemDao fulfillmentGroupItemDao;

    @Cacheable(key="#type?.type ?: '_null_'")
    public Map<? extends FulfillmentOption, Money> readFulfillmentOptionsWithPricesAvailableByFulfillmentType(FulfillmentType type) throws FulfillmentPriceException {
        final List<FulfillmentOption> fulfillmentOptions = type != null
                ? fulfillmentOptionService.readAllFulfillmentOptionsByFulfillmentType(type)
                : fulfillmentOptionService.readAllFulfillmentOptions();


        return fulfillmentPricingService.estimateCostForFulfillmentGroup(
                createTemporaryFulfillmentGroup(),
                new HashSet(fulfillmentOptions)
        ).getFulfillmentOptionPrices();
    }

    public Map<? extends FulfillmentOption, Money> readFulfillmentOptionsWithPricesAvailableForProductsInFulfillmentGroup(FulfillmentGroup fulfillmentGroup) throws FulfillmentPriceException {
        return fulfillmentPricingService.estimateCostForFulfillmentGroup(
                fulfillmentGroup,
                findFulfillmentOptionsForProductsInFulfillmentGroup(fulfillmentGroup)
        ).getFulfillmentOptionPrices();
    }

    @Cacheable(key="#product?.id")
    public Map<? extends FulfillmentOption, Money> readFulfillmentOptionsWithPricesAvailableForProduct(Product product) throws FulfillmentPriceException {
        final List<FulfillmentOption> fulfillmentOptions = Optional.ofNullable(product.getCategory())
                .map(Category::getFulfillmentType)
                .map(fulfillmentOptionService::readAllFulfillmentOptionsByFulfillmentType)
                .orElseGet(fulfillmentOptionService::readAllFulfillmentOptions);

        fulfillmentOptions.removeAll(product.getDefaultSku().getExcludedFulfillmentOptions());

        return fulfillmentPricingService.estimateCostForFulfillmentGroup(
                createTemporaryFulfillmentGroupWithSingleItem(product),
                new HashSet(fulfillmentOptions)
        ).getFulfillmentOptionPrices();
    }

    private FulfillmentGroup createTemporaryFulfillmentGroup() {

        FulfillmentGroup fulfillmentGroup = fulfillmentGroupDao.create();
        Order order = orderDao.create();
        if (BroadleafRequestContext.getBroadleafRequestContext() != null) {
            order.setCurrency(BroadleafRequestContext.getBroadleafRequestContext().getBroadleafCurrency());
            order.setLocale(BroadleafRequestContext.getBroadleafRequestContext().getLocale());
        }
        fulfillmentGroup.setOrder(order);
        order.getFulfillmentGroups().add(fulfillmentGroup);

        return fulfillmentGroup;
    }

    private FulfillmentGroup createTemporaryFulfillmentGroupWithSingleItem(Product product) {

        FulfillmentGroup fulfillmentGroup = createTemporaryFulfillmentGroup();

        DiscreteOrderItem orderItem = (DiscreteOrderItem) orderItemDao.create(OrderItemType.DISCRETE);
        orderItem.setSku(product.getDefaultSku());
        orderItem.setOrder(fulfillmentGroup.getOrder());
        orderItem.setPrice(product.getDefaultSku().getPrice());

        FulfillmentGroupItem fgi = fulfillmentGroupItemDao.create();
        fgi.setFulfillmentGroup(fulfillmentGroup);
        fgi.setOrderItem(orderItem);
        fgi.setQuantity(1);

        fulfillmentGroup.addFulfillmentGroupItem(fgi);

        return fulfillmentGroup;

    }

    public Set<FulfillmentOption> findFulfillmentOptionsForProductsInFulfillmentGroup(FulfillmentGroup fulfillmentGroup) {
        Set<FulfillmentOption> fulfillmentOptions = new HashSet<>(fulfillmentOptionService.readAllFulfillmentOptions());

        for (DiscreteOrderItem item : fulfillmentGroup.getDiscreteOrderItems()) {
            Optional.of(item.getSku())
                    .map(Sku::getFulfillmentType)
                    .map(fulfillmentOptionService::readAllFulfillmentOptionsByFulfillmentType)
                    .ifPresent(fulfillmentOptions::retainAll);

            Optional.of(item.getSku())
                    .map(Sku::getExcludedFulfillmentOptions)
                    .ifPresent(fulfillmentOptions::removeAll);

            Optional.of(item.getSku())
                    .map(Sku::getProduct)
                    .map(Product::getDefaultSku)
                    .map(Sku::getExcludedFulfillmentOptions)
                    .ifPresent(fulfillmentOptions::removeAll);

        }

        return fulfillmentOptions;
    }

}