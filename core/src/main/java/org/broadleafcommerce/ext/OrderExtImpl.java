package org.broadleafcommerce.ext;

import org.broadleafcommerce.core.catalog.domain.Sku;
import org.broadleafcommerce.core.order.domain.BundleOrderItem;
import org.broadleafcommerce.core.order.domain.DiscreteOrderItem;
import org.broadleafcommerce.core.order.domain.OrderImpl;
import org.broadleafcommerce.core.order.domain.OrderItem;

import javax.persistence.Entity;

@Entity
public class OrderExtImpl extends OrderImpl {

    public boolean isWinningForSku(Sku sku) {
        for (OrderItem orderItem : getOrderItems()) {
            Sku orderItemSku = null;
            if (orderItem instanceof DiscreteOrderItem) {
                DiscreteOrderItem discreteOrderItem = (DiscreteOrderItem) orderItem;
                orderItemSku = discreteOrderItem.getSku();
            } else if (orderItem instanceof BundleOrderItem) {
                BundleOrderItem bundleOrderItem = (BundleOrderItem) orderItem;
                orderItemSku = bundleOrderItem.getSku();
            }
            if (orderItemSku != null && orderItemSku.equals(sku) && orderItem.getTotalPrice().compareTo(((SkuExtImpl)sku).getBidPrice()) >= 0) {
                return true;
            }
        }
        return false;
    }

}
