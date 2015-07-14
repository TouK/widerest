package pl.touk.widerest.api.cart.dto;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Builder;


import java.math.BigDecimal;
import java.util.List;

/**
 * Created by mst on 07.07.15.
 */

@Data
@Builder
@ApiModel
public class OrderDto {
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
    private List<OrderItemDto> orderItems;
    @ApiModelProperty
    private BigDecimal totalPrice;
    @ApiModelProperty
    private List<CartAttributeDto> cartAttributeDtos;
    @ApiModelProperty
    private List<OrderPaymentDto> orderPaymentDto;
}
