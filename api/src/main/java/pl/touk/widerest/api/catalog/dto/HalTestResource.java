package pl.touk.widerest.api.catalog.dto;

import lombok.Data;
import lombok.experimental.Builder;
import org.springframework.hateoas.ResourceSupport;

@Data
@Builder
public class HalTestResource extends ResourceSupport {

    private String name;
}
