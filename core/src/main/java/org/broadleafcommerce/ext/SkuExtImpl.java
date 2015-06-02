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
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;

@Entity
@Table(name = "BLC_EXT_SKU")
public class SkuExtImpl extends SkuImpl {

    @Column(name = "MINIMAL_PRICE", precision = 19, scale = 5)
    @AdminPresentation(friendlyName = "SkuImpl_Sku_Minimal_Price", order = 2000,
            group = ProductImpl.Presentation.Group.Name.Price, groupOrder = ProductImpl.Presentation.Group.Order.Price,
            prominent = true, gridOrder = 7,
            fieldType = SupportedFieldType.MONEY)
    protected BigDecimal minimalPrice;

    @Column(name = "BID_PRICE", precision = 19, scale = 5)
    @AdminPresentation(friendlyName = "SkuImpl_Sku_Bid_Price", order = 2000,
            group = ProductImpl.Presentation.Group.Name.Price, groupOrder = ProductImpl.Presentation.Group.Order.Price,
            prominent = true, gridOrder = 8,
            fieldType = SupportedFieldType.MONEY)
    protected BigDecimal bidPrice;

    public Money getMinimalPrice() {
        return new Money(minimalPrice == null ? BigDecimal.ZERO : minimalPrice, getCurrency());
    }

    public void setMinimalPrice(Money minimalPrice) {
        this.minimalPrice = Money.toAmount(minimalPrice);
    }

    public Money getBidPrice() {
        return new Money(bidPrice == null ? BigDecimal.ZERO : bidPrice, getCurrency());
    }

    public void setBidPrice(Money bidPrice) {
        this.bidPrice = Money.toAmount(bidPrice);
    }

    public Money getNextBidPrice() {
        BigDecimal amount = bidPrice == null ? BigDecimal.ZERO : bidPrice;
        amount = amount.round(new MathContext(2, RoundingMode.HALF_EVEN));
        if (amount.scale() > 0)
            amount = amount.setScale(0, RoundingMode.HALF_EVEN);
        amount = BigDecimal.valueOf(amount.unscaledValue().add(BigInteger.ONE).longValue(), amount.scale());
        return new Money(amount, getCurrency());
    }

    @Override
    public Money getRetailPrice() {
        Money tmpRetailPrice = getRetailPriceInternal();
//        if (tmpRetailPrice == null) {
//            throw new IllegalStateException("Retail price on Sku with id " + getId() + " was null");
//        }
        return tmpRetailPrice;
    }

    public boolean isForAuction() {
        return (this.minimalPrice != null);
    }

    public boolean isForSale() {
        Money retailPrice = getRetailPrice();
        Money salePrice = getSalePrice();
        Money bidPrice = getBidPrice();
        Money minimalPrice = getMinimalPrice();
        return ((retailPrice != null && !retailPrice.isZero()) || (salePrice != null && !salePrice.isZero()))
                && (!isForAuction() || bidPrice.lessThan(minimalPrice));
    }

}
