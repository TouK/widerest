package pl.touk.widerest.base;

import pl.touk.widerest.api.cart.dto.OrderDto;
import pl.touk.widerest.api.catalog.dto.CategoryDto;
import pl.touk.widerest.api.catalog.dto.ProductDto;
import pl.touk.widerest.api.catalog.dto.SkuDto;

import java.math.BigDecimal;

/**
 * Created by mst on 20.07.15.
 */
public class DtoTestFactory {

    private static CategoryDto newCategoryDto;
    private static ProductDto newProductDto;
    private static SkuDto newSkuDto;
    private static OrderDto newOrderDto;

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

    public static ProductDto getTestProduct() {
        if(newProductDto == null) {
            newProductDto = ProductDto.builder()
                    .name("testProduct")
                    .description("testProductDescription")
                    .longDescription("testProductLongDescription")
                    .defaultSku(getTestSku())
                    .category(getTestCategory())
                    .build();
        }

        return newProductDto;
    }

    public static SkuDto getTestSku() {
        if(newSkuDto == null) {
            newSkuDto = SkuDto.builder()
                    .description("testSkuDescription")
                    .price(new BigDecimal(99.99))
                    .quantityAvailable(99)
                    .build();
        }
        return newSkuDto;
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
