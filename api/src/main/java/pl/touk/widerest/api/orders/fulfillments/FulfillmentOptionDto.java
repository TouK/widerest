package pl.touk.widerest.api.orders.fulfillments;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ApiModel(value = "Fulfillment Option", description = "Fulfillment Option DTO resource representation")
public class FulfillmentOptionDto {

    @ApiModelProperty(position = 2, value = "Short description of the fulfillment option", required = true)
    private String description;

    @ApiModelProperty(position = 3, value = "Price of the fulfillment option", required = true)
    private BigDecimal price;
}
