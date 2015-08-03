package pl.touk.widerest.api.cart.dto;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Builder;

import java.math.BigDecimal;

/**
 * Created by mst on 31.07.15.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel
public class FulfillmentOptionDto {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
}
