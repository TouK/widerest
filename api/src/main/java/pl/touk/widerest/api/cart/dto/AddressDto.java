package pl.touk.widerest.api.cart.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Builder;

/**
 * Created by mst on 08.07.15.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("Address")
public class AddressDto {

    @ApiModelProperty(required = true)
    private String firstName;

    @ApiModelProperty(required = true)
    private String lastName;

    @ApiModelProperty(required = true)
    protected String city;

    /* TODO: (mst) Validation? */
    @ApiModelProperty
    protected String postalCode;

    @ApiModelProperty(required = true)
    protected String addressLine1;

    @ApiModelProperty
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    protected String addressLine2;

    @ApiModelProperty
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    protected String addressLine3;

    @ApiModelProperty
    protected String companyName;

    @ApiModelProperty
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    protected String addressName;


    @ApiModelProperty
    protected String county;



}
