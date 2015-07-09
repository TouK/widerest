package pl.touk.widerest.api.catalog.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.experimental.Builder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mst on 06.07.15.
 */
@ApiModel
@Data
@Builder
public class BundleDto {

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    private List<BundleItemDto> bundleItems = new ArrayList<>();

    private BigDecimal bundlePrice;

    private BigDecimal potentialSavings;
}
