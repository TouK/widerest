package pl.touk.widerest.api.cart.dto;


import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Builder;
import org.broadleafcommerce.common.money.Money;


/**
 * Created by mst on 07.07.15.
 */
@Data
@Builder
@ApiModel
public class OrderPaymentDto {

    @JsonIgnore
    private Long orderId;

    private Long paymentId;

    @ApiModelProperty
    protected AddressDto billingAddress;

    @ApiModelProperty
    protected Money amount;

    @ApiModelProperty
    protected String referenceNumber;
    @ApiModelProperty
    protected String type;


}
