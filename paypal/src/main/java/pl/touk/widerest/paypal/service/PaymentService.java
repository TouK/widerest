package pl.touk.widerest.paypal.service;

import org.broadleafcommerce.common.payment.dto.PaymentResponseDTO;
import org.broadleafcommerce.core.order.service.OrderService;
import org.broadleafcommerce.core.payment.service.OrderPaymentService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * Created by mst on 05.08.15.
 */
@Service("wdPaymentService")
public class PaymentService {

    @Resource(name = "blOrderService")
    private OrderService orderService;

    @Resource(name = "blOrderPaymentService")
    private OrderPaymentService orderPaymentService;

    public void processPaymentGatewayResponse(PaymentResponseDTO paymentResponse) {

    }
}
