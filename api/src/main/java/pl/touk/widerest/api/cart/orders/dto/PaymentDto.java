package pl.touk.widerest.api.cart.orders.dto;

import com.fasterxml.jackson.annotation.JsonRootName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;
import pl.touk.widerest.api.BaseDto;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonRootName("payment")
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "Payment", description = "Payment request details")
public class PaymentDto extends BaseDto {

    @ApiModelProperty
    private String provider;

    @ApiModelProperty
    protected String successUrl;

    @ApiModelProperty
    protected String cancelUrl;

    @ApiModelProperty
    protected String failureUrl;



}
