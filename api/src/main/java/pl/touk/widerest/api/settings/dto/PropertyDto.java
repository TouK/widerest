package pl.touk.widerest.api.settings.dto;

import com.fasterxml.jackson.annotation.JsonRootName;
import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import pl.touk.widerest.api.BaseDto;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonRootName("property")
@ApiModel(value = "Property", description = "System property DTO representation")
public class PropertyDto extends BaseDto {
    private String name;
    private Object value;
}
