package pl.touk.widerest.api.cart.dto;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Builder;
import org.springframework.hateoas.ResourceSupport;

import java.math.BigDecimal;
import java.util.List;

/**
 * Created by mst on 07.07.15.
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "Order", description = "Order DTO resource representation")
public class OrderDto extends ResourceSupport {

    @JsonIgnore
    private Long orderId;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @ApiModelProperty(position = 0, value = "Order number", required = true, dataType = "java.lang.String")
    private String orderNumber;

    @ApiModelProperty(position = 1, value = "Status of the order", required = true, dataType = "java.lang.String")
    private String status;

    @ApiModelProperty(position = 2, value = "Shipping address for the order", required = true, dataType = "pl.touk.widerest.api.cart.dto.AddressDto")
    private AddressDto shippingAddress;

    @ApiModelProperty(position = 3, value = "Customer information", required = true, dataType = "pl.touk.widerest.api.cart.dto.CustomerDto")
    private CustomerDto customer;

    @ApiModelProperty(position = 4, value = "Items belonging to the order", dataType = "java.util.List")
    private List<DiscreteOrderItemDto> orderItems;

    @ApiModelProperty(position = 5, value = "Total price for the order", dataType = "java.math.BigDecimal")
    private BigDecimal totalPrice;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @ApiModelProperty(position = 6, value = "Additional attributes for the order", dataType = "java.util.List")
    private List<CartAttributeDto> cartAttributes;

    /* TODO: (mst) what is this!? */
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @ApiModelProperty(position = 7, value = "", dataType = "java.util.List")
    private List<OrderPaymentDto> orderPaymentDto;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @ApiModelProperty(position = 8, value = "Name of the fulfillment", dataType = "java.lang.String")
    private String fulfillment;
}
