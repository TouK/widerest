package pl.touk.widerest.api.cart.service;

import org.broadleafcommerce.core.order.domain.DiscreteOrderItem;
import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.order.domain.OrderImpl;
import org.broadleafcommerce.core.order.service.FulfillmentGroupService;
import org.broadleafcommerce.core.order.service.OrderService;
import org.broadleafcommerce.core.order.service.call.OrderItemRequestDTO;
import org.broadleafcommerce.core.order.service.exception.RemoveFromCartException;
import org.broadleafcommerce.core.order.service.exception.UpdateCartException;
import org.broadleafcommerce.openadmin.server.security.service.AdminUserDetails;
import org.broadleafcommerce.profile.core.domain.Customer;
import org.broadleafcommerce.profile.core.service.CustomerService;
import org.broadleafcommerce.profile.core.service.CustomerUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.touk.widerest.api.DtoConverters;
import pl.touk.widerest.api.cart.dto.AddressDto;
import pl.touk.widerest.api.cart.dto.DiscreteOrderItemDto;
import pl.touk.widerest.api.cart.exceptions.CustomerNotFoundException;
import pl.touk.widerest.api.cart.exceptions.OrderNotFoundException;
import pl.touk.widerest.api.catalog.exceptions.ResourceNotFoundException;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.Collections;
import java.util.List;
import java.util.Optional;


@Service("wdOrderService")
public class OrderServiceProxy {

    @Resource(name = "blOrderService")
    private OrderService orderService;

    @Resource(name = "blCustomerService")
    private CustomerService customerService;

    @Resource(name = "wdfulfilmentService")
    FulfilmentServiceProxy fulfillmentServiceProxy;

    @PersistenceContext(unitName = "blPU")
    protected EntityManager em;

    @PostAuthorize("hasAnyRole('PERMISSION_ALL_ADMIN_ROLES', 'ROLE_USER')")
    @Transactional
    public List<Order> getOrdersByCustomer(CustomerUserDetails customerUserDetails) throws CustomerNotFoundException {

        Customer customer = customerService.readCustomerById(customerUserDetails.getId());
        if(customer == null) {
            throw new CustomerNotFoundException("Cannot find customer with ID: " + customerUserDetails.getId());
        }

        return orderService.findOrdersForCustomer(customer);
    }

    @PostAuthorize("hasRole('PERMISSION_ALL_ORDER')")
    @Transactional
    public List<Order> getAllOrders() {
        CriteriaBuilder builder = this.em.getCriteriaBuilder();
        CriteriaQuery criteria = builder.createQuery(Order.class);
        Root order = criteria.from(OrderImpl.class);
        criteria.select(order);
        TypedQuery query = this.em.createQuery(criteria);
        query.setHint("org.hibernate.cacheable", Boolean.valueOf(true));
        query.setHint("org.hibernate.cacheRegion", "query.Order");
        return query.getResultList();
    }

    @Transactional
    public Order getProperCart(UserDetails userDetails, Long orderId) {
        Order cart = null;

        if (userDetails instanceof CustomerUserDetails) {
            cart = getOrderForCustomerById((CustomerUserDetails) userDetails, orderId);
        } else if (userDetails instanceof AdminUserDetails) {
            cart = orderService.findOrderById(orderId);
        }

        return cart;
    }

    @Transactional
    public List<DiscreteOrderItem> getDiscreteOrderItemsFromProperCart(UserDetails userDetails, Long orderId) {
        return Optional.ofNullable(getProperCart(userDetails, orderId))
                .orElseThrow(ResourceNotFoundException::new)
                .getDiscreteOrderItems();
    }

    public Order getOrderForCustomerById(CustomerUserDetails customerUserDetails, Long orderId) throws OrderNotFoundException {

        return Optional.ofNullable(getOrdersByCustomer(customerUserDetails))
                .orElseThrow(() -> new OrderNotFoundException("Cannot find order with ID: " + orderId + " for customer with ID: " + customerUserDetails.getId()))
                .stream()
                .filter(x -> x.getId().equals(orderId))
                .findAny()
                .orElseThrow(() -> new OrderNotFoundException("Cannot find order with ID: " + orderId + " for customer with ID: " + customerUserDetails.getId()));
    }

    @Transactional
    public AddressDto getOrderFulfilmentAddress(
           UserDetails userDetails, Long orderId) {

        Order order = Optional.ofNullable(getProperCart(userDetails, orderId))
                .orElseThrow(ResourceNotFoundException::new);

        return Optional.ofNullable(fulfillmentServiceProxy.getFulfillmentAddress(order))
                .map(DtoConverters.addressEntityToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Address for fulfillment for order with ID: " + orderId + " does not exist"));
    }

    @Transactional
    public ResponseEntity<?> updateItemQuantityInOrder (
            Integer quantity, UserDetails userDetails, Long orderId, Long itemId)
                throws UpdateCartException, RemoveFromCartException {

        if (quantity <= 0) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }

        Order cart = Optional.ofNullable(getProperCart(userDetails, orderId))
                .orElseThrow(ResourceNotFoundException::new);

        if (cart.getDiscreteOrderItems().stream().filter(x -> x.getId() == itemId).count() != 1) {
            throw new ResourceNotFoundException("Cannot find an item with ID: " + itemId);
        }

        OrderItemRequestDTO orderItemRequestDto = new OrderItemRequestDTO();
        orderItemRequestDto.setQuantity(quantity);
        orderItemRequestDto.setOrderItemId(itemId);


        orderService.updateItemQuantity(orderId, orderItemRequestDto, true);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
