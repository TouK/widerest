package pl.touk.widerest.ext;

import org.broadleafcommerce.common.extension.ExtensionResultHolder;
import org.broadleafcommerce.common.extension.ExtensionResultStatusType;
import org.broadleafcommerce.core.catalog.domain.Sku;
import org.broadleafcommerce.core.inventory.service.type.InventoryType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class InventoryServiceImpl extends org.broadleafcommerce.core.inventory.service.InventoryServiceImpl {
    @Override
    public boolean isAvailable(Sku sku, int quantity, Map<String, Object> context) {
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity " + quantity + " is not valid. Must be greater than zero.");
        }
        if (!checkBasicAvailablility(sku)) {
            return false;
        }
        if (sku.getInventoryType() == null) {
            return true;
        }

        Integer quantityAvailable = retrieveQuantityAvailable(sku, context);

        return quantityAvailable == null || quantity <= quantityAvailable;
        //return quantityAvailable != null && quantity <= quantityAvailable;
    }

    @Override
    public Map<Sku, Integer> retrieveQuantitiesAvailable(Collection<Sku> skus, Map<String, Object> context) {
        ExtensionResultHolder<Map<Sku, Integer>> holder = new ExtensionResultHolder<Map<Sku, Integer>>();
        ExtensionResultStatusType res = extensionManager.getProxy().retrieveQuantitiesAvailable(skus, context, holder);
        if (ExtensionResultStatusType.NOT_HANDLED.equals(res)) {
            Map<Sku, Integer> inventories = new HashMap<Sku, Integer>();
            for (Sku sku : skus) {
                if (checkBasicAvailablility(sku)) {
                    if (InventoryType.CHECK_QUANTITY.equals(sku.getInventoryType())) {
                        if (sku.getQuantityAvailable() == null) {
                            inventories.put(sku, 0);
                        }
                        inventories.put(sku, sku.getQuantityAvailable());
                    } else if (sku.getInventoryType() == null || InventoryType.ALWAYS_AVAILABLE.equals(sku.getInventoryType())) {
                        inventories.put(sku, null);
                    } else {
                        inventories.put(sku, 0);
                    }
                } else {
                    inventories.put(sku, 0);
                }
            }

            return inventories;
        } else {
            return holder.getResult();
        }
    }

}
