package pl.touk.widerest.api.cart.dto;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;
import java.util.Locale;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Builder;

/**
 * Created by mst on 07.07.15.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel
public class CustomerDto {
    private Long id;

    @ApiModelProperty
    private String username;
    @ApiModelProperty
    private String firstName;
    @ApiModelProperty
    private String lastName;


    @ApiModelProperty
    private Locale locale;


    @ApiModelProperty
    private Boolean registered = false;
    @ApiModelProperty
    private Boolean deactivaed = false;
    
    /* customer attributes */

    @ApiModelProperty
    private AddressDto correspondenceAddress;
    @ApiModelProperty
    private AddressDto residenceAddress;

    private List<CustomerAddressDto> addresses;


}