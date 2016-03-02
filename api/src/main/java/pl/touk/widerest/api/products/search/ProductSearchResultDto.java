package pl.touk.widerest.api.products.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pl.touk.widerest.api.products.ProductDto;

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
