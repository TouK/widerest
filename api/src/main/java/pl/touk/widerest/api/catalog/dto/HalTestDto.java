package pl.touk.widerest.api.catalog.dto;

import lombok.Data;
import lombok.experimental.Builder;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;

@Data
@Builder
public class HalTestDto extends ResourceSupport {

    private String name;

}
