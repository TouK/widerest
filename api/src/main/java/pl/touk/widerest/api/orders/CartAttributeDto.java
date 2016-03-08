package pl.touk.widerest.api.orders;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "Cart Attribute", description = "Cart Attribute DTO resource description")
public class CartAttributeDto {

    @ApiModelProperty(position = 0, value = "Name of the attribute", required = true, dataType = "java.lang.String")
    private String name;

    @ApiModelProperty(position = 1, value = "Value of the attribute", required = true, dataType = "java.lang.String")
    private String value;
}