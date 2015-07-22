package pl.touk.widerest.api.catalog.dto;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Builder;

/**
 * Created by mst on 06.07.15.
 */
@ApiModel
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BundleItemDto {
    private Integer quantity;
    private Long productId;
}
