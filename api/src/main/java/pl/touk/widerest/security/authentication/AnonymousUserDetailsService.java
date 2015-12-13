package pl.touk.widerest.security.authentication;

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

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

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
        Customer customer = createAnonymousCustomer();
        UserDetails anonymous = createCustomerUserDetails(customer);
        return anonymous;
    }

    public UserDetails createCustomerUserDetails(Customer customer) {
        List<GrantedAuthority> grantedAuthorities = createGrantedAuthorities(roleService.findCustomerRolesByCustomerId(customer.getId()));
        CustomerUserDetails userDetails = new CustomerUserDetails(customer.getId(), customer.getUsername(), customer.getPassword(), !customer.isDeactivated(), true, !customer.isPasswordChangeRequired(), true, grantedAuthorities);
        userDetails.eraseCredentials();
        return userDetails;
    }

    protected List<GrantedAuthority> createGrantedAuthorities(List<CustomerRole> customerRoles) {
        boolean roleUserFound = false;

        List<GrantedAuthority> grantedAuthorities = new ArrayList<GrantedAuthority>();
        for (CustomerRole role : customerRoles) {
            grantedAuthorities.add(new SimpleGrantedAuthority(role.getRoleName()));
            if (role.getRoleName().equals("ROLE_USER")) {
                roleUserFound = true;
            }
        }

        if (!roleUserFound) {
            grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        return grantedAuthorities;
    }

}
