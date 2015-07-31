package pl.touk.widerest.api.cart.dto;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.experimental.Builder;

import java.math.BigDecimal;

/**
 * Created by mst on 31.07.15.
 */
@Data
@Builder
@ApiModel
public class FulfillmentOptionDto {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
}
