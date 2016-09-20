package pl.touk.widerest.base;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.broadleafcommerce.core.catalog.service.type.ProductOptionType;
import pl.touk.widerest.api.products.ProductDto;
import pl.touk.widerest.api.products.ProductOptionDto;
import pl.touk.widerest.api.products.skus.SkuDto;
import pl.touk.widerest.api.products.skus.SkuProductOptionValueDto;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static pl.touk.widerest.base.DtoTestFactory.categories;

public class ProductDtoFactory {
    public static final String TEST_PRODUCT_NAME = "TestProductName";
    public static final String TEST_PRODUCT_DESCRIPTION= "TestProductDescription";

    public static final ProductOptionDto TEST_PRODUCT_OPTION1 = ProductOptionDto.builder()
            .name("OPTION_NAME1")
            .type(ProductOptionType.TEXT.getType())
            .required(false)
            .allowedValues(ImmutableList.of("OPTION_VALUE1", "OPTION_VALUE2"))
            .build();

    public static final List<ProductOptionDto> TEST_PRODUCT_OPTIONS = ImmutableList.of(TEST_PRODUCT_OPTION1);

    public static final String TEST_DEFAULT_SKU_NAME = "TestDefaultSkuName";
    public static final String TEST_ADDITIONAL_SKU_NAME = "TestAdditionalSkuName";
    public static final String TEST_DEFAULT_SKU_DESCRIPTION = "TestDefaultSkuDescription";
    public static final String TEST_ADDITIONAL_SKU_DESCRIPTION = "TestAdditionalSkuDescription";
    public static final String TEST_DEFAULT_SKU_TAX_CODE = "TestDefaultSkuTaxCode";
    public static final String TEST_ADDITIONAL_SKU_TAX_CODE = "TestAdditionalSkuTaxCode";
    public static final BigDecimal TEST_DEFAULT_SKU_RETAIL_PRICE = BigDecimal.valueOf(99.99);
    public static final BigDecimal TEST_DEFAULT_SKU_SALE_PRICE = BigDecimal.valueOf(79.99);
    public static final BigDecimal TEST_ADDITIONAL_SKU_RETAIL_PRICE = BigDecimal.valueOf(99.99);
    public static final BigDecimal TEST_ADDITIONAL_SKU_SALE_PRICE = BigDecimal.valueOf(79.99);
    public static final Integer TEST_DEFAULT_SKU_QUANTITY = 3;
    public static final Integer TEST_ADDiTIONAL_SKU_QUANTITY = 2;

    public static final SkuProductOptionValueDto SKU_PRODUCT_OPTION1_VALUE1 = SkuProductOptionValueDto.builder()
            .attributeName("OPTION_NAME1")
            .attributeValue("OPTION_VALUE1")
            .build();

    public static final SkuProductOptionValueDto SKU_PRODUCT_OPTION1_VALUE2 = SkuProductOptionValueDto.builder()
            .attributeName("OPTION_NAME1")
            .attributeValue("OPTION_VALUE2")
            .build();

    public static final Set<SkuProductOptionValueDto> TEST_DEFAULT_SKU_PRODUCT_OPTIONS = ImmutableSet.of(SKU_PRODUCT_OPTION1_VALUE1);
    public static final Set<SkuProductOptionValueDto> TEST_ADDITIONAL_SKU_PRODUCT_OPTION = ImmutableSet.of(SKU_PRODUCT_OPTION1_VALUE2);

    private static final AtomicLong productCounter;
    private static final AtomicLong skuCounter;
    private static final ZonedDateTime defaultActiveStartDate;

    static {
        productCounter = new AtomicLong(0);
        skuCounter = new AtomicLong(0);
        defaultActiveStartDate = ZonedDateTime.now();
    }

    public ProductDto testProductDto() {
        final long productCounterValue = productCounter.getAndIncrement();

        return ProductDto.builder()
                .name(TEST_PRODUCT_NAME.concat(String.valueOf(productCounterValue)))
                .description(TEST_PRODUCT_DESCRIPTION.concat(String.valueOf(productCounterValue)))
                .longDescription("DefaultTestProductLongDescription" + productCounter)
                .manufacturer("Test Product Manufacturer" + productCounter)
                .model("Test Product Model" + productCounter)
                .offerMessage("Test Product Offer Message" + productCounter)
                .options(TEST_PRODUCT_OPTIONS)
                .categoryName(null)
                .validFrom(defaultActiveStartDate)
                .build();
    }

    public ProductDto testProductWithDefaultSKUandCategory() {
        final ProductDto p = testProductDto();
        updateNextTestDefaultSku(p);
        p.setCategoryName(categories().testCategoryDto().getName());
        return p;
    }

    public ProductDto getTestProductWithoutDefaultCategory() {
        final ProductDto p = testProductDto();
        updateNextTestDefaultSku(p);
        return p;
    }

    public ProductDto testProductWithDefaultCategory(final String defaultCategoryName) {
        final ProductDto productDto = testProductDto();
        productDto.setCategoryName(defaultCategoryName);
        updateNextTestDefaultSku(productDto);
        return productDto;
    }

    public SkuDto testSkuDto() {
        final long skuCounterValue = skuCounter.getAndIncrement();

        return SkuDto.builder()
                .name(TEST_DEFAULT_SKU_NAME.concat(String.valueOf(skuCounterValue)))
                .description(TEST_DEFAULT_SKU_DESCRIPTION.concat(String.valueOf(skuCounterValue)))
                .retailPrice(TEST_DEFAULT_SKU_RETAIL_PRICE)
                .salePrice(TEST_DEFAULT_SKU_SALE_PRICE)
                .quantityAvailable(TEST_DEFAULT_SKU_QUANTITY)
                .isAvailable(true)
                .taxCode(TEST_DEFAULT_SKU_TAX_CODE.concat(String.valueOf(skuCounterValue)))
                .validFrom(defaultActiveStartDate)
                .skuProductOptionValues(TEST_DEFAULT_SKU_PRODUCT_OPTIONS)
                .build();
    }

    public SkuDto testAdditionalSkuDto() {
        final long additionalSkuCounterValue = skuCounter.getAndIncrement();

        return SkuDto.builder()
                .name(TEST_ADDITIONAL_SKU_NAME.concat(String.valueOf(additionalSkuCounterValue)))
                .description(TEST_ADDITIONAL_SKU_DESCRIPTION.concat(String.valueOf(additionalSkuCounterValue)))
                .retailPrice(TEST_ADDITIONAL_SKU_RETAIL_PRICE)
                .salePrice(TEST_ADDITIONAL_SKU_SALE_PRICE)
                .quantityAvailable(TEST_ADDiTIONAL_SKU_QUANTITY)
                .isAvailable(true)
                .taxCode(TEST_ADDITIONAL_SKU_TAX_CODE.concat(String.valueOf(additionalSkuCounterValue)))
                .validFrom(defaultActiveStartDate)
                .skuProductOptionValues(TEST_ADDITIONAL_SKU_PRODUCT_OPTION)
                .build();
    }

    private static void updateNextTestDefaultSku(final ProductDto productDto) {
        productDto.setName(TEST_PRODUCT_NAME);
        productDto.setDescription(TEST_PRODUCT_DESCRIPTION + skuCounter);
        productDto.setValidFrom(defaultActiveStartDate);
        productDto.setRetailPrice(new BigDecimal("39.99"));
        productDto.setQuantityAvailable(99);
        productDto.setTaxCode("DefaultSKU Tax Code");
    }
}
