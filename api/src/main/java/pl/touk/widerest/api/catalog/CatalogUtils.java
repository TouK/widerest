package pl.touk.widerest.api.catalog;

import org.broadleafcommerce.common.persistence.Status;
import org.broadleafcommerce.core.catalog.domain.Category;
import org.broadleafcommerce.core.catalog.domain.Product;

/**
 * Created by mst on 27.07.15.
 */
public class CatalogUtils {
    public static boolean archivedProductFilter(Product product) {
        return ((Status) product).getArchived() == 'N';
    }

    public static boolean archivedCategoryFilter(Category category) {
        return ((Status) category).getArchived() == 'N';
    }
}
