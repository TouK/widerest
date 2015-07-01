package pl.touk.widerest.security.config;

import org.broadleafcommerce.profile.core.service.UserDetailsServiceImpl;
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

@Configuration
@EnableWebMvcSecurity
@Order(101)
public class SiteWebSecurityConfig extends WebSecurityConfigurerAdapter {

    public SiteWebSecurityConfig() {
        super(false);
    }

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Bean
    public UserDetailsService transacionalSiteUserDetailsService() {
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
        auth.userDetailsService(transacionalSiteUserDetailsService());
    }

    @Override
    @Bean
    public AuthenticationManager authenticationManagerBean() throws Exception {
        AuthenticationManager authenticationManager = super.authenticationManagerBean();
        return authenticationManager;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .antMatcher("/site/**")
                .authorizeRequests()
                    .anyRequest().authenticated()
                    .and()
                .formLogin().loginPage("/site/login").permitAll().and()
                .httpBasic()
        ;
    }

}
