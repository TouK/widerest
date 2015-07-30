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
    private static long productCounter = 0;
    private static long skuCounter = 0;

    public static final String TEST_CATEGORY_DEFAULT_NAME = "TestCategoryName";
    public static final String TEST_PRODUCT_DEFAULT_NAME = "DefaultTestProduct";
    public static final String TEST_DEFAULT_SKU_DESC = "DefaultTestProductDescription";
    public static final String TEST_ADDITIONAL_SKU_DESC = "TestAdditionalSKUDescription";

    static {
        Calendar gmtCal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        defaultActiveStartDate = gmtCal.getTime();
    }

    public static CategoryDto getTestCategory(DtoTestType dtoTestType) {
        switch(dtoTestType) {
            case SAME:
                return testCategory();
            case NEXT:
                return nextTestCategory();
            default:
                return null;
        }
    }

    private static CategoryDto testCategory() {
        if(newCategoryDto == null) {
            newCategoryDto = CategoryDto.builder()
                    .name(TEST_CATEGORY_DEFAULT_NAME)
                    .description("TestCategoryDescription")
                    .longDescription("TestCategoryLongDescription")
                    .build();
        }

        return newCategoryDto;
    }

    private static CategoryDto nextTestCategory() {
        CategoryDto nextCategoryDto = CategoryDto.builder()
                .name(TEST_CATEGORY_DEFAULT_NAME + categoryCounter)
                .description("TestCategoryDescription" + categoryCounter)
                .longDescription("TestCategoryLongDescription")
                .build();

        categoryCounter++;

        return nextCategoryDto;
    }


    private static ProductDto testProduct() {
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

    private static ProductDto nextTestProduct() {
        ProductDto nextProductDto = ProductDto.builder()
                .name(TEST_PRODUCT_DEFAULT_NAME + productCounter)
                .description("DefaultTestProductDescription" + productCounter)
                .longDescription("DefaultTestProductLongDescription" + productCounter)
                .manufacturer("Test Product Manufacturer" + productCounter)
                .model("Test Product Model" + productCounter)
                .defaultSku(null)
                .build();

        productCounter++;

        return nextProductDto;
    }


    public static ProductDto getTestProductWithDefaultSKUandCategory(DtoTestType dtoTestType) {
        switch(dtoTestType) {
            case NEXT:
                ProductDto p = nextTestProduct();
                p.setDefaultSku(nextTestDefaultSku());
                p.setCategoryName(nextTestCategory().getName());
                return p;
            case SAME: {
                if (fullProductDto == null) {
                    fullProductDto = testProduct();
                    fullProductDto.setDefaultSku(getTestDefaultSku());
                    fullProductDto.setCategoryName(testCategory().getName());
                }
                return fullProductDto;
            }
            default:
                return null;
        }

    }

    public static ProductDto getTestProductWithoutDefaultCategory(DtoTestType dtoTestType) {

        switch(dtoTestType) {
            case NEXT:
                ProductDto p = nextTestProduct();
                p.setDefaultSku(nextTestDefaultSku());
                return p;
            case SAME: {
                if(defaultProductWithDefaultSKU == null) {
                    defaultProductWithDefaultSKU = testProduct();
                    defaultProductWithDefaultSKU.setDefaultSku(getTestDefaultSku());
                }
                return defaultProductWithDefaultSKU;
            }

            default:
                return null;
        }
    }

    public static SkuDto getTestAdditionalSku(DtoTestType dtoTestType) {
        switch (dtoTestType) {
            case NEXT:
                return nextTestAdditionalSku();
            case SAME:
                return testAdditionalSku();
            default:
                return null;
        }
    }


    public static ProductDto getTestProductWithoutDefaultSKU() {
        if(defaultProductWithoutSku == null) {
            defaultProductWithoutSku = testProduct();
        }

        return defaultProductWithoutSku;
    }

    public static SkuDto getTestDefaultSku() {
        if(newSkuDto == null) {
            newSkuDto = SkuDto.builder()
                    .description(TEST_DEFAULT_SKU_DESC)
                    .name(TEST_PRODUCT_DEFAULT_NAME)
                    .salePrice(new BigDecimal(39.99))
                    .quantityAvailable(99)
                    .taxCode("DefaultSKU Tax Code")
                    .activeStartDate(defaultActiveStartDate)
                    .build();
        }
        return newSkuDto;
    }

    private static SkuDto nextTestDefaultSku() {
        SkuDto skuDto = SkuDto.builder()
                .description(TEST_DEFAULT_SKU_DESC + skuCounter)
                .name(TEST_PRODUCT_DEFAULT_NAME)
                .salePrice(new BigDecimal(39.99))
                .quantityAvailable(99)
                .taxCode("DefaultSKU Tax Code")
                .activeStartDate(defaultActiveStartDate)
                .build();


        skuCounter++;

        return skuDto;
    }

    private static SkuDto testAdditionalSku() {
        if(newSkuDto2 == null) {
            newSkuDto2 = SkuDto.builder()
                    .description(TEST_ADDITIONAL_SKU_DESC)
                    .salePrice(new BigDecimal(99.99))
                    .quantityAvailable(34)
                    .taxCode("AdditionalSKU Tax Code")
                    .activeStartDate(defaultActiveStartDate)
                    .build();
        }
        return newSkuDto2;
    }

    private static SkuDto nextTestAdditionalSku() {
        SkuDto skuDto = SkuDto.builder()
                .description(TEST_ADDITIONAL_SKU_DESC + skuCounter)
                .salePrice(new BigDecimal(3 + skuCounter))
                .quantityAvailable((int) (3 + skuCounter))
                .taxCode("AdditionalSKU Tax Code" + skuCounter)
                .activeStartDate(defaultActiveStartDate)
                .build();

        skuCounter++;


        return skuDto;


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
