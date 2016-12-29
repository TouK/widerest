package pl.touk.widerest.api.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotBlank;
import pl.touk.widerest.api.BaseDto;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonRootName("media")
@ApiModel(value = "Media", description = "Media DTO resource representation")
public class MediaDto extends BaseDto {

    @ApiModelProperty(position = 0, value = "Title of the media", required = true, dataType = "java.lang.String")
    private String title;

    @NotBlank(message = "Media has to have an URL provided")
    @ApiModelProperty(position = 1, value = "URL to the resource associated with this media", required = true, dataType = "java.lang.String")
    private String url;

    @ApiModelProperty(position = 2, value = "Attribute (alt) for HTML property of IMG", dataType = "java.lang.String")
    private String altText;

    @ApiModelProperty(position = 3, value = "Tags describing the media", dataType = "java.lang.String")
    private String tags;
}
