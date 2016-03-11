package pl.touk.widerest.api.orders;


import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import pl.touk.widerest.api.BaseDto;
import pl.touk.widerest.api.common.AddressDto;
import pl.touk.widerest.api.orders.payments.OrderPaymentDto;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonRootName("order")
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "Order", description = "Order DTO resource representation")
public class OrderDto extends BaseDto {

//    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @ApiModelProperty(position = 0, value = "Order number", required = true, dataType = "java.lang.String")
    private String orderNumber;

    @ApiModelProperty(position = 1, value = "Status of the order", readOnly = true)
    private String status;

    @ApiModelProperty(position = 4, value = "Items belonging to the order", dataType = "java.util.List")
    private List<DiscreteOrderItemDto> orderItems;

    @ApiModelProperty(position = 5, value = "Total price for the order", dataType = "java.math.BigDecimal")
    private BigDecimal totalPrice;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    @ApiModelProperty(position = 6, value = "Additional attributes for the order", dataType = "java.util.List")
    private List<CartAttributeDto> attributes;

    /* TODO: (mst) what is this!? */
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    @ApiModelProperty(position = 7, value = "", readOnly = true)
    private List<OrderPaymentDto> orderPayment;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @ApiModelProperty(position = 8, value = "Name of the fulfillment", dataType = "java.lang.String")
    private String fulfillment;
}
