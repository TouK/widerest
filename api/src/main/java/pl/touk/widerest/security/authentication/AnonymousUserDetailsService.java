package pl.touk.widerest.security.authentication;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Resource;

import org.apache.commons.lang3.RandomStringUtils;
import org.broadleafcommerce.profile.core.domain.Customer;
import org.broadleafcommerce.profile.core.domain.CustomerRole;
import org.broadleafcommerce.profile.core.service.CustomerService;
import org.broadleafcommerce.profile.core.service.CustomerUserDetails;
import org.broadleafcommerce.profile.core.service.RoleService;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class AnonymousUserDetailsService  {

    @Resource(name = "blCustomerService")
    protected CustomerService customerService;

    @Resource(name = "blRoleService")
    protected RoleService roleService;

    public Customer createAnonymousCustomer() throws DataAccessException {
        Customer customer = customerService.createNewCustomer();
        customer.setUsername(String.valueOf(customer.getId()));
        customer.setPassword(RandomStringUtils.randomAscii(8));
        return customerService.saveCustomer(customer);
    }

    public UserDetails createAnonymousUserDetails() throws DataAccessException {
        return createCustomerUserDetails(createAnonymousCustomer());
    }

    public UserDetails createCustomerUserDetails(Customer customer) {
        List<GrantedAuthority> grantedAuthorities = createGrantedAuthorities(roleService.findCustomerRolesByCustomerId(customer.getId()));
        CustomerUserDetails userDetails = new CustomerUserDetails(customer.getId(), customer.getUsername(), customer.getPassword(), !customer.isDeactivated(), true, !customer.isPasswordChangeRequired(), true, grantedAuthorities);
        userDetails.eraseCredentials();
        return userDetails;
    }

    protected List<GrantedAuthority> createGrantedAuthorities(List<CustomerRole> customerRoles) {
        return customerRoles.stream()
                .flatMap(r -> {
                    final Stream.Builder<SimpleGrantedAuthority> builder = Stream.<SimpleGrantedAuthority>builder().add(new
                            SimpleGrantedAuthority(r.getRoleName()));

                    if (r.getRoleName().equals("ROLE_USER")) {
                        builder.add(new SimpleGrantedAuthority("ROLE_USER"));
                    }

                    return builder.build();
                }).distinct()
                .collect(toList());
    }

}
