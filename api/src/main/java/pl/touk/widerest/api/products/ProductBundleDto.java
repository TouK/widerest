package pl.touk.widerest.api.products;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonRootName("bundle")
@ApiModel(value = "Product Bundle", description = "Product Bundle Dto resource representation")
public class ProductBundleDto extends ProductDto {

    @ApiModelProperty(position = 0, value = "Sale price of this bundle", required = true, dataType = "java.math.BigDecimal")
    private BigDecimal bundleSalePrice;

    @ApiModelProperty(position = 1, value = "Retail price of this bundle", required = true, dataType = "java.math.BigDecimal")
    private BigDecimal bundleRetailPrice;

    @ApiModelProperty(position = 2, value = "Potential savings when buying items as a bundle instead of separately", dataType = "java.math.BigDecimal")
    private BigDecimal potentialSavings;

    @ApiModelProperty(position = 3, value = "Items belonging to this bundle", dataType = "java.util.List")
    private List<BundleItemDto> bundleItems;
}
