package pl.touk.widerest.paypal.gateway;

import org.broadleafcommerce.common.payment.service.*;
import org.broadleafcommerce.common.web.payment.expression.PaymentGatewayFieldExtensionHandler;
import org.broadleafcommerce.common.web.payment.processor.CreditCardTypesExtensionHandler;
import org.broadleafcommerce.common.web.payment.processor.TRCreditCardExtensionHandler;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class PayPalGatewayConfigurationService implements PaymentGatewayConfigurationService {

    @Resource
    PayPalGatewayConfiguration payPalRestGatewayConfiguration;

    @Resource
    PayPalGatewayService payPalGatewayService;

    @Override
    public PaymentGatewayConfiguration getConfiguration() {
        return payPalRestGatewayConfiguration;
    }

    @Override
    public PaymentGatewayTransactionService getTransactionService() {
        return null;
    }

    @Override
    public PaymentGatewayTransactionConfirmationService getTransactionConfirmationService() {
        return payPalGatewayService;
    }

    @Override
    public PaymentGatewayReportingService getReportingService() {
        return payPalGatewayService;
    }

    @Override
    public PaymentGatewayCreditCardService getCreditCardService() {
        return null;
    }

    @Override
    public PaymentGatewayCustomerService getCustomerService() {
        return null;
    }

    @Override
    public PaymentGatewaySubscriptionService getSubscriptionService() {
        return null;
    }

    @Override
    public PaymentGatewayFraudService getFraudService() {
        return null;
    }

    @Override
    public PaymentGatewayHostedService getHostedService() {
        return payPalGatewayService;
    }

    @Override
    public PaymentGatewayRollbackService getRollbackService() {
        return null;
    }

    @Override
    public PaymentGatewayWebResponseService getWebResponseService() {
        return payPalGatewayService;
    }

    @Override
    public PaymentGatewayTransparentRedirectService getTransparentRedirectService() {
        return null;
    }

    @Override
    public TRCreditCardExtensionHandler getCreditCardExtensionHandler() {
        return null;
    }

    @Override
    public PaymentGatewayFieldExtensionHandler getFieldExtensionHandler() {
        return null;
    }

    @Override
    public CreditCardTypesExtensionHandler getCreditCardTypesExtensionHandler() {
        return null;
    }
}
