package org.broadleafcommerce.ext;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import org.broadleafcommerce.common.money.Money;
import org.broadleafcommerce.common.presentation.AdminPresentation;
import org.broadleafcommerce.common.presentation.client.SupportedFieldType;
import org.broadleafcommerce.core.catalog.domain.ProductImpl;
import org.broadleafcommerce.core.catalog.domain.SkuImpl;
import org.broadleafcommerce.core.catalog.service.CatalogService;

import javax.annotation.Resource;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "BLC_EXT_SKU")
public class SkuExtImpl extends SkuImpl {

    @Column(name = "MINIMAL_PRICE", precision = 19, scale = 5)
    @AdminPresentation(friendlyName = "SkuImpl_Sku_Minimal_Price", order = 2000,
            group = ProductImpl.Presentation.Group.Name.Price, groupOrder = ProductImpl.Presentation.Group.Order.Price,
            prominent = true, gridOrder = 7,
            fieldType = SupportedFieldType.MONEY)
    @Getter(AccessLevel.NONE)
    protected BigDecimal minimalPrice;

    public Money getMinimalPrice() {
        return minimalPrice == null ? null : new Money(minimalPrice, getCurrency());
    }

    public void setMinimalPrice(Money minimalPrice) {
        this.minimalPrice = Money.toAmount(minimalPrice);
    }

    public Money getBidPrice() {
        return new Money(BigDecimal.valueOf(10), getCurrency());
    }

    public boolean isForAuction() {
        Money minimalPrice = getMinimalPrice();
        return (minimalPrice != null);
    }

    public boolean isForSale() {
        Money retailPrice = getRetailPrice();
        Money salePrice = getSalePrice();
        return (retailPrice != null && !retailPrice.isZero()) || (salePrice != null && !salePrice.isZero());
    }

    @Override
    public Money getRetailPrice() {
        Money tmpRetailPrice = getRetailPriceInternal();
//        if (tmpRetailPrice == null) {
//            throw new IllegalStateException("Retail price on Sku with id " + getId() + " was null");
//        }
        return tmpRetailPrice;
    }


}
