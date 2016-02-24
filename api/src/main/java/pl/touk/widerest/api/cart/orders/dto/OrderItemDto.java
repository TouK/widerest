package pl.touk.widerest.api.cart.orders.dto;


import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

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

    @ApiModelProperty(position = 3, value = "Href of a SKU to be added into the order", required = true, dataType = "java.lang.String")
    private String skuHref;

    /* (mst) ProductHref + Options */
    @ApiModelProperty(position = 4, value = "Href of a product to be added into the order", required = true, dataType = "java.lang.String")
    private String productHref;

    @ApiModelProperty(position = 5, value = "A list of selected options for the product", required = true, dataType = "java.util.List")
    private List<OrderItemOptionDto> selectedProductOptions;
}