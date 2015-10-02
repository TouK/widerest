package pl.touk.widerest.multitenancy;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Builder;

import javax.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel
public class TenantRequest {

    @NotNull
    private String adminPassword;

    @NotNull
    private String adminEmail;

}
