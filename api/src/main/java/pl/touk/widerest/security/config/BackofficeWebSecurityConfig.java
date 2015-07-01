package pl.touk.widerest.security.config;

import org.broadleafcommerce.openadmin.server.security.service.AdminUserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.servlet.configuration.EnableWebMvcSecurity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.SessionAttributes;

@Configuration
@EnableWebMvcSecurity
@SessionAttributes("authorizationRequest")
@Order(-10)
public class BackofficeWebSecurityConfig extends WebSecurityConfigurerAdapter {

    public BackofficeWebSecurityConfig() {
        super(false);
    }

    @Autowired
    private AdminUserDetailsServiceImpl userDetailsService;

    @Bean
    public UserDetailsService transacionalBackofficeUserDetailsService() {
        return new UserDetailsService() {

            @Override
            @Transactional
            public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
                return userDetailsService.loadUserByUsername(username);
            }
        };
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(transacionalBackofficeUserDetailsService());
    }

    @Override
    @Bean(name = "backofficeAuthenticationManager")
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .requestMatchers()
                    .antMatchers("/admin/**", "/logout")
                    .and()
                .authorizeRequests()
                    .anyRequest().authenticated()
                    .and()
                .formLogin().loginPage("/admin/login").permitAll().and()
                .logout().permitAll().and()
                .httpBasic()
        ;
    }

}
