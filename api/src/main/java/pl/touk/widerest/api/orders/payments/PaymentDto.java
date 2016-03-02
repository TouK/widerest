package pl.touk.widerest.api.orders.payments;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import pl.touk.widerest.api.BaseDto;

@Data
@NoArgsConstructor
//@AllArgsConstructor
@JsonRootName("payment")
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "Payment", description = "Payment request details")
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property="provider")
public abstract class PaymentDto extends BaseDto {

    @ApiModelProperty
    public void setProvider(String provider) {
        throw new UnsupportedOperationException();
    };

//    @ApiModelProperty
//    private String provider;
//
//    @ApiModelProperty
//    protected String successUrl;
//
//    @ApiModelProperty
//    protected String cancelUrl;
//
//    @ApiModelProperty
//    protected String failureUrl;

}
