package pl.touk.widerest.api.cart.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Builder;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "Address", description = "Address DTO resource representation")
public class AddressDto {

    @ApiModelProperty(position = 0, value = "First name of the customer", required = true)
    private String firstName;

    @ApiModelProperty(position = 1, value = "Last name of the customer", required = true)
    private String lastName;

    @ApiModelProperty(position = 2, value = "City", required = true)
    protected String city;

    @ApiModelProperty(position = 3, value = "Postal Code", required = true)
    protected String postalCode;

    @ApiModelProperty(position = 4, value = "First line of customer's address", required = true)
    protected String addressLine1;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @ApiModelProperty(position = 5, value = "Second line of customer's address")
    protected String addressLine2;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @ApiModelProperty(position = 6, value = "Third line of customer's address")
    protected String addressLine3;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @ApiModelProperty(position = 7, value = "Company name")
    protected String companyName;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @ApiModelProperty(position = 8, value = "Name of the address")
    protected String addressName;

    @ApiModelProperty(position = 9, value = "ISO 3166 alpha-2 country code", required = true)
    protected String countryCode;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @ApiModelProperty(position = 10, value = "ISO 3166 country subdivision code")
    protected String countrySubdivisionCode;

}
