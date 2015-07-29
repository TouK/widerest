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
    private static ProductDto defaultProductWithoutSku;
    private static ProductDto fullProductDto;
    private static ProductDto defaultProductWithDefaultSKU;
    private static SkuDto newSkuDto;
    private static SkuDto newSkuDto2;
    private static OrderDto newOrderDto;

    private static final Date defaultActiveStartDate;

    private static long categoryCounter = 0;

    public static final String TEST_CATEGORY_DEFAULT_NAME = "TestCategoryName";
    public static final String TEST_PRODUCT_DEFAULT_NAME = "DefaultTestProduct";

    static {
        Calendar gmtCal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        defaultActiveStartDate = gmtCal.getTime();
    }


    public static CategoryDto getTestCategory() {
        if(newCategoryDto == null) {
            newCategoryDto = CategoryDto.builder()
                    .name(TEST_CATEGORY_DEFAULT_NAME)
                    .description("TestCategoryDescription")
                    .longDescription("TestCategoryLongDescription")
                    .build();
        }

        return newCategoryDto;
    }

    public static CategoryDto getNextTestCategory() {
        CategoryDto nextCategoryDto = CategoryDto.builder()
                .name(TEST_CATEGORY_DEFAULT_NAME + categoryCounter)
                .description("TestCategoryDescription" + categoryCounter)
                .longDescription("TestCategoryLongDescription")
                .build();

        categoryCounter++;

        return nextCategoryDto;
    }

    private static ProductDto getTestProduct() {
        ProductDto defaultProductDto = ProductDto.builder()
                    .name(TEST_PRODUCT_DEFAULT_NAME)
                    .description("DefaultTestProductDescription")
                    .longDescription("DefaultTestProductLongDescription")
                    .manufacturer("Test Product Manufacturer")
                    .model("Test Product Model")
                    .defaultSku(null)
                    .build();

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
                    .description("DefaultTestProductDescription")
                    .name(TEST_PRODUCT_DEFAULT_NAME)
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
