package pl.touk.widerest.api.catalog;

import com.wordnik.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.experimental.Builder;

@ApiModel
@Data
@Builder
public class ProductOptionSelection {

    private String optionName;

    private String value;

}
