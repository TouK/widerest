package pl.touk.widerest.api.cart.dto;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Builder;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Created by mst on 07.07.15.
 */
@Data
@Builder
@ApiModel
public class PaymentInfoDto {

    private Long orderId;
    @ApiModelProperty
    protected AddressDto billingAddress;

    /* TODO: Currency - Money */
    @ApiModelProperty
    protected BigDecimal amount;

    @ApiModelProperty
    protected String referenceNumber;
    @ApiModelProperty
    protected String type;


}
