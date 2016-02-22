package pl.touk.widerest.api.catalog;

import org.apache.commons.lang.StringUtils;
import pl.touk.widerest.api.catalog.exceptions.DtoValidationException;
import pl.touk.widerest.api.catalog.products.dto.MediaDto;
import pl.touk.widerest.api.catalog.products.dto.ProductDto;
import pl.touk.widerest.api.catalog.products.dto.SkuDto;

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

    public static void validateProductDto(final ProductDto productDto) throws DtoValidationException {

        validateSkuPrices(productDto.getSalePrice(), productDto.getRetailPrice());

        if(StringUtils.isEmpty(productDto.getName())) {
            throw new DtoValidationException("Product has to have a name");
        }
    }

    public static void validateMediaDto(final MediaDto mediaDto) throws DtoValidationException {
        if(StringUtils.isEmpty(mediaDto.getUrl())) {
            throw new DtoValidationException("Media has to have an URL");
        }
    }

    public static void validateSkuDto(final SkuDto skuDto) throws DtoValidationException {

        if (StringUtils.isEmpty(skuDto.getName())) {
            throw new DtoValidationException("Sku has to have a name");
        }

        if(skuDto.getQuantityAvailable() == null) {
            throw new DtoValidationException("Sku has to have available quantity set");
        }

        if(skuDto.getActiveStartDate() == null) {
            throw new DtoValidationException("Sku has to have an Active Start Date set");
        }

        CatalogValidators.validateSkuPrices(skuDto.getSalePrice(), skuDto.getRetailPrice());
    }

}
