package pl.touk.widerest.api.cart.controllers;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.broadleafcommerce.core.order.service.OrderService;
import org.broadleafcommerce.profile.core.domain.Customer;
import org.broadleafcommerce.profile.core.domain.CustomerImpl;
import org.broadleafcommerce.profile.core.service.CustomerService;
import org.broadleafcommerce.profile.core.service.CustomerUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.UserAuthenticationConverter;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import pl.touk.widerest.api.cart.dto.CustomerDto;
import pl.touk.widerest.api.cart.exceptions.CustomerNotFoundException;
import pl.touk.widerest.api.DtoConverters;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/customers")
@Api(value = "customers", description = "Customer management endpoint")
public class CustomerController {

    @Resource(name="blCustomerService")
    private CustomerService customerService;

    @Resource(name="blOrderService")
    private OrderService orderService;

    @PersistenceContext(unitName = "blPU")
    protected EntityManager em;


    @Transactional
    //@PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @ApiOperation(value = "Get a single customer details", response = CustomerDto.class)
    public ResponseEntity<CustomerDto> readOneCustomer(@PathVariable(value = "id") Long customerId) {

        CustomerDto customer = Optional.ofNullable(customerService.readCustomerById(customerId))
                .map(DtoConverters.customerEntityToDto)
                .orElseThrow(CustomerNotFoundException::new);
        return new ResponseEntity<>(customer, HttpStatus.OK);
    }

    @Transactional
    //@PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(method = RequestMethod.GET)
    @ApiOperation(
            value = "List all customers",
            notes = "Gets a list of all currently active customers",
            response = CustomerDto.class,
            responseContainer = "List"
    )
    public List<CustomerDto> readAllCustomers() {
        return getAllCustomers().stream().map(DtoConverters.customerEntityToDto).collect(Collectors.toList());
    }


    @PostAuthorize("hasRole('PERMISSION_ALL_CUSTOMER')")
    private List<Customer> getAllCustomers() {
        CriteriaBuilder builder = this.em.getCriteriaBuilder();
        CriteriaQuery criteria = builder.createQuery(Customer.class);
        Root customer = criteria.from(CustomerImpl.class);
        criteria = criteria.select(customer);
        TypedQuery query = this.em.createQuery(criteria);
        query.setHint("org.hibernate.cacheable", Boolean.valueOf(true));
        query.setHint("org.hibernate.cacheRegion", "query.Order");
        return query.getResultList();
    }


    private void mergeUsers(
            @AuthenticationPrincipal CustomerUserDetails anonymousUser,
            @AuthenticationPrincipal CustomerUserDetails loggedInUser) {

        TokenStore tokenStore = null;

        UserAuthenticationConverter userAuthenticationConverter;

        Collection<OAuth2AccessToken> anonToken = tokenStore.findTokensByClientId(anonymousUser.getId().toString());

    }

}
