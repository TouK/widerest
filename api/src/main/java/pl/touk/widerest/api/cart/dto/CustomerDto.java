package pl.touk.widerest.api.cart.dto;


import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;
import java.util.Locale;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Builder;
import org.springframework.hateoas.ResourceSupport;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel
public class CustomerDto extends ResourceSupport {

    @JsonIgnore
    private Long customerId;

    @ApiModelProperty
    private String username;
    @ApiModelProperty
    private String passwordHash;

    @ApiModelProperty
    private String firstName;
    @ApiModelProperty
    private String lastName;

    @ApiModelProperty
    private String email;

    @ApiModelProperty
    private Locale locale;

    @ApiModelProperty
    private Boolean registered = false;

    @ApiModelProperty
    private Boolean deactivated = false;
    
    /* customer attributes */

    @ApiModelProperty
    private AddressDto correspondenceAddress;
    @ApiModelProperty
    private AddressDto residenceAddress;

    private List<CustomerAddressDto> addresses;

}