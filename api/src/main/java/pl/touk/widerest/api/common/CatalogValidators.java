package pl.touk.widerest.api.common;

import java.math.BigDecimal;

public class CatalogValidators {

    public static void validateSkuPrices(final BigDecimal salePrice, final BigDecimal retailPrice) throws DtoValidationException {

        if(salePrice == null && retailPrice == null) {
            throw new DtoValidationException("Product's SKU has to have a price");
        }

        if((salePrice != null && salePrice.longValue() < 0) ||
                (retailPrice != null && retailPrice.longValue() < 0)) {
            throw new DtoValidationException("Sku's prices cannot be negative");
        }
    }
}
