package pl.touk.widerest.api.catalog.dto;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Builder;

/**
 * Created by mst on 04.08.15.
 */
@ApiModel
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductOptionValueDto {
    private ProductOptionDto productOption;
    private String attributeValue;
}
