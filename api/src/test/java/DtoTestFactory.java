import pl.touk.widerest.api.catalog.dto.CategoryDto;

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
}
