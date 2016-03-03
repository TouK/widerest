package pl.touk.widerest.api.orders;

import org.broadleafcommerce.common.money.Money;
import org.broadleafcommerce.core.order.domain.FulfillmentOption;
import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.order.domain.OrderImpl;
import org.broadleafcommerce.core.order.service.type.OrderStatus;
import org.springframework.hateoas.EmbeddedResource;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Component;
import pl.touk.widerest.api.Converter;
import pl.touk.widerest.api.DtoConverters;
import pl.touk.widerest.api.customers.CustomerController;
import pl.touk.widerest.api.customers.CustomerConverter;
import pl.touk.widerest.api.orders.fulfillments.FulfillmentController;
import pl.touk.widerest.api.orders.payments.OrderPaymentConverter;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Component
public class OrderConverter implements Converter<Order, OrderDto> {

    @Resource
    private OrderPaymentConverter orderPaymentConverter;

    @Resource
    private DiscreteOrderItemConverter discreteOrderItemConverter;

    @Resource
    private CustomerConverter customerConverter;

    @Override
    public OrderDto createDto(final Order order, final boolean embed) {
        final OrderDto orderDto = OrderDto.builder()
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus().getType())
                .orderPayment(order.getPayments().stream()
                        .map(orderPayment -> orderPaymentConverter.createDto(orderPayment, false)).collect(Collectors.toList()))
                .orderItems(order.getDiscreteOrderItems().stream()
                        .map(discreteOrderItem -> discreteOrderItemConverter.createDto(discreteOrderItem, false))
                        .collect(Collectors.toList()))
//                .customer(DtoConverters.customerEntityToDto.apply(entity.getCustomer()))
                .totalPrice(Money.toAmount(order.getTotal()))
                .fulfillment(CartUtils.getFulfilmentOption(order)
                        .map(FulfillmentOption::getLongDescription)
                        .orElse(null))
                .cartAttributes(Optional.ofNullable(order.getOrderAttributes()).orElse(Collections.emptyMap()).entrySet().stream()
                        .map(Map.Entry::getValue)
                        .map(DtoConverters.orderAttributeEntityToDto)
                        .collect(toList()))
                .build();


        // (mst) Add Customer details as an embedded resource
        if(embed) {
            Optional.ofNullable(order.getCustomer()).ifPresent(customer -> {
                orderDto.add(new EmbeddedResource("customer", customerConverter.createDto(customer, false)));
            });
        }

        orderDto.add(linkTo(
                methodOn(CustomerController.class).readOneCustomer(null, String.valueOf(order.getCustomer().getId()))
        ).withRel("customer"));

        orderDto.add(ControllerLinkBuilder.linkTo(methodOn(OrderController.class).getOrderById(null, order.getId())).withSelfRel());

//        orderDto.add(linkTo(methodOn(OrderController.class).getOrdersCount(null)).withRel("order-count"));

        /* link to items placed in an order */
        orderDto.add(linkTo(methodOn(OrderController.class).getAllItemsInOrder(null, order.getId())).withRel("items"));

//        orderDto.add(linkTo(methodOn(OrderController.class).getItemsCountByOrderId(null, order.getId())).withRel("items-count"));

        /* link to fulfillment */
        orderDto.add(linkTo(methodOn(FulfillmentController.class).getOrderFulfilment(null, order.getId())).withRel("fulfillment"));

        orderDto.add(linkTo(methodOn(OrderController.class).getOrderStatusById(null, order.getId())).withRel("status"));

        return orderDto;
    }

    @Override
    public Order createEntity(final OrderDto orderDto) {
        final Order orderEntity = new OrderImpl();
        return updateEntity(orderEntity, orderDto);
    }

    @Override
    public Order updateEntity(final Order order, final OrderDto orderDto) {
        order.setOrderNumber(orderDto.getOrderNumber());
        order.setStatus(OrderStatus.getInstance(orderDto.getStatus()));
        order.setPayments(orderDto.getOrderPayment().stream().map(orderPaymentConverter::createEntity)
                .collect(Collectors.toList()));

        return order;
    }

    @Override
    public Order partialUpdateEntity(final Order order, final OrderDto orderDto) {
        throw new UnsupportedOperationException();
    }
}
