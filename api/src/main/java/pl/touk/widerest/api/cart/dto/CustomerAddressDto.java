package pl.touk.widerest.api.cart.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Builder;

/**
 * Created by mst on 13.07.15.
 */
@Builder
@Data
//@ApiModelProperty
public class CustomerAddressDto {

    private Long id;

    private String addressName;

    private AddressDto addressDto;
}
