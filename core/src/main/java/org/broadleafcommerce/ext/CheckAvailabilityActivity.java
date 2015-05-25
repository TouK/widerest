package org.broadleafcommerce.ext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadleafcommerce.core.catalog.domain.Sku;
import org.broadleafcommerce.core.catalog.service.CatalogService;
import org.broadleafcommerce.core.inventory.service.ContextualInventoryService;
import org.broadleafcommerce.core.inventory.service.InventoryUnavailableException;
import org.broadleafcommerce.core.order.domain.BundleOrderItem;
import org.broadleafcommerce.core.order.domain.DiscreteOrderItem;
import org.broadleafcommerce.core.order.domain.OrderItem;
import org.broadleafcommerce.core.order.service.OrderItemService;
import org.broadleafcommerce.core.order.service.workflow.CartOperationRequest;
import org.broadleafcommerce.core.workflow.ProcessContext;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

public class CheckAvailabilityActivity extends org.broadleafcommerce.core.order.service.workflow.CheckAvailabilityActivity {

    private static final Log LOG = LogFactory.getLog(CheckAvailabilityActivity.class);

    @Resource(name = "blCatalogService")
    protected CatalogService catalogService;

    @Resource(name = "blOrderItemService")
    protected OrderItemService orderItemService;

    @Resource(name = "blInventoryService")
    protected ContextualInventoryService inventoryService;


    @Override
    public ProcessContext<CartOperationRequest> execute(ProcessContext<CartOperationRequest> context) throws Exception {
        CartOperationRequest request = context.getSeedData();

        Sku sku;
        Long orderItemId = request.getItemRequest().getOrderItemId();
        if (orderItemId != null) {
            // this must be an update request as there is an order item ID available
            OrderItem orderItem = orderItemService.readOrderItemById(orderItemId);
            if (orderItem instanceof DiscreteOrderItem) {
                sku = ((DiscreteOrderItem) orderItem).getSku();
            } else if (orderItem instanceof BundleOrderItem) {
                sku = ((BundleOrderItem) orderItem).getSku();
            } else {
                LOG.warn("Could not check availability; did not recognize passed-in item " + orderItem.getClass().getName());
                return context;
            }
        } else {
            // No order item, this must be a new item add request
            Long skuId = request.getItemRequest().getSkuId();
            sku = catalogService.findSkuById(skuId);
        }

        Integer requestedQuantity = request.getItemRequest().getQuantity();

        Map<String, Object> inventoryContext = new HashMap<String, Object>();
        inventoryContext.put(ContextualInventoryService.ORDER_KEY, context.getSeedData().getOrder());
        boolean available = inventoryService.isAvailable(sku, requestedQuantity, inventoryContext);
        if (!available) {
                throw new InventoryUnavailableException(sku.getId(),
                        requestedQuantity, inventoryService.retrieveQuantityAvailable(sku, inventoryContext));
        }

        return context;
    }

}
