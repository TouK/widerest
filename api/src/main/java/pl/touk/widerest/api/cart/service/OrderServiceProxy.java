package pl.touk.widerest.api.cart.service;

import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.order.domain.OrderImpl;
import org.broadleafcommerce.core.order.service.OrderService;
import org.broadleafcommerce.profile.core.domain.Customer;
import org.broadleafcommerce.profile.core.service.CustomerService;
import org.broadleafcommerce.profile.core.service.CustomerUserDetails;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import pl.touk.widerest.api.cart.dto.DiscreteOrderItemDto;
import pl.touk.widerest.api.cart.exceptions.CustomerNotFoundException;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.Collections;
import java.util.List;

/**
 * Created by mst on 15.07.15.
 */
@Service("wdOrderService")
public class OrderServiceProxy {

    @Resource(name = "blOrderService")
    private OrderService orderService;

    @Resource(name = "blCustomerService")
    private CustomerService customerService;

    @PersistenceContext(unitName = "blPU")
    protected EntityManager em;

    @PostAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    public List<Order> getOrdersByCustomer(CustomerUserDetails customerUserDetails) throws CustomerNotFoundException {

        if(customerUserDetails.getAuthorities().contains(new SimpleGrantedAuthority("'ROLE_ADMIN"))) {
            return getAllOrders();
        } else {
            Customer customer = customerService.readCustomerById(customerUserDetails.getId());

            if(customer == null) {
                throw new CustomerNotFoundException("Cannot find customer with ID: " + customerUserDetails.getId());
            }

            return orderService.findOrdersForCustomer(customer);
        }
    }

    private List<Order> getAllOrders() {
        CriteriaBuilder builder = this.em.getCriteriaBuilder();
        CriteriaQuery criteria = builder.createQuery(Order.class);
        Root order = criteria.from(OrderImpl.class);
        criteria.select(order);
        TypedQuery query = this.em.createQuery(criteria);
        query.setHint("org.hibernate.cacheable", Boolean.valueOf(true));
        query.setHint("org.hibernate.cacheRegion", "query.Order");
        return query.getResultList();
    }

}
