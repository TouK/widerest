package pl.touk.widerest.api.cart;

import org.broadleafcommerce.common.email.domain.EmailTarget;
import org.broadleafcommerce.common.email.domain.EmailTargetImpl;
import org.broadleafcommerce.common.email.service.EmailService;
import org.broadleafcommerce.common.email.service.info.EmailInfo;
import org.broadleafcommerce.core.checkout.service.workflow.CheckoutSeed;
import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.order.service.type.OrderStatus;
import org.broadleafcommerce.core.workflow.BaseActivity;
import org.broadleafcommerce.core.workflow.ProcessContext;
import org.hibernate.validator.constraints.Email;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mst on 08.10.15.
 */
public class OrderCompletedActivity extends BaseActivity<ProcessContext<CheckoutSeed>> {

    @Resource(name = "blEmailService")
    private EmailService emailService;

    @Override
    public ProcessContext<CheckoutSeed> execute(ProcessContext<CheckoutSeed> context) throws Exception {
        final Order order = context.getSeedData().getOrder();

        if(order.getStatus() != OrderStatus.SUBMITTED) {
            return context;
        }

        final Map<String, Object> emailParams = new HashMap<>();

        emailParams.put("customer", order.getCustomer());
        emailParams.put("orderNumber", order.getOrderNumber());
        emailParams.put("orderStatus", order.getStatus());
        emailParams.put("order", order);

        final EmailInfo emailInfo = new EmailInfo();

        emailInfo.setSubject("Order " + order.getOrderNumber() + " status change");

        final StringBuffer emailBody = new StringBuffer();

        for(Map.Entry<String, Object> entry : emailParams.entrySet()) {
            emailBody.append(entry.getKey());
            emailBody.append(":");
            emailBody.append(entry.getValue());
            emailBody.append("<br />");
        }

        emailInfo.setMessageBody(emailBody.toString());

        final EmailTarget emailTarget = new EmailTargetImpl();
        /* (mst) TODO: Tenant's admin address */
        emailTarget.setEmailAddress("mst@touk.pl");

        try {
            emailService.sendBasicEmail(emailInfo, emailTarget, emailParams);
        } catch(Exception ex) {

        }

        return context;
    }
}
