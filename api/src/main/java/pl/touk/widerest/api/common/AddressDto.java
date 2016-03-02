package pl.touk.widerest.api.common;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import pl.touk.widerest.api.BaseDto;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "Address", description = "Address DTO resource representation")
public class AddressDto extends BaseDto {

    @ApiModelProperty(position = 0, value = "First name of the customer", required = true, dataType = "java.lang.String")
    private String firstName;

    @ApiModelProperty(position = 1, value = "Last name of the customer", required = true, dataType = "java.lang.String")
    private String lastName;

    @ApiModelProperty(position = 2, value = "City", required = true, dataType = "java.lang.String")
    protected String city;

    @ApiModelProperty(position = 3, value = "Postal Code", required = true, dataType = "java.lang.String")
    protected String postalCode;

    @ApiModelProperty(position = 4, value = "First line of customer's address", required = true, dataType = "java.lang.String")
    protected String addressLine1;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @ApiModelProperty(position = 5, value = "Second line of customer's address", dataType = "java.lang.String")
    protected String addressLine2;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @ApiModelProperty(position = 6, value = "Third line of customer's address", dataType = "java.lang.String")
    protected String addressLine3;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @ApiModelProperty(position = 7, value = "Company name", dataType = "java.lang.String")
    protected String companyName;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @ApiModelProperty(position = 8, value = "Name of the address", dataType = "java.lang.String")
    protected String addressName;

    @ApiModelProperty(position = 9, value = "ISO 3166 alpha-2 country code", required = true, dataType = "java.lang.String")
    protected String countryCode;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @ApiModelProperty(position = 10, value = "ISO 3166 country subdivision code", dataType = "java.lang.String")
    protected String countrySubdivisionCode;

}
