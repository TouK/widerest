package pl.touk.widerest.api.cart.dto;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Builder;

import java.math.BigDecimal;
import java.util.List;

/**
 * Created by mst on 31.07.15.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel
public class FulfillmentDto {
    private List<FulfillmentOptionDto> options;
    private Long selectedOptionId;
    private BigDecimal price;

    private AddressDto address;

}
