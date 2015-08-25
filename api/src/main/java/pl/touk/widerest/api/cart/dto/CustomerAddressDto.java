package pl.touk.widerest.api.cart.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Builder;

/**
 * Created by mst on 13.07.15.
 */
@Builder
@Data
@ApiModel("CustomerAddress")
public class CustomerAddressDto {
    private String addressName;
    private AddressDto addressDto;
}
