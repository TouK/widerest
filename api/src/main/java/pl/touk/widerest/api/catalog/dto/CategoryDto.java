package pl.touk.widerest.api.catalog.dto;

import org.springframework.hateoas.ResourceSupport;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Builder;

/**
 * Created by mst on 06.07.15.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "Category", description = "Category resource representation")
public class CategoryDto extends ResourceSupport {


    @JsonIgnore
    private Long categoryId;

    @ApiModelProperty(position = 1, value = "Category name", required = true)
    private String name;

    @ApiModelProperty(position = 2, value = "Short description of the category", required = true)
    private String description;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @ApiModelProperty(position = 3, value = "Long description of the category", required = false)
    private String longDescription;
}
