package pl.touk.widerest.base;

import pl.touk.widerest.api.common.MediaDto;
import pl.touk.widerest.api.products.ProductBundleDto;
import pl.touk.widerest.api.products.ProductDto;

import java.util.concurrent.atomic.AtomicLong;

public class DtoTestFactory {
//    public static final String TEST_BUNDLE_DEFAULT_NAME = "TestBundleName";
    private static final CategoryDtoFactory CATEGORY_DTO_FACTORY = new CategoryDtoFactory();
    private static final ProductDtoFactory PRODUCT_DTO_FACTORY = new ProductDtoFactory();

    private static final AtomicLong skuMediaCounter;

    public static CategoryDtoFactory categories() {
        return CATEGORY_DTO_FACTORY;
    }
    public static ProductDtoFactory products() { return PRODUCT_DTO_FACTORY; }

    static {
        skuMediaCounter = new AtomicLong(0);
    }

    public static MediaDto getTestSkuMedia() {
        return nextTestMediaDto();
    }

    public static ProductBundleDto getTestBundle() {
        return nextTestProductBundle();
    }

    public static ProductBundleDto nextTestProductBundle() {


        ProductDto productBundleDto = new ProductBundleDto();


//        productBundleDto.setName(TEST_BUNDLE_DEFAULT_NAME + productCounter);
//        productBundleDto.setDescription("DefaultTestBundleDescription" + productCounter);
//        productBundleDto.setLongDescription("DefaultTestBundleLongDescription" + productCounter);
//        productBundleDto.setManufacturer("Test Bundle Manufacturer" + productCounter);
//        productBundleDto.setModel("Test Bundle Model" + productCounter);
//        productBundleDto.setOfferMessage("Test Bundle Offer Message" + productCounter);
//
//        //productBundleDto.setDefaultSku(nextTestDefaultSku());
//        productBundleDto.setOptions(Arrays.asList(new ProductOptionDto("TESTOPTION", ProductOptionType.TEXT.getType(), false, Arrays.asList("test1", "test2"))));
//        productBundleDto.setValidFrom(defaultActiveStartDate);
//
//        ((ProductBundleDto) productBundleDto).setBundleSalePrice(new BigDecimal("19.99"));
//        ((ProductBundleDto) productBundleDto).setBundleRetailPrice(new BigDecimal("29.99"));
//
//
//        productCounter++;

        return (ProductBundleDto) productBundleDto;
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
}
