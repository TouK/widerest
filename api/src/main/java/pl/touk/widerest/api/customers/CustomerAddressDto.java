package pl.touk.widerest.api.customers;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import pl.touk.widerest.api.common.AddressDto;

@Data
@Builder
@ApiModel(value = "Customer Address", description = "Customer Address DTO resource description")
public class CustomerAddressDto {

    @ApiModelProperty(position = 0, value = "Name of the address", required = true, dataType = "java.lang.String")
    private String addressName;

    @ApiModelProperty(position = 1, value = "Address", required = true, dataType = "pl.touk.widerest.api.common.AddressDto")
    private AddressDto addressDto;
}
