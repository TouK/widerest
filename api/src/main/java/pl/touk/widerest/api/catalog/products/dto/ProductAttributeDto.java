package pl.touk.widerest.api.catalog.products.dto;

import com.fasterxml.jackson.annotation.JsonRootName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;
import pl.touk.widerest.api.BaseDto;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonRootName("attribute")
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "Product Attribute", description = "Product Attribute DTO resource representation")
public class ProductAttributeDto extends BaseDto {

    @ApiModelProperty(position = 0, value = "Name of the attribute", required = true, dataType = "java.lang.String")
    private String attributeName;

    @ApiModelProperty(position = 1, value = "Value of the attribute", required = true, dataType = "java.lang.String")
    private String attributeValue;
}
