package pl.touk.widerest.base;

import pl.touk.widerest.api.cart.dto.OrderDto;
import pl.touk.widerest.api.catalog.dto.CategoryDto;
import pl.touk.widerest.api.catalog.dto.MediaDto;
import pl.touk.widerest.api.catalog.dto.ProductBundleDto;
import pl.touk.widerest.api.catalog.dto.ProductDto;
import pl.touk.widerest.api.catalog.dto.ProductOptionDto;
import pl.touk.widerest.api.catalog.dto.SkuDto;
import pl.touk.widerest.api.catalog.dto.SkuProductOptionValueDto;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

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

    private static MediaDto newMediaDto;

    private static final Date defaultActiveStartDate;

    private static long categoryCounter = 0;
    private static long productCounter = 0;
    private static long skuCounter = 0;
    private static AtomicLong skuMediaCounter;

    public static final String TEST_CATEGORY_DEFAULT_NAME = "TestCategoryName";
    public static final String TEST_PRODUCT_DEFAULT_NAME = "DefaultTestProduct";
    public static final String TEST_DEFAULT_SKU_DESC = "DefaultTestProductDescription";
    public static final String TEST_ADDITIONAL_SKU_DESC = "TestAdditionalSKUDescription";
    public static final String TEST_ADDITIONAL_SKU_NAME = "TestAdditionalSKUName";
    public static final String TEST_BUNDLE_DEFAULT_NAME = "TestBundleName";

    static {
        Calendar gmtCal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        defaultActiveStartDate = gmtCal.getTime();

        skuMediaCounter = new AtomicLong(0);
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

    public static MediaDto getTestSkuMedia(DtoTestType dtoTestType) {
        switch (dtoTestType) {
            case NEXT:
                return nextTestMediaDto();
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
                    .offerMessage("Test Product Offer Message")
                    .defaultSku(null)
                    .options(Arrays.asList(new ProductOptionDto("TESTOPTION", Arrays.asList("test1", "test2"))))
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
                .offerMessage("Test Product Offer Message" + productCounter)
                .defaultSku(null)
                .options(Arrays.asList(new ProductOptionDto("TESTOPTION", Arrays.asList("test1", "test2"))))
                .categoryName(null)
                .validFrom(defaultActiveStartDate)
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

    public static ProductBundleDto getTestBundle(DtoTestType dtoTestType) {
        switch(dtoTestType) {
            case NEXT:
                return nextTestProductBundle();
            default:
                return null;
        }
    }


    public static ProductBundleDto nextTestProductBundle() {


        ProductDto productBundleDto = new ProductBundleDto();


        productBundleDto.setName(TEST_BUNDLE_DEFAULT_NAME + productCounter);
        productBundleDto.setDescription("DefaultTestBundleDescription" + productCounter);
        productBundleDto.setLongDescription("DefaultTestBundleLongDescription" + productCounter);
        productBundleDto.setManufacturer("Test Bundle Manufacturer" + productCounter);
        productBundleDto.setModel("Test Bundle Model" + productCounter);
        productBundleDto.setOfferMessage("Test Bundle Offer Message" + productCounter);
        productBundleDto.setDefaultSku(nextTestDefaultSku());
        productBundleDto.setOptions(Arrays.asList(new ProductOptionDto("TESTOPTION", Arrays.asList("test1", "test2"))));
        productBundleDto.setValidFrom(defaultActiveStartDate);

        ((ProductBundleDto)productBundleDto).setBundleSalePrice(new BigDecimal("19.99"));
        ((ProductBundleDto)productBundleDto).setBundleRetailPrice(new BigDecimal("29.99"));



        productCounter++;

        return (ProductBundleDto)productBundleDto;
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
                    .salePrice(new BigDecimal("39.99"))
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
                .salePrice(new BigDecimal("39.99"))
                .quantityAvailable(99)
                .taxCode("DefaultSKU Tax Code")
                .activeStartDate(defaultActiveStartDate)
                .build();


        skuCounter++;

        return skuDto;
    }

    private static SkuDto testAdditionalSku() {
        if(newSkuDto2 == null) {

            Set<SkuProductOptionValueDto> h = new HashSet<>();
            h.add(new SkuProductOptionValueDto("TESTOPTION", "test1"));

            newSkuDto2 = SkuDto.builder()
                    .name(TEST_ADDITIONAL_SKU_NAME)
                    .description(TEST_ADDITIONAL_SKU_DESC)
                    .salePrice(new BigDecimal(99.99))
                    .quantityAvailable(34)
                    .taxCode("AdditionalSKU Tax Code")
                    .activeStartDate(defaultActiveStartDate)
                    .skuProductOptionValues(h)
                    .build();
        }
        return newSkuDto2;
    }

    private static SkuDto nextTestAdditionalSku() {
        Set<SkuProductOptionValueDto> h = new HashSet<>();
        h.add(new SkuProductOptionValueDto("TESTOPTION", "test1"));

        SkuDto skuDto = SkuDto.builder()
                .name(TEST_ADDITIONAL_SKU_NAME + skuCounter)
                .description(TEST_ADDITIONAL_SKU_DESC + skuCounter)
                .salePrice(new BigDecimal(String.valueOf(3 + skuCounter) + ".00"))
                .quantityAvailable((int) (3 + skuCounter))
                .taxCode("AdditionalSKU Tax Code" + skuCounter)
                .activeStartDate(defaultActiveStartDate)
                .skuProductOptionValues(h)
                .build();

        skuCounter++;


        return skuDto;


    }


    private static MediaDto nextTestMediaDto() {

        long currentSkuMediaCounter = skuMediaCounter.incrementAndGet();

        return MediaDto.builder()
                    .altText("Test Media Alt Text" + currentSkuMediaCounter)
                    .tags("Test Media Tags" + currentSkuMediaCounter)
                    .title("Test Media Title" + currentSkuMediaCounter)
                    .url("http://localhost:8080/images/testmedia" + currentSkuMediaCounter + ".png")
                    .build();
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
