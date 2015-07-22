package pl.touk.widerest.api.catalog.dto;


import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Builder;
import org.broadleafcommerce.core.catalog.domain.ProductOptionValue;

/**
 * Created by mst on 06.07.15.
 */
@ApiModel
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductOptionSelectionDto {

    private String optionName;

    private String value;

    public ProductOptionSelectionDto(ProductOptionValue selection) {
        this.optionName = selection.getProductOption().getAttributeName();
        this.value = selection.getAttributeValue();
    }
}
