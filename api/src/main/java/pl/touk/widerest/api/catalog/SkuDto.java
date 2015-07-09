package pl.touk.widerest.api.catalog;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Builder;
import org.broadleafcommerce.core.catalog.domain.Sku;

import java.math.BigDecimal;
import java.util.Collections;
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
