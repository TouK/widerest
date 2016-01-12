package pl.touk.widerest.api.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Builder;

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
