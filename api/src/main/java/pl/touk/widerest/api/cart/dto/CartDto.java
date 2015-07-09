package pl.touk.widerest.api.cart.dto;

import com.wordnik.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.experimental.Builder;

import java.math.BigDecimal;
import java.util.List;

/**
 * Created by mst on 07.07.15.
 */
@Data
@Builder
@ApiModel("cart")
public class CartDto {

    private final List<OrderItemDto> items;
    private BigDecimal totalPrice;

    private List<CartAttributeDto> cartAttributeDtos;

}
