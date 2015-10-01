package pl.touk.widerest.api.cart.dto;


import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Builder;
import org.springframework.hateoas.ResourceSupport;

import java.util.List;
import java.util.Locale;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "Customer", description = "Customer DTO resource description")
public class CustomerDto extends ResourceSupport {

    @JsonIgnore
    private Long customerId;

    @ApiModelProperty(position = 0, value = "Customer's username", required = true, dataType = "java.lang.String")
    private String username;

    @JsonIgnore
    @ApiModelProperty(position = 1, value = "Customer's password hash", dataType = "java.lang.String")
    private String passwordHash;

    @ApiModelProperty(position = 2, value = "Customer's first name", required = true, dataType = "java.lang.String")
    private String firstName;

    @ApiModelProperty(position = 3, value = "Customer's last name", required = true, dataType = "java.lang.String")
    private String lastName;

    /* TODO: (mst) Validation? */
    @ApiModelProperty(position = 4, value = "Customer's e-mail address", required = true, dataType = "java.lang.String")
    private String email;

    @ApiModelProperty(position = 5, value = "Customer's locale", dataType = "java.util.Locale")
    private Locale locale;

    @JsonIgnore
    @ApiModelProperty
    private Boolean registered = false;

    @JsonIgnore
    @ApiModelProperty
    private Boolean deactivated = false;
    
    /* customer attributes */

    @ApiModelProperty(position = 6, value = "Customer's correspondence address", dataType = "pl.touk.widerest.api.cart.dto.AddressDto")
    private AddressDto correspondenceAddress;

    @ApiModelProperty(position = 7, value = "Customer's residence address", dataType = "pl.touk.widerest.api.cart.dto.AddressDto")
    private AddressDto residenceAddress;

    private List<CustomerAddressDto> addresses;

}