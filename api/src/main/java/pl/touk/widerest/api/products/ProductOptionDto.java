package pl.touk.widerest.api.products;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "Product Option", description = "Product Option DTO resource representation")
public class ProductOptionDto {

    @ApiModelProperty(position = 0, value = "Name of an option", required = true, dataType = "java.lang.String")
    private String name;

    @ApiModelProperty(position = 1, value = "List of all allowed values for this option", required = true, dataType = "java.util.List")
    private List<String> allowedValues;
}
