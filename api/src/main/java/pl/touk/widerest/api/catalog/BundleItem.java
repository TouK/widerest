package pl.touk.widerest.api.catalog;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.experimental.Builder;

@ApiModel
@Data
@Builder
public class BundleItem {

    private long quantity;

    private long productId;

}
