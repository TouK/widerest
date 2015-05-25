package com.mycompany.controller.auction;

import org.broadleafcommerce.common.web.controller.BroadleafAbstractController;
import org.broadleafcommerce.core.order.service.exception.AddToCartException;
import org.broadleafcommerce.core.pricing.service.exception.PricingException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/auction")
public class AuctionController extends BroadleafAbstractController {

    @RequestMapping(value = "/bid", produces = "application/json")
    public @ResponseBody
    Map<String, Object> bidJson(HttpServletRequest request, HttpServletResponse response, Model model,
                                @ModelAttribute("bidAtAuctionItem") Object bidAtAuctionItem) throws IOException, PricingException, AddToCartException {
        Map<String, Object> responseMap = new HashMap<String, Object>();
        return responseMap;
    }

}
