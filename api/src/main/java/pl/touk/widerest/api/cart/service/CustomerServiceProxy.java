package pl.touk.widerest.api.cart.service;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.broadleafcommerce.profile.core.domain.Customer;
import org.broadleafcommerce.profile.core.domain.CustomerImpl;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Service;

@Service("wdCustomerService")
public class CustomerServiceProxy {

    @PersistenceContext(unitName = "blPU")
    protected EntityManager em;

    @PostAuthorize("hasRole('PERMISSION_ALL_CUSTOMER')")
    public List<Customer> getAllCustomers() {
        final CriteriaBuilder builder = this.em.getCriteriaBuilder();
        CriteriaQuery criteria = builder.createQuery(Customer.class);
        final Root customer = criteria.from(CustomerImpl.class);
        criteria = criteria.select(customer);
        final TypedQuery query = this.em.createQuery(criteria);
        query.setHint("org.hibernate.cacheable", Boolean.valueOf(true));
        query.setHint("org.hibernate.cacheRegion", "query.Order");
        return query.getResultList();
    }

    public Customer getCustomerById(Long id) {
        return this.em.find(CustomerImpl.class, id);
    }

}
