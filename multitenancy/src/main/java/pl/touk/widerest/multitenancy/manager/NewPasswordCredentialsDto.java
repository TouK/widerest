package pl.touk.widerest.multitenancy.manager;

import com.fasterxml.jackson.annotation.JsonRootName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Builder;

/**
 * Created by mst on 15.10.15.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "New Password Credentials", description = "New Password Credentials DTO resource representation")
public class NewPasswordCredentialsDto {

    @ApiModelProperty(position = 0, value = "Current password", required = true, dataType = "java.lang.String")
    private String currentPassword;

    @ApiModelProperty(position = 1, value = "New password", required = true, dataType = "java.lang.String")
    private String newPassword;
}
