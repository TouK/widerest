package pl.touk.widerest.api.cart.dto;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
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
@ApiModel(value = "Order", description = "Order resource representation")
public class OrderDto extends ResourceSupport {
    @ApiModelProperty(required = true)
    private Long orderId;
    @ApiModelProperty(required = true)
    private String orderNumber;
    @ApiModelProperty(required = true)
    private String paymentUrl;
    /* ???? */
    @ApiModelProperty(required = true)
    private String status;

    @ApiModelProperty(required = true)
    private AddressDto shippingAddress;

    @ApiModelProperty(required = true)
    private CustomerDto customer;

    @ApiModelProperty
    private List<DiscreteOrderItemDto> orderItems;
    @ApiModelProperty
    private BigDecimal totalPrice;
    @ApiModelProperty
    private List<CartAttributeDto> cartAttributeDtos;
    @ApiModelProperty
    private List<OrderPaymentDto> orderPaymentDto;

    @ApiModelProperty
    private String fulfillment;
}
