package pl.touk.widerest.api.catalog.dto;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.experimental.Builder;

/**
 * Created by mst on 06.07.15.
 */
@Data
@Builder
@ApiModel
public class ReviewDto {
    private String reviewText;

    private int helpfulCount;

    private int notHelpfulCount;

    private String statusType;
}
