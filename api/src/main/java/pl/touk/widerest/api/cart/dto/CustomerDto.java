package pl.touk.widerest.api.cart.dto;


import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Builder;
import org.broadleafcommerce.profile.core.domain.Address;

import java.util.List;
import java.util.Locale;

/**
 * Created by mst on 07.07.15.
 */
@Data
@Builder
@ApiModel
public class CustomerDto {
    private Long id;

    @ApiModelProperty
    private String username;
    @ApiModelProperty
    private String firstName;
    @ApiModelProperty
    private String lastName;

    /* ChallengeQuestion type ? */
    private String challengeQuestion;

    /* ignotr ? */
    private String challengeAnswer;

    private Boolean passwordChangeRequired = false;

    @ApiModelProperty
    private Locale locale;


    @ApiModelProperty
    private Boolean registered = false;
    @ApiModelProperty
    private Boolean deactivaed = false;
    @ApiModelProperty
    private Boolean receiveEmail = false;

    /* customer attributes */

    @ApiModelProperty
    private AddressDto correspondenceAddress;
    @ApiModelProperty
    private AddressDto residenceAddress;

    private List<CustomerAddressDto> addresses;


}
