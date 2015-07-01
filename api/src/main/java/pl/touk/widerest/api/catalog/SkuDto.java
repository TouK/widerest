package pl.touk.widerest.api.catalog;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Builder;

import java.math.BigDecimal;
import java.util.List;

@ApiModel
@Data
@Builder
public class SkuDto {

    @ApiModelProperty
    private Long id;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    @ApiModelProperty
    private List<ProductOptionSelection> selection;

    @ApiModelProperty
    private String description;

    @ApiModelProperty
    private BigDecimal price;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    @ApiModelProperty
    private Integer quantityAvailable;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    @ApiModelProperty
    private String code;

}
