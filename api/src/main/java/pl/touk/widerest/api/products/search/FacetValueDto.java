package pl.touk.widerest.api.products.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacetValueDto {
    private String value;
    private BigDecimal minValue;
    private BigDecimal maxValue;
    private Integer quantity;
}
