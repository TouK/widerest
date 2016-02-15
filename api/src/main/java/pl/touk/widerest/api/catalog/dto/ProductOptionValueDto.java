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
@ApiModel(value = "Product Option Value", description = "Product Option Value DTO resource representation")
public class ProductOptionValueDto {

    @ApiModelProperty(position = 0, value = "Product Option description", required = true,
            dataType = "pl.touk.widerest.api.catalog.dto.ProductOptionDto")
    private ProductOptionDto productOption;

    @ApiModelProperty(position = 1, value = "Attribute value for the specified product option", required = true, dataType = "java.lang.String")
    private String attributeValue;
}
