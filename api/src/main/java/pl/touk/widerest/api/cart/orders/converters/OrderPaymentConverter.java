package pl.touk.widerest.api.cart.orders.converters;

import org.broadleafcommerce.common.payment.PaymentType;
import org.broadleafcommerce.core.payment.domain.OrderPayment;
import org.broadleafcommerce.core.payment.domain.OrderPaymentImpl;
import org.springframework.stereotype.Component;
import pl.touk.widerest.api.Converter;
import pl.touk.widerest.api.cart.customers.converters.AddressConverter;
import pl.touk.widerest.api.cart.orders.dto.OrderPaymentDto;

import javax.annotation.Resource;

@Component
public class OrderPaymentConverter implements Converter<OrderPayment, OrderPaymentDto>{

    @Resource
    private AddressConverter addressConverter;

    @Override
    public OrderPaymentDto createDto(final OrderPayment orderPayment, final boolean embed) {
        return OrderPaymentDto.builder()
                .amount(orderPayment.getAmount())
                .billingAddress(addressConverter.createDto(orderPayment.getBillingAddress(), false))
                .orderId(orderPayment.getOrder().getId()).paymentId(orderPayment.getId())
                .referenceNumber(orderPayment.getReferenceNumber()).type(orderPayment.getType().getType()).build();
    }

    @Override
    public OrderPayment createEntity(final OrderPaymentDto orderPaymentDto) {
        final OrderPayment orderPayment = new OrderPaymentImpl();
        return updateEntity(orderPayment, orderPaymentDto);
    }

    @Override
    public OrderPayment updateEntity(final OrderPayment orderPayment, final OrderPaymentDto orderPaymentDto) {
        orderPayment.setId(orderPaymentDto.getOrderId());
        orderPayment.setAmount(orderPaymentDto.getAmount());
        orderPayment.setBillingAddress(addressConverter.createEntity(orderPaymentDto.getBillingAddress()));
        orderPayment.setReferenceNumber(orderPaymentDto.getReferenceNumber());
        orderPayment.setType(PaymentType.getInstance(orderPaymentDto.getType()));

        return orderPayment;
    }

    @Override
    public OrderPayment partialUpdateEntity(OrderPayment orderPayment, OrderPaymentDto orderPaymentDto) {
        throw new UnsupportedOperationException();
    }
}
