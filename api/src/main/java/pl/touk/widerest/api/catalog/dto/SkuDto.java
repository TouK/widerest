package pl.touk.widerest.api.catalog.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Builder;
import org.springframework.hateoas.ResourceSupport;


import java.math.BigDecimal;
import java.util.List;

@ApiModel
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkuDto extends ResourceSupport {

    @ApiModelProperty
    @JsonIgnore
    private Long skuId;

    /*
    @ApiModelProperty
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    private List<ProductOptionSelectionDto> selection;
*/

    @ApiModelProperty(required = true)
    private String description;

    @ApiModelProperty(required = true)
    private BigDecimal price;

    @ApiModelProperty(required = true)
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    private Integer quantityAvailable;

    @ApiModelProperty
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    private String code;

    @ApiModelProperty
    private String pictureUrl;
}
