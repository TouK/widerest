package pl.touk.widerest.api.cart.service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Resource;

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

import pl.touk.widerest.api.DtoConverters;
import pl.touk.widerest.api.cart.dto.FulfillmentDto;
import pl.touk.widerest.api.cart.dto.FulfillmentOptionDto;
import pl.touk.widerest.api.cart.exceptions.FulfillmentOptionNotAllowedException;
import pl.touk.widerest.api.cart.exceptions.NotShippableException;
import pl.touk.widerest.api.cart.exceptions.UnknownFulfillmentOptionException;

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

    @Resource(name = "blOrderService")
    private OrderService orderService;

    public Map<? extends FulfillmentOption, Money> getFulfillmentOptionsWithPricesAvailableForProductsInOrder(Order cart) throws FulfillmentPriceException {
        FulfillmentGroup fulfillmentGroup = fulfillmentGroupService.getFirstShippableFulfillmentGroup(cart);

        if (fulfillmentGroup == null) {
            return null;
        }

        return fulfillmentPricingService.estimateCostForFulfillmentGroup(fulfillmentGroup, findFulfillmentOptionsForProductsInOrder(cart)).getFulfillmentOptionPrices();
    }

    private Set<FulfillmentOption> findFulfillmentOptionsForProductsInOrder(Order cart) {
        Set<FulfillmentOption> fulfillmentOptions = new HashSet<>(fulfillmentOptionService.readAllFulfillmentOptions());

        for (DiscreteOrderItem item : cart.getDiscreteOrderItems()) {
            fulfillmentOptions.removeAll(((DiscreteOrderItem) item).getSku().getExcludedFulfillmentOptions());
        }

        return fulfillmentOptions;
    }

    public Order updateFulfillmentOption(Order order, long fulfillmentOptionId) throws PricingException {
        Set<FulfillmentOption> allowedFulfillmentOptions = findFulfillmentOptionsForProductsInOrder(order);
        FulfillmentOption fulfillmentOption = fulfillmentOptionService.readFulfillmentOptionById(fulfillmentOptionId);

        if (fulfillmentOption == null) {
            throw new UnknownFulfillmentOptionException();
        }
        if (!allowedFulfillmentOptions.contains(fulfillmentOption)) {
            throw new FulfillmentOptionNotAllowedException();
        }

        FulfillmentGroup shippableFulfillmentGroup = fulfillmentGroupService.getFirstShippableFulfillmentGroup(order);

        if(shippableFulfillmentGroup != null) {
            shippableFulfillmentGroup.setFulfillmentOption(fulfillmentOption);
            order = orderService.save(order, true);
        }
        return order;
    }



    public Address getFulfillmentAddress(Order order) {
        FulfillmentGroup shippableFulfillmentGroup = fulfillmentGroupService.getFirstShippableFulfillmentGroup(order);

        Address fulfillmentAddress = null;

        if(shippableFulfillmentGroup != null) {
            fulfillmentAddress = shippableFulfillmentGroup.getAddress();
        }

        return fulfillmentAddress;
    }

    public Order updateFulfillmentAddress(Order order, Address address) throws PricingException {
        FulfillmentGroup shippableFulfillmentGroup = fulfillmentGroupService.getFirstShippableFulfillmentGroup(order);

        if(shippableFulfillmentGroup != null) {
            shippableFulfillmentGroup.setAddress(address);
            order = orderService.save(order, true);
    		
    		
    		/* TODO: (mst) update address for Order's customer??? */
        } else {
            throw new NotShippableException();
        }
        return order;
    }

    public Function<Order, FulfillmentDto> createFulfillmentDto = order -> {
        FulfillmentDto fulfillmentDto = new FulfillmentDto();

        FulfillmentGroup fulfillmentGroup = fulfillmentGroupService.getFirstShippableFulfillmentGroup(order);

        if(fulfillmentGroup != null) {
            if(fulfillmentGroup.getAddress() != null) {
                fulfillmentDto.setAddress(DtoConverters.addressEntityToDto.apply(fulfillmentGroup.getAddress()));
            }

            if(fulfillmentGroup.getFulfillmentOption() != null) {
                fulfillmentDto.setSelectedOptionId(fulfillmentGroup.getFulfillmentOption().getId());
            }

            if(fulfillmentGroup.getFulfillmentPrice() != null) {
                fulfillmentDto.setPrice(fulfillmentGroup.getFulfillmentPrice().getAmount());
            }
        } else {
            fulfillmentDto.setPrice(BigDecimal.ZERO);
        }

        try {
            Map<? extends FulfillmentOption, Money> options = getFulfillmentOptionsWithPricesAvailableForProductsInOrder(order);

            if(options != null) {

                fulfillmentDto.setOptions(options.entrySet().stream()
                        .map(e -> {
                            FulfillmentOptionDto fulfillmentOptionDto = new FulfillmentOptionDto();
                            fulfillmentOptionDto.setDescription(e.getKey().getLongDescription());
                            fulfillmentOptionDto.setName(e.getKey().getName());
                            fulfillmentOptionDto.setId(e.getKey().getId());
                            fulfillmentOptionDto.setPrice(e.getValue().getAmount());
                            return fulfillmentOptionDto;
                        })
                        .collect(Collectors.toList()));
            }

        } catch (FulfillmentPriceException e) {
            e.printStackTrace();
        }



        return fulfillmentDto;
    };


}