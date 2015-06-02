package org.broadleafcommerce.ext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadleafcommerce.common.crossapp.service.CrossAppAuthService;
import org.broadleafcommerce.common.util.BLCRequestUtils;
import org.broadleafcommerce.common.web.AbstractBroadleafWebRequestProcessor;
import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.order.service.MergeCartService;
import org.broadleafcommerce.core.order.service.OrderService;
import org.broadleafcommerce.core.order.service.call.MergeCartResponse;
import org.broadleafcommerce.core.order.service.exception.RemoveFromCartException;
import org.broadleafcommerce.core.order.service.type.OrderStatus;
import org.broadleafcommerce.core.pricing.service.exception.PricingException;
import org.broadleafcommerce.core.web.service.UpdateCartService;
import org.broadleafcommerce.profile.core.domain.Customer;
import org.broadleafcommerce.profile.web.core.CustomerState;
import org.broadleafcommerce.profile.web.core.security.CustomerStateRequestProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Component("blBiddingsStateRequestProcessor")
public class BiddingsStateRequestProcessor extends AbstractBroadleafWebRequestProcessor {

    /** Logger for this class and subclasses */
    protected final Log LOG = LogFactory.getLog(getClass());

    @Resource(name = "blOrderService")
    protected OrderService orderService;

    @Resource(name = "blUpdateCartService")
    protected UpdateCartService updateCartService;

    @Resource(name = "blMergeCartService")
    protected MergeCartService mergeCartService;

    @Resource(name = "blCustomerStateRequestProcessor")
    protected CustomerStateRequestProcessor customerStateRequestProcessor;

    @Autowired(required = false)
    @Qualifier("blCrossAppAuthService")
    protected CrossAppAuthService crossAppAuthService;

    protected static String biddingsRequestAttributeName = "biddings";

    @Override
    public void process(WebRequest request) {
        Customer customer = CustomerState.getCustomer();

        if (customer == null) {
            LOG.warn("No customer was found on the current request, no biddings order will be added to the current request. Ensure that the"
                    + " blCustomerStateFilter occurs prior to the blBiddingsStateFilter");
            return;
        }

        Order biddings = orderService.findNamedOrderForCustomer("biddings", customer);

        if (biddings == null) {
            biddings = orderService.getNullOrder();
        }

        request.setAttribute(biddingsRequestAttributeName, biddings, WebRequest.SCOPE_REQUEST);

    }

}
