package pl.touk.widerest.api.settings;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Builder;
import org.springframework.hateoas.ResourceSupport;

@Data
@Builder
@NoArgsConstructor
//@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "Settings", description = "System settings representation")
public class SettingsDto extends ResourceSupport {
}
