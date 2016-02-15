package pl.touk.widerest.api.cart.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.Builder;

@Builder
@Data
@ApiModel(value = "Customer Address", description = "Customer Address DTO resource description")
public class CustomerAddressDto {

    @ApiModelProperty(position = 0, value = "Name of the address", required = true, dataType = "java.lang.String")
    private String addressName;

    @ApiModelProperty(position = 1, value = "Address", required = true, dataType = "pl.touk.widerest.api.cart.dto.AddressDto")
    private AddressDto addressDto;
}
