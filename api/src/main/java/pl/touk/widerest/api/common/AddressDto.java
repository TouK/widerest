package pl.touk.widerest.api.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Email;
import pl.touk.widerest.api.BaseDto;
import pl.touk.widerest.constraints.IsoCountryCode;

import javax.annotation.Nullable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@ApiModel(value = "Address", description = "Address DTO resource representation")
public class AddressDto extends BaseDto {

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

    @ApiModelProperty(position = 5, value = "Second line of customer's address")
    protected String addressLine2;

    @ApiModelProperty(position = 6, value = "Third line of customer's address")
    protected String addressLine3;

    @ApiModelProperty(position = 7, value = "Company name")
    protected String companyName;

    @ApiModelProperty(position = 9, value = "ISO 3166 alpha-2 country code", required = true)
    @IsoCountryCode
    protected String countryCode;

    @ApiModelProperty(position = 10, value = "ISO 3166 country subdivision code")
    protected String countrySubdivisionCode;

    @ApiModelProperty
    @Email
    protected String email;



}
