package pl.touk.widerest.api.orders.fulfillments;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.hateoas.ResourceSupport;
import pl.touk.widerest.api.common.AddressDto;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "Fulfillment", description = "Fulfillment DTO resource representation")
public class FulfillmentDto extends ResourceSupport {

    @ApiModelProperty(position = 0, value = "Price of the whole fulfillment", required = true,
            dataType = "java.math.BigDecimal")
    private BigDecimal price;

    @ApiModelProperty(position = 1, value = "Address of the fulfillment", required = true,
            dataType = "pl.touk.widerest.api.common.AddressDto")
    private AddressDto address;

    @ApiModelProperty(position = 2, value = "ID of a selected fulfillment option", required = true,
            dataType = "java.lang.Long")
    private Long selectedOptionId;

    @ApiModelProperty(position = 3, value = "Available options for fulfillment", required = true,
            dataType = "pl.touk.widerest.api.orders.fulfillments.FulfillmentOptionDto")
    private List<FulfillmentOptionDto> options;
}
