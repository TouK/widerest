package pl.touk.widerest.api.customers;


import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Email;
import pl.touk.widerest.api.BaseDto;
import pl.touk.widerest.api.common.AddressDto;

import java.util.Locale;
import java.util.Map;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonRootName("customer")
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "Customer", description = "Customer DTO resource description")
public class CustomerDto extends BaseDto {

    @ApiModelProperty(position = 0, value = "Customer's username")
    private String username;

    @ApiModelProperty(position = 1, value = "Customer's first name")
    private String firstName;

    @ApiModelProperty(position = 2, value = "Customer's last name")
    private String lastName;

    @ApiModelProperty(position = 3, value = "Customer's e-mail address")
    @Email
    private String email;

    @ApiModelProperty(position = 4, value = "Customer's locale")
    private Locale locale;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    @ApiModelProperty(position = 5, value = "Collection of available customer addresses")
    private Map<String, AddressDto> addresses;
}
