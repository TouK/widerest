package pl.touk.widerest.api.catalog.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Builder;
import org.springframework.hateoas.ResourceSupport;

import java.math.BigDecimal;

/**
 * Created by mst on 06.07.15.
 */
@ApiModel
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BundleItemDto extends ResourceSupport {
    private Integer quantity;

    @ApiModelProperty(required = true)
    private Long skuId;

    @ApiModelProperty(required = true)
    private BigDecimal salePrice;

}
