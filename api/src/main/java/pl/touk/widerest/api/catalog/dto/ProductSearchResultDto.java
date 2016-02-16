package pl.touk.widerest.api.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;
import pl.touk.widerest.api.catalog.products.ProductDto;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSearchResultDto {
    private Integer page;
    private Integer pageSize;
    private Integer totalResults;
    private Integer totalPages;
    private List<FacetDto> facets;
    private List<ProductDto> products;
}
