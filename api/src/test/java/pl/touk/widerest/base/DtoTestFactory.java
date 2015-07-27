package pl.touk.widerest.base;

import pl.touk.widerest.api.cart.dto.OrderDto;
import pl.touk.widerest.api.catalog.dto.CategoryDto;
import pl.touk.widerest.api.catalog.dto.ProductDto;
import pl.touk.widerest.api.catalog.dto.SkuDto;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by mst on 20.07.15.
 */
public class DtoTestFactory {

    private static CategoryDto newCategoryDto;
    private static ProductDto defaultProductDto;
    private static ProductDto defaultProductWithoutSku;
    private static ProductDto fullProductDto;
    private static ProductDto defaultProductWithDefaultSKU;
    private static SkuDto newSkuDto;
    private static SkuDto newSkuDto2;
    private static OrderDto newOrderDto;

    private static final Date defaultActiveStartDate;

    static {
        Calendar gmtCal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        defaultActiveStartDate = gmtCal.getTime();
    }


    public static CategoryDto getTestCategory() {
        if(newCategoryDto == null) {
            newCategoryDto = CategoryDto.builder()
                    .name("TestCategoryName")
                    .description("TestCategoryDescription")
                    .longDescription("TestCategoryLongDescription")
                    .build();
        }

        return newCategoryDto;
    }

    private static ProductDto getTestProduct() {
        if(defaultProductDto == null) {
            defaultProductDto = ProductDto.builder()
                    .name("testProduct")
                    .description("testProductDescription")
                    .longDescription("testProductLongDescription")
                    .manufacturer("Test Product Manufacturer")
                    .model("Test Product Model")
                    .build();
        }

        return defaultProductDto;
    }

    public static ProductDto getTestProductWithDefaultSKUandCategory() {
        if(fullProductDto == null) {
            fullProductDto = getTestProduct();
            fullProductDto.setDefaultSku(getTestDefaultSku());
            fullProductDto.setCategoryName(getTestCategory().getName());
        }
        return fullProductDto;
    }

    public static ProductDto getTestProductWithoutDefaultCategory() {
        if(defaultProductWithDefaultSKU == null) {
            defaultProductWithDefaultSKU = getTestProduct();
            defaultProductWithDefaultSKU.setDefaultSku(getTestDefaultSku());
        }
        return defaultProductWithDefaultSKU;
    }

    public static ProductDto getTestProductWithoutDefaultSKU() {
        if(defaultProductWithoutSku == null) {
            defaultProductWithoutSku = getTestProduct();
        }

        return defaultProductWithoutSku;
    }

    public static SkuDto getTestDefaultSku() {
        if(newSkuDto == null) {
            newSkuDto = SkuDto.builder()
                    .description("TestDefaultSKUDescription")
                    .salePrice(new BigDecimal(39.99))
                    .quantityAvailable(99)
                    .taxCode("DefaultSKU Tax Code")
                    .activeStartDate(defaultActiveStartDate)
                    .build();
        }
        return newSkuDto;
    }

    public static SkuDto getTestAdditionalSku() {
        if(newSkuDto2 == null) {
            newSkuDto2 = SkuDto.builder()
                    .description("TestAdditionalSKUDescription")
                    .salePrice(new BigDecimal(99.99))
                    .quantityAvailable(34)
                    .taxCode("AdditionalSKU Tax Code")
                    .activeStartDate(defaultActiveStartDate)
                    .build();
        }
        return newSkuDto2;
    }

    public static OrderDto getTestOrder() {
        if(newOrderDto == null) {
            newOrderDto = OrderDto.builder()
                /*...*/
                    .build();
        }
        return newOrderDto;
    }
}
