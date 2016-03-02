package pl.touk.widerest.api.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import pl.touk.widerest.api.BaseDto;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonRootName("media")
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "Media", description = "Media DTO resource representation")
public class MediaDto extends BaseDto {

    @ApiModelProperty(position = 0, value = "Title of the media", required = true, dataType = "java.lang.String")
    private String title;

    @ApiModelProperty(position = 1, value = "URL to the resource associated with this media", required = true, dataType = "java.lang.String")
    private String url;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @ApiModelProperty(position = 2, value = "Attribute (alt) for HTML property of IMG", dataType = "java.lang.String")
    private String altText;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @ApiModelProperty(position = 3, value = "Tags describing the media", dataType = "java.lang.String")
    private String tags;
}
