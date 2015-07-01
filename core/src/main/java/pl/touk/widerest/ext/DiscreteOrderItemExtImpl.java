package pl.touk.widerest.ext;

import org.broadleafcommerce.common.money.Money;
import org.broadleafcommerce.core.order.domain.DiscreteOrderItemImpl;

import javax.persistence.Entity;

@Entity
public class DiscreteOrderItemExtImpl extends DiscreteOrderItemImpl {

    @Override
    public void setBaseRetailPrice(Money baseRetailPrice) {
        this.baseRetailPrice = baseRetailPrice == null ? null : baseRetailPrice.getAmount();
    }
}
