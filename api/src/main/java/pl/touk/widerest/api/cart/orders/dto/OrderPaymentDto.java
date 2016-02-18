package pl.touk.widerest.api.cart.orders.dto;


import com.fasterxml.jackson.annotation.JsonRootName;
import lombok.*;
import org.broadleafcommerce.common.money.Money;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import pl.touk.widerest.api.BaseDto;
import pl.touk.widerest.api.cart.customers.dto.AddressDto;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonRootName("orderPayment")
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "Order Payment", description = "Order Payment DTO resource description")
public class OrderPaymentDto extends BaseDto {

    @JsonIgnore
    private Long orderId;

    @ApiModelProperty(position = 0, value = "ID of this payment", required = true, dataType = "java.lang.Long")
    private Long paymentId;

    @ApiModelProperty(position = 1, value = "Customer's billing address", required = true, dataType = "pl.touk.widerest.api.cart.customers.dto.AddressDto")
    protected AddressDto billingAddress;

    @ApiModelProperty(position = 2, value = "Total price for this order", required = true, dataType = "org.broadleafcommerce.common.money.Money")
    protected Money amount;

    @ApiModelProperty(position = 3, value = "Reference number of this number", required = true, dataType = "java.lang.String")
    protected String referenceNumber;

    @ApiModelProperty(position = 4, value = "Type of payment", dataType = "java.lang.String")
    protected String type;
}
