package pl.touk.widerest.api.orders;

import org.broadleafcommerce.common.money.Money;
import org.broadleafcommerce.core.order.domain.FulfillmentOption;
import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.order.domain.OrderAttribute;
import org.broadleafcommerce.core.order.domain.OrderImpl;
import org.broadleafcommerce.core.order.service.OrderService;
import org.broadleafcommerce.core.order.service.type.OrderStatus;
import org.springframework.hateoas.EmbeddedResource;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Component;
import pl.touk.widerest.api.Converter;
import pl.touk.widerest.api.DtoConverters;
import pl.touk.widerest.api.customers.CustomerController;
import pl.touk.widerest.api.customers.CustomerConverter;
import pl.touk.widerest.api.orders.fulfillments.FulfillmentController;
import pl.touk.widerest.api.orders.fulfillments.FulfillmentConverter;
import pl.touk.widerest.api.orders.fulfillments.FulfillmentDto;
import pl.touk.widerest.api.orders.payments.OrderPaymentConverter;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Component
public class OrderConverter implements Converter<Order, OrderDto> {

    @Resource
    protected OrderPaymentConverter orderPaymentConverter;

    @Resource
    protected DiscreteOrderItemConverter discreteOrderItemConverter;

    @Resource
    protected CustomerConverter customerConverter;

    @Resource
    protected OrderService orderService;

    @Resource
    protected FulfillmentConverter fulfillmentConverter;

    @Override
    public OrderDto createDto(final Order order, final boolean embed, final boolean link) {
        final OrderDto orderDto = OrderDto.builder()
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus().getType())
                .orderPayment(order.getPayments().stream()
                        .map(orderPayment -> orderPaymentConverter.createDto(orderPayment, embed, link)).collect(Collectors.toList()))
                .orderItems(order.getDiscreteOrderItems().stream()
                        .map(discreteOrderItem -> discreteOrderItemConverter.createDto(discreteOrderItem, embed, link))
                        .collect(Collectors.toList()))
//                .customer(DtoConverters.customerEntityToDto.apply(entity.getCustomer()))
                .totalPrice(Money.toAmount(order.getTotal()))
                .fulfillment(CartUtils.getFulfilmentOption(order)
                        .map(FulfillmentOption::getLongDescription)
                        .orElse(null))
                .attributes(Optional.ofNullable(order.getOrderAttributes()).orElse(Collections.emptyMap()).entrySet().stream()
                        .map(Map.Entry::getValue)
                        .map(orderAttributeEntityToDto)
                        .collect(toList()))
                .build();


        // (mst) Add Customer details as an embedded resource
        if(embed) {
            Optional.ofNullable(order.getCustomer()).ifPresent(customer -> {
                orderDto.add(new EmbeddedResource("customer", customerConverter.createDto(customer, embed, link)));
            });
            Optional.ofNullable(order.getFulfillmentGroups())
                    .map(fulfillmentGroups -> fulfillmentGroups.stream()
                            .map(fulfillmentGroup -> fulfillmentConverter.createDto(fulfillmentGroup, embed, link))
                            .collect(toList())
                    )
                    .filter(((Predicate<Collection>) Collection::isEmpty).negate())
                    .map(fulfillmentDtos -> new EmbeddedResource("fulfillments", fulfillmentDtos))
                    .ifPresent(orderDto::add);
        }

        if (link) {
            orderDto.add(linkTo(
                    methodOn(CustomerController.class).readOneCustomer(null, String.valueOf(order.getCustomer().getId()))
            ).withRel("customer"));

            orderDto.add(ControllerLinkBuilder.linkTo(methodOn(OrderController.class).getOrderById(null, order.getId(), null, null)).withSelfRel());

//        orderDto.add(linkTo(methodOn(OrderController.class).getOrdersCount(null)).withRel("order-count"));

        /* link to items placed in an order */
            orderDto.add(linkTo(methodOn(OrderController.class).getAllItemsInOrder(null, order.getId(), null, null)).withRel("items"));

//        orderDto.add(linkTo(methodOn(OrderController.class).getItemsCountByOrderId(null, order.getId())).withRel("items-count"));

        /* link to fulfillment */
            orderDto.add(linkTo(methodOn(FulfillmentController.class).getOrderFulfillments(null, order.getId())).withRel("fulfillments"));

            orderDto.add(linkTo(methodOn(OrderController.class).getOrderStatusById(null, order.getId())).withRel("status"));
        }

        return orderDto;
    }

    private static Function<OrderAttribute, CartAttributeDto> orderAttributeEntityToDto = entity ->
            CartAttributeDto.builder().name(entity.getName()).value(entity.getValue()).build();
}
