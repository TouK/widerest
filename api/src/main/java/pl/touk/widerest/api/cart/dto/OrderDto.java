package pl.touk.widerest.api.cart.dto;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Builder;
import org.broadleafcommerce.profile.core.domain.Address;

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

    private String status;


    private AddressDto shippingAddress;

}
