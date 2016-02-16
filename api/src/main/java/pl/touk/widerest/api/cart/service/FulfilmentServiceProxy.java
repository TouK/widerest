package pl.touk.widerest.api.cart.service;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
import org.springframework.transaction.annotation.Transactional;

import pl.touk.widerest.api.cart.customers.converters.AddressConverter;
import pl.touk.widerest.api.cart.orders.OrderController;
import pl.touk.widerest.api.cart.dto.FulfillmentDto;
import pl.touk.widerest.api.cart.dto.FulfillmentOptionDto;
import pl.touk.widerest.api.cart.exceptions.FulfillmentOptionNotAllowedException;
import pl.touk.widerest.api.cart.exceptions.NotShippableException;
import pl.touk.widerest.api.cart.exceptions.UnknownFulfillmentOptionException;

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

    public Map<? extends FulfillmentOption, Money> getFulfillmentOptionsWithPricesAvailableForProductsInOrder(Order cart) throws FulfillmentPriceException {
        FulfillmentGroup fulfillmentGroup = fulfillmentGroupService.getFirstShippableFulfillmentGroup(cart);

        if (fulfillmentGroup == null) {
            return null; // TODO nie lepiej pustą mapę?
        }

        return fulfillmentPricingService.estimateCostForFulfillmentGroup(fulfillmentGroup, findFulfillmentOptionsForProductsInOrder(cart)).getFulfillmentOptionPrices();
    }

    private Set<FulfillmentOption> findFulfillmentOptionsForProductsInOrder(Order cart) {
        Set<FulfillmentOption> fulfillmentOptions = new HashSet<>(fulfillmentOptionService.readAllFulfillmentOptions());

        for (DiscreteOrderItem item : cart.getDiscreteOrderItems()) {
            fulfillmentOptions.removeAll(item.getSku().getExcludedFulfillmentOptions());
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

    @Transactional
    public FulfillmentDto createFulfillmentDto(Order order) {
        FulfillmentDto fulfillmentDto = new FulfillmentDto();

        FulfillmentGroup fulfillmentGroup = fulfillmentGroupService.getFirstShippableFulfillmentGroup(order);

        if(fulfillmentGroup != null) {
            if(fulfillmentGroup.getAddress() != null) {
                fulfillmentDto.setAddress(addressConverter.createDto(fulfillmentGroup.getAddress(), false));
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


        fulfillmentDto.add(linkTo(methodOn(OrderController.class).getOrderFulfilment(null, order.getId())).withSelfRel());

        fulfillmentDto.add(linkTo(methodOn(OrderController.class).getOrderFulfilmentAddress(null, order.getId())).withRel("fulfillment-address"));

        return fulfillmentDto;
    }


}