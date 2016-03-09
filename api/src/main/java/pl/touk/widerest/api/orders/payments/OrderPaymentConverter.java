package pl.touk.widerest.api.orders.payments;

import org.broadleafcommerce.common.payment.PaymentType;
import org.broadleafcommerce.core.payment.domain.OrderPayment;
import org.broadleafcommerce.core.payment.domain.OrderPaymentImpl;
import org.broadleafcommerce.core.payment.service.OrderPaymentService;
import org.springframework.stereotype.Component;
import pl.touk.widerest.api.Converter;
import pl.touk.widerest.api.common.AddressConverter;

import javax.annotation.Resource;

@Component
public class OrderPaymentConverter implements Converter<OrderPayment, OrderPaymentDto>{

    @Resource
    protected AddressConverter addressConverter;

    @Resource
    protected OrderPaymentService orderPaymentService;

    @Override
    public OrderPaymentDto createDto(final OrderPayment orderPayment, final boolean embed) {
        return OrderPaymentDto.builder()
                .amount(orderPayment.getAmount())
                .billingAddress(addressConverter.createDto(orderPayment.getBillingAddress(), false))
                .orderId(orderPayment.getOrder().getId()).paymentId(orderPayment.getId())
                .referenceNumber(orderPayment.getReferenceNumber()).type(orderPayment.getType().getType()).build();
    }

}
