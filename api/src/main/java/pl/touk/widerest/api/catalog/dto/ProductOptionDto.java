package pl.touk.widerest.api.catalog.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Builder;

import java.util.List;

/**
 * Created by mst on 06.07.15.
 */
@ApiModel
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductOptionDto {

    @ApiModelProperty
    private String name;

    @ApiModelProperty
    private List<String> allowedValues;
}
