package pl.touk.widerest.api.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Builder;

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
