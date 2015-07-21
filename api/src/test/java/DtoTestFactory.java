import pl.touk.widerest.api.cart.dto.OrderDto;
import pl.touk.widerest.api.catalog.dto.CategoryDto;
import pl.touk.widerest.api.catalog.dto.ProductDto;
import pl.touk.widerest.api.catalog.dto.SkuDto;

/**
 * Created by mst on 20.07.15.
 */
public class DtoTestFactory {

    /* Implement Flyweight pattern ??? */
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
        return null;
    }

    private static SkuDto getTestSku() {
        return null;
    }

    private static OrderDto getTestOrder() {
        return null;
    }
}
