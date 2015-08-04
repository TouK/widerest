package pl.touk.widerest.api.cart.dto;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Builder;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Created by mst on 07.07.15.
 */
@Data
@Builder
@ApiModel
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDto {

    @JsonIgnore
    private long itemId;


    @ApiModelProperty(required = true)
    private int quantity = 1;

    @ApiModelProperty(required = true)
    private long skuId;

    /* (mst) do we need attributes when dealing only with SKUs? */
    @ApiModelProperty
    private Map attributes;
}