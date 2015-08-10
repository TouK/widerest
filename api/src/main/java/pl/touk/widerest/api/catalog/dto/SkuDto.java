package pl.touk.widerest.api.catalog.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Builder;

import org.broadleafcommerce.core.catalog.domain.ProductOptionValue;
import org.springframework.hateoas.ResourceSupport;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/*
 * TODO: (mst) Add SkuMedia (= pictures etc)
 */

@ApiModel
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
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
    private String name;

    @ApiModelProperty
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    private String description;

    @ApiModelProperty(required = true)
    private BigDecimal salePrice;

    @ApiModelProperty
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    private BigDecimal retailPrice;

    @ApiModelProperty(required = true)
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    private Integer quantityAvailable;

    @ApiModelProperty(required = true)
    private String currencyCode;

    @ApiModelProperty
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    private String taxCode;

    @ApiModelProperty(required = true)
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    private Date activeStartDate;

    @ApiModelProperty
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    private Date activeEndDate;

    @ApiModelProperty
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    private Map<String, String> skuAttributes;

    @ApiModelProperty
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    private Set<SkuProductOptionValueDto> skuProductOptionValues;

    @ApiModelProperty
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    private Set<ProductOptionValueDto> productOptionValues;

    @ApiModelProperty
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    private List<SkuMediaDto> skuMedia;

}