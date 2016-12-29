package pl.touk.widerest.api.customers;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
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

import javax.validation.Valid;
import java.util.Map;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonRootName("customer")
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
    private String locale;

    @ApiModelProperty(position = 5, value = "Collection of available customer addresses")
    @Valid
    private Map<String, AddressDto> addresses;
}
