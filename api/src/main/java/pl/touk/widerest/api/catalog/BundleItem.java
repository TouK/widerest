package pl.touk.widerest.api.catalog;

import com.wordnik.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.experimental.Builder;

@ApiModel
@Data
@Builder
public class BundleItem {

    private long quantity;

    private long productId;

}
