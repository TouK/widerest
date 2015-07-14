package pl.touk.widerest.api.cart.dto;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Created by mst on 08.07.15.
 */
@Data
@ApiModel("cartAttributes")
public class CartAttributeDto {
    @ApiModelProperty
    private String name;
    @ApiModelProperty
    private String value;
}
