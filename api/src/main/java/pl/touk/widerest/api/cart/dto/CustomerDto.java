package pl.touk.widerest.api.cart.dto;

import com.wordnik.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.experimental.Builder;

/**
 * Created by mst on 07.07.15.
 */
@Data
@Builder
@ApiModel
public class CustomerDto {
    private Long id;

    private String username;

    private String password;

    private String firstName;

    private String lastName;

    /* ChallengeQuestion type ? */
    private String challengeQuestion;

    /* ignotr ? */
    private String challengeAnswer;

    private Boolean passwordChangeRequired = false;

    // Locale ?

    private Boolean registered = false;

    private Boolean deactivaed = false;

    private Boolean receiveEmail = false;

    /* customer attributes */


    private AddressDto correspondenceAddress;

    private AddressDto residenceAddress;

}
