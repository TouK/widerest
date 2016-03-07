package pl.touk.widerest.api.customers;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import pl.touk.widerest.api.BaseDto;
import pl.touk.widerest.api.common.AddressDto;

import java.util.List;
import java.util.Locale;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonRootName("customer")
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "Customer", description = "Customer DTO resource description")
public class CustomerDto extends BaseDto {

    @ApiModelProperty(position = 0, value = "Customer's username", required = true, dataType = "java.lang.String")
    private String username;

    @ApiModelProperty(position = 1, value = "Customer's first name", required = true, dataType = "java.lang.String")
    private String firstName;

    @ApiModelProperty(position = 2, value = "Customer's last name", required = true, dataType = "java.lang.String")
    private String lastName;

    @ApiModelProperty(position = 3, value = "Customer's e-mail address", required = true, dataType = "java.lang.String")
    private String email;

    @ApiModelProperty(position = 4, value = "Customer's locale", dataType = "java.util.Locale")
    private Locale locale;

    @JsonIgnore
    @ApiModelProperty
    private Boolean registered = false;

    @JsonIgnore
    @ApiModelProperty
    private Boolean deactivated = false;

    @ApiModelProperty(position = 5, value = "Customer's correspondence address", dataType = "pl.touk.widerest.api.common.AddressDto")
    private AddressDto correspondenceAddress;

    @ApiModelProperty(position = 6, value = "Customer's residence address", dataType = "pl.touk.widerest.api.common.AddressDto")
    private AddressDto residenceAddress;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    @ApiModelProperty(position = 7, value = "List of available customer addresses", dataType = "java.util.List")
    private List<CustomerAddressDto> addresses;
}
