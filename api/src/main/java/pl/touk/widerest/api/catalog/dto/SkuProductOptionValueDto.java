package pl.touk.widerest.api.catalog.dto;

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
@ApiModel(value = "Sku Product Option Value", description = "Describes a single product option associated with a particular SKU")
public class SkuProductOptionValueDto {

    @ApiModelProperty(position = 0, value = "Name of the product option", required = true, dataType = "java.lang.String")
    private String attributeName;

    @ApiModelProperty(position = 1, value = "Value of the product option. It must be one of the allowable value for that specific product option",
            required = true, dataType = "java.lang.String")
    private String attributeValue;
}
