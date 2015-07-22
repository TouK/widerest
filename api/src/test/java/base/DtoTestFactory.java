package base;

import pl.touk.widerest.api.cart.dto.OrderDto;
import pl.touk.widerest.api.catalog.dto.CategoryDto;
import pl.touk.widerest.api.catalog.dto.ProductDto;
import pl.touk.widerest.api.catalog.dto.SkuDto;

import java.math.BigDecimal;

/**
 * Created by mst on 20.07.15.
 */
public class DtoTestFactory {

    /* TODO: Implement Flyweight pattern instead of blindly creating the same NEW object every time */
    public static Object getDtoTestObject(DtoTestType dtoTestType) {

        Object returnObjectDto;

        switch(dtoTestType) {
            case CATEGORY_DTO:
                returnObjectDto = getTestCategory();
                break;
            case PRODUCT_DTO:
                returnObjectDto = getTestProduct();
                break;
            case SKU_DTO:
                returnObjectDto = getTestSku();
                break;
            case ORDER_DTO:
                returnObjectDto = getTestOrder();
                break;
            default:
                returnObjectDto = null;
        }

        return returnObjectDto;
    }

    private static CategoryDto getTestCategory() {

        CategoryDto newCategory = CategoryDto.builder()
                .name("TestCategoryName")
                .description("TestCategoryDescription")
                .longDescription("TestCategoryLongDescription")
                .build();

        return newCategory;
    }

    private static ProductDto getTestProduct() {

        ProductDto productDto = ProductDto.builder()
                .name("testProduct")
                .description("testProductDescription")
                .longDescription("testProductLongDescription")
                .defaultSku(getTestSku())
                .category(getTestCategory())
                .build();

        return productDto;
    }

    private static SkuDto getTestSku() {
        SkuDto newSku = SkuDto.builder()
                .description("testSkuDescription")
                .price(new BigDecimal(99.99))
                .quantityAvailable(99)
                .build();
        return newSku;
    }

    private static OrderDto getTestOrder() {
        OrderDto newOrder = OrderDto.builder()
                /*...*/
                .build();

        return newOrder;
    }
}
