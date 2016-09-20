package pl.touk.widerest.base;

import pl.touk.widerest.api.categories.CategoryDto;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class CategoryDtoFactory {
    public static final String TEST_CATEGORY_NAME = "TestCategoryName";
    public static final String TEST_CATEGORY_DESCRIPTION = "TestCategoryDescription";
    public static final String TEST_CATEGORY_LONG_DESCRIPTION= "TestCategoryLongDescription";

    private static final AtomicLong categoryCounter;

    static {
        categoryCounter = new AtomicLong(0);
    }

    public CategoryDto testCategoryDto() {
        final long categoryCounterValue = categoryCounter.getAndIncrement();

        return CategoryDto.builder()
                .name(TEST_CATEGORY_NAME.concat(String.valueOf(categoryCounterValue)))
                .description(TEST_CATEGORY_DESCRIPTION.concat(String.valueOf(categoryCounterValue)))
                .longDescription(TEST_CATEGORY_LONG_DESCRIPTION.concat(String.valueOf(categoryCounterValue)))
                .build();
    }

    public CategoryDto testCategoryDtoWithAttributes(final Map<String, String> categoryAttributes) {
        final CategoryDto categoryDto = testCategoryDto();
        categoryDto.setAttributes(categoryAttributes);
        return categoryDto;
    }
}
