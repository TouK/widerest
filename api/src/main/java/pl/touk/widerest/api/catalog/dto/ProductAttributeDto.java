package pl.touk.widerest.api.catalog.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Builder;

/**
 * Created by mst on 28.08.15.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "Product Attribute", description = "Product Attribute DTO resource representation")
public class ProductAttributeDto {

    @ApiModelProperty(position = 0, value = "Name of the attribute", required = true, dataType = "java.lang.String")
    private String attributeName;

    @ApiModelProperty(position = 1, value = "Value of the attribute", required = true, dataType = "java.lang.String")
    private String attributeValue;
}
