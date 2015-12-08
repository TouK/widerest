package pl.touk.widerest.api.cart.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Builder;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "Payment", description = "Payment request details")
public class PaymentDto {

    public enum Provider {
        PAYPAL
    };

    @ApiModelProperty
    private Provider provider;

    @ApiModelProperty
    protected String successUrl;

    @ApiModelProperty
    protected String cancelUrl;

    @ApiModelProperty
    protected String failureUrl;



}
