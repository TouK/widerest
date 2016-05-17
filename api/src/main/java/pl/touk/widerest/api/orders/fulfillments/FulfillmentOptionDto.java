package pl.touk.widerest.api.orders.fulfillments;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@ApiModel(value = "Fulfillment Option", description = "Fulfillment Option DTO resource representation")
public class FulfillmentOptionDto {

    @ApiModelProperty(value = "Short description of the fulfillment option", required = true)
    private String description;

    @ApiModelProperty(value = "Price of the fulfillment option", required = true)
    private BigDecimal price;

    private BigDecimal priceFrom;

    private BigDecimal priceTo;
}
