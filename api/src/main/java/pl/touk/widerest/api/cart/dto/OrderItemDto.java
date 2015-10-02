package pl.touk.widerest.api.cart.dto;


import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Builder;

/**
 * Created by mst on 07.07.15.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "Order Item", description = "Order Item DTO resource description")
public class OrderItemDto {

    @JsonIgnore
    private long itemId;

    @ApiModelProperty(position = 0, value = "Quantity of an item to be added into the order", required = true, dataType = "java.lang.Integer")
    private Integer quantity = 1;

    @ApiModelProperty(position = 1, value = "ID of a SKU to be added into the order", required = true, dataType = "java.lang.Long")
    private Long skuId;

    @ApiModelProperty(position = 2, value = "ID of a bundle to be added into the order", dataType = "java.lang.Long")
    private Long bundleProductId;
}
