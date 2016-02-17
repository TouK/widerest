package pl.touk.widerest.api.cart.orders.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "Payment", description = "Payment request details")
public class PaymentDto {

    @ApiModelProperty
    private String provider;

    @ApiModelProperty
    protected String successUrl;

    @ApiModelProperty
    protected String cancelUrl;

    @ApiModelProperty
    protected String failureUrl;



}
