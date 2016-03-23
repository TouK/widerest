package pl.touk.widerest.api.products;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
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

    @ApiModelProperty(value = "Name of the option", required = true)
    private String name;

    @ApiModelProperty(value = "Type of the option")
    private String type;

    @ApiModelProperty(value = "Specifies if the option is required or not")
    private Boolean required;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    @ApiModelProperty(value = "List of all allowed values for this option", required = true)
    private List<String> allowedValues;
}
