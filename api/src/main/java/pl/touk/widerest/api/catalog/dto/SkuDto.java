package pl.touk.widerest.api.catalog.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Builder;
import org.springframework.hateoas.ResourceSupport;


import java.math.BigDecimal;
import java.util.List;

@ApiModel
@Data
@Builder
public class SkuDto extends ResourceSupport {

    @ApiModelProperty
    private Long skuId;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    @ApiModelProperty
    private List<ProductOptionSelectionDto> selection;

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
