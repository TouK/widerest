package pl.touk.widerest.api.products.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacetDto {
    private Boolean active;
    private String label;
    private List<FacetValueDto> facetOptions;
}
