package org.broadleafcommerce.ext;

import org.broadleafcommerce.common.extension.ExtensionResultHolder;
import org.broadleafcommerce.common.extension.ExtensionResultStatusType;
import org.broadleafcommerce.core.catalog.domain.Sku;
import org.broadleafcommerce.core.inventory.service.AbstractInventoryServiceExtensionHandler;
import org.broadleafcommerce.core.inventory.service.InventoryServiceExtensionManager;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collection;
import java.util.Map;

public class AuctionInventoryExtensionHandler extends AbstractInventoryServiceExtensionHandler {

    @Resource(name = "blInventoryServiceExtensionManager")
    InventoryServiceExtensionManager inventoryServiceExtensionManager;

    @PostConstruct
    public void init() {
        inventoryServiceExtensionManager.registerHandler(this);
    }

    @Override
    public ExtensionResultStatusType retrieveQuantitiesAvailable(Collection<Sku> skus, Map<String, Object> context, ExtensionResultHolder<Map<Sku, Integer>> resultHolder) {
//        for (Sku sku : skus) {
//            resultHolder.getResult().put(sku, 0);
//        }
        return ExtensionResultStatusType.NOT_HANDLED;
    }
}
