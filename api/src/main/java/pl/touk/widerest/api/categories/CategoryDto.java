package pl.touk.widerest.api.categories;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotBlank;
import pl.touk.widerest.api.BaseDto;
import pl.touk.widerest.api.common.MediaDto;

import java.time.ZonedDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonRootName("category")
@ApiModel(value = "Category", description = "Category DTO resource representation")
public class CategoryDto extends BaseDto {

    @NotBlank(message = "Category has to have a non empty name")
    @ApiModelProperty(position = 0, value = "Name of the category", required = true, dataType = "java.lang.String")
    private String name;

    @ApiModelProperty(position = 1, value = "Short description of the category", dataType = "java.lang.String")
    private String description;

    @ApiModelProperty(position = 2, value = "Long description of the category", dataType = "java.lang.String")
    private String longDescription;

    @ApiModelProperty(position = 3, value = "Availability of all products in this category", dataType = "java.lang.String",
            allowableValues = "ALWAYS_AVAILABLE, UNAVAILABLE, CHECK_QUANTITY")
    private String productsAvailability;

    @ApiModelProperty(position = 4, value = "Attributes associated with the category")
    private Map<String, String> attributes;

    @ApiModelProperty(position = 5, value = "List of medias associated with a category")
    private Map<String, MediaDto> media;

    @ApiModelProperty(position = 21, dataType = "java.lang.String")
    private String url;

    @ApiModelProperty(value = "Date from which the product becomes active/valid", dataType = "java.util.Date")
    private ZonedDateTime validFrom;

    @ApiModelProperty(value = "Date from which the product becomes inactive/invalid", dataType = "java.util.Date")
    private ZonedDateTime validTo;
}
