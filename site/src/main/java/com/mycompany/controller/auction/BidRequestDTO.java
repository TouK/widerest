package com.mycompany.controller.auction;

import lombok.Getter;
import lombok.Setter;
import org.broadleafcommerce.common.money.Money;
import org.broadleafcommerce.core.order.service.call.OrderItemRequestDTO;

import java.math.BigDecimal;

public class BidRequestDTO extends OrderItemRequestDTO {

    @Getter
    @Setter
    private Money bidPrice;

}
