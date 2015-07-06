package pl.touk.widerest.api.catalog.api.catalog.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Builder;
import org.broadleafcommerce.core.catalog.domain.Category;
import org.springframework.hateoas.ResourceSupport;

/**
 * Created by mst on 06.07.15.
 */

@Data
@Builder
@ApiModel
public class CategoryDto extends ResourceSupport {

    @ApiModelProperty(required = true)
    private Long categoryId;

    @ApiModelProperty(required = true)
    private String name;

    @ApiModelProperty(required = true)
    private String description;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    private String longDescription;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    private Category parentCategory;
}
