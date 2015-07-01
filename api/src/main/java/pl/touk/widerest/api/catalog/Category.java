package pl.touk.widerest.api.catalog;

import lombok.Data;
import lombok.experimental.Builder;
import org.springframework.hateoas.ResourceSupport;

@Data
@Builder
public class Category extends ResourceSupport {

    private String name;

}
