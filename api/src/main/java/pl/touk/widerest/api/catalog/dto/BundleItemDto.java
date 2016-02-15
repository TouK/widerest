package pl.touk.widerest.api.catalog.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Builder;
import org.springframework.hateoas.ResourceSupport;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "Bundle Item", description = "Bundle Item DTO resource representation")
public class BundleItemDto extends ResourceSupport {

    @ApiModelProperty(position = 0, value = "Quantity of this item", required = true, dataType = "java.lang.Integer")
    private Integer quantity;

    @ApiModelProperty(position = 1, value = "ID of the SKU", required = true, dataType = "java.lang.Long")
    private Long skuId;

    @ApiModelProperty(position = 2, value = "Sale price for this item when selling as part of a bundle", required = true, dataType = "java.math.BigDecimal")
    private BigDecimal salePrice;

}
