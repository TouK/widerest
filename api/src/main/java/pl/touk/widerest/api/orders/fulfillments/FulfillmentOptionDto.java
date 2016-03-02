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
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "Fulfillment Option", description = "Fulfillment Option DTO resource representation")
public class FulfillmentOptionDto {

    @ApiModelProperty(position = 0, value = "ID of the fulfillment option", required = true, dataType = "java.lang.Long")
    private Long id;

    @ApiModelProperty(position = 1, value = "Name of the fulfillment option", required = true, dataType = "java.lang.String")
    private String name;

    @ApiModelProperty(position = 2, value = "Short description of the fulfillment option", required = true, dataType = "java.lang.String")
    private String description;

    @ApiModelProperty(position = 3, value = "Price of the fulfillment option", required = true, dataType = "java.math.BigDecimal")
    private BigDecimal price;
}
