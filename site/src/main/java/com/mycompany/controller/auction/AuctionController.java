package com.mycompany.controller.auction;

import org.broadleafcommerce.common.currency.util.BroadleafCurrencyUtils;
import org.broadleafcommerce.common.money.Money;
import org.broadleafcommerce.common.web.BroadleafRequestContext;
import org.broadleafcommerce.common.web.controller.BroadleafAbstractController;
import org.broadleafcommerce.core.catalog.domain.Product;
import org.broadleafcommerce.core.catalog.service.CatalogService;
import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.order.domain.OrderItem;
import org.broadleafcommerce.core.order.service.OrderItemService;
import org.broadleafcommerce.core.order.service.OrderService;
import org.broadleafcommerce.core.order.service.call.AddToCartItem;
import org.broadleafcommerce.core.order.service.exception.AddToCartException;
import org.broadleafcommerce.core.order.service.exception.RemoveFromCartException;
import org.broadleafcommerce.core.order.service.exception.RequiredAttributeNotProvidedException;
import org.broadleafcommerce.core.pricing.service.exception.PricingException;
import org.broadleafcommerce.core.web.controller.account.AbstractAccountController;
import org.broadleafcommerce.core.web.controller.account.BroadleafManageWishlistController;
import org.broadleafcommerce.ext.SkuExtImpl;
import org.broadleafcommerce.profile.web.core.CustomerState;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/auction")
public class AuctionController extends BroadleafAbstractController {

    @Resource(name="blOrderService")
    protected OrderService orderService;

    @Resource(name="blCatalogService")
    protected CatalogService catalogService;

    @Resource(name = "blOrderItemService")
    protected OrderItemService orderItemService;

    public static final String BIDDINGS_ORDER_NAME = "biddings";

    @RequestMapping(value = "/bid", produces = "application/json")
    public @ResponseBody
    Map<String, Object> bidJson(HttpServletRequest request, HttpServletResponse response, Model model,
                                @ModelAttribute("bidAtAuctionItem") BidRequestDTO bidRequest) throws IOException, PricingException, AddToCartException {
        Order customerBiddings = orderService.findNamedOrderForCustomer(BIDDINGS_ORDER_NAME, CustomerState.getCustomer(request));

        if (customerBiddings == null) {
            customerBiddings = orderService.createNamedOrderForCustomer(BIDDINGS_ORDER_NAME, CustomerState.getCustomer(request));
        }

        Product product = catalogService.findProductById(bidRequest.getProductId());
        if (((SkuExtImpl)product.getDefaultSku()).getBidPrice().compareTo(bidRequest.getBidPrice()) >= 0)
            throw new PricingException();
        ((SkuExtImpl)product.getDefaultSku()).setBidPrice(bidRequest.getBidPrice());
        catalogService.saveProduct(product);

        OrderItem bidding = orderService.findLastMatchingItem(customerBiddings, bidRequest.getSkuId(), bidRequest.getProductId());
        if (bidding != null) {
            bidding.setPrice(bidRequest.getBidPrice());
            orderItemService.saveOrderItem(bidding);
        } else {
            bidRequest.setOverrideSalePrice(bidRequest.getBidPrice());
            bidRequest.setQuantity(1);
            customerBiddings = orderService.addItemWithPriceOverrides(customerBiddings.getId(), bidRequest, false);
            bidding = orderService.findLastMatchingItem(customerBiddings, bidRequest.getSkuId(), bidRequest.getProductId());
        }


        Map<String, Object> responseMap = new HashMap<String, Object>();
        responseMap.put("productName", product.getName());

        Money price = bidRequest.getBidPrice();
        BroadleafRequestContext brc = BroadleafRequestContext.getBroadleafRequestContext();
        responseMap.put("bidPrice",
                brc.getJavaLocale() != null ? BroadleafCurrencyUtils.getNumberFormatFromCache(brc.getJavaLocale(), price.getCurrency()).format(price.getAmount()) : "$ " + price.getAmount().toString()
        );

        responseMap.put("productId", bidRequest.getProductId());
        return responseMap;
    }

}
