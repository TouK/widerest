package pl.touk.widerest.api.catalog.products.dto;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonRootName("bundle")
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "Product Bundle", description = "Product Bundle Dto resource representation")
public class ProductBundleDto extends ProductDto {

    @ApiModelProperty(position = 0, value = "Sale price of this bundle", required = true, dataType = "java.math.BigDecimal")
    private BigDecimal bundleSalePrice;

    @ApiModelProperty(position = 1, value = "Retail price of this bundle", required = true, dataType = "java.math.BigDecimal")
    private BigDecimal bundleRetailPrice;

    @ApiModelProperty(position = 2, value = "Potential savings when buying items as a bundle instead of separately", dataType = "java.math.BigDecimal")
    private BigDecimal potentialSavings;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    @ApiModelProperty(position = 3, value = "Items belonging to this bundle", dataType = "java.util.List")
    private List<BundleItemDto> bundleItems;
}
