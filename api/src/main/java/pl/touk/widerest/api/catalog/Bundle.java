package pl.touk.widerest.api.catalog;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.wordnik.swagger.annotations.ApiModel;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@ApiModel
@Data
public class Bundle extends Product {

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    private List<BundleItem> bundleItems = new ArrayList<>();

    private BigDecimal bundlePrice;

    private BigDecimal potentialSavings;

}