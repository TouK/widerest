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
import java.util.Date;
import java.util.List;

/*
 * TODO: (mst) Add SkuMedia (= pictures etc)
 */

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

    @ApiModelProperty
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    private String name;

    @ApiModelProperty
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    private String description;

    @ApiModelProperty(required = true)
    private BigDecimal salePrice;

    @ApiModelProperty(required = true)
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    private Integer quantityAvailable;

    @ApiModelProperty
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    private String taxCode;

    @ApiModelProperty(required = true)
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    private Date activeStartDate;

    @ApiModelProperty
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    private Date activeEndDate;
}
