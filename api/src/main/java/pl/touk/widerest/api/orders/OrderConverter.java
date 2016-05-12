package pl.touk.widerest.api.orders;

import org.broadleafcommerce.common.money.Money;
import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.order.domain.OrderAttribute;
import org.broadleafcommerce.core.order.service.OrderService;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Component;
import pl.touk.widerest.api.Converter;
import pl.touk.widerest.api.customers.CustomerController;
import pl.touk.widerest.api.customers.CustomerConverter;
import pl.touk.widerest.api.orders.fulfillments.FulfillmentController;
import pl.touk.widerest.api.orders.fulfillments.FulfillmentConverter;
import pl.touk.widerest.api.orders.payments.OrderPaymentConverter;
import pl.touk.widerest.hal.EmbeddedResource;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Component
public class OrderConverter implements Converter<Order, OrderDto> {

    public static final String REL_ITEMS = "items";
    public static final String REL_FULFILLMENTS = "fulfillments";
    public static final String REL_STATUS = "status";
    public static final String REL_PAYMENT = "payment";

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
//                .payment(order.getPayments().stream()
//                        .map(orderPayment -> orderPaymentConverter.createDto(orderPayment, embed, link)).collect(Collectors.toList()))
//                .orderItems(order.getDiscreteOrderItems().stream()
//                        .map(discreteOrderItem -> discreteOrderItemConverter.createDto(discreteOrderItem, embed, link))
//                        .collect(Collectors.toList()))
                .totalPrice(Money.toAmount(order.getTotal()))
//                .fulfillment(CartUtils.getFulfilmentOption(order)
//                        .map(FulfillmentOption::getLongDescription)
//                        .orElse(null))
                .attributes(
                        Optional.ofNullable(order.getOrderAttributes())
                                .map(Map::values)
                                .map(Collection::stream)
                                .map(stream -> stream.collect(toMap(OrderAttribute::getName, OrderAttribute::getValue))).orElse(null)
                )
                .build();


        // (mst) Add Customer details as an embedded resource
        if(embed) {
            Optional.ofNullable(order.getCustomer()).ifPresent(customer -> {
                orderDto.add(new EmbeddedResource("customer", customerConverter.createDto(customer, embed, link)));
            });

            Optional.ofNullable(order.getDiscreteOrderItems())
                    .map(orderItems -> orderItems.stream()
                            .map(discreteOrderItem -> discreteOrderItemConverter.createDto(discreteOrderItem, embed, link))
                            .collect(Collectors.toList())
                    )
                    .filter(((Predicate<Collection>) Collection::isEmpty).negate())
                    .map(fulfillmentDtos -> new EmbeddedResource("items", fulfillmentDtos))
                    .ifPresent(orderDto::add);

            Optional.ofNullable(order.getFulfillmentGroups())
                    .map(fulfillmentGroups -> fulfillmentGroups.stream()
                            .map(fulfillmentGroup -> fulfillmentConverter.createDto(fulfillmentGroup, embed, link))
                            .collect(toList())
                    )
                    .filter(((Predicate<Collection>) Collection::isEmpty).negate())
                    .map(fulfillmentDtos -> new EmbeddedResource("fulfillments", fulfillmentDtos))
                    .ifPresent(orderDto::add);
        }

        orderDto.add(ControllerLinkBuilder.linkTo(methodOn(OrderController.class).getOrderById(null, order.getId(), null, null)).withSelfRel());

        if (link) {
            orderDto.add(linkTo(
                    methodOn(CustomerController.class).readOneCustomer(null, String.valueOf(order.getCustomer().getId()), null, null)
            ).withRel("customer"));

            orderDto.add(linkTo(methodOn(OrderController.class).getAllItemsInOrder(null, order.getId(), null, null)).withRel(REL_ITEMS));
            orderDto.add(linkTo(methodOn(FulfillmentController.class).getOrderFulfillments(null, order.getId(), null, null)).withRel(REL_FULFILLMENTS));
            orderDto.add(linkTo(methodOn(OrderController.class).getOrderStatusById(null, order.getId())).withRel(REL_STATUS));
            orderDto.add(linkTo(methodOn(OrderController.class).initiatePayment(null, null, order.getId())).withRel(REL_PAYMENT));
        }

        return orderDto;
    }

}
