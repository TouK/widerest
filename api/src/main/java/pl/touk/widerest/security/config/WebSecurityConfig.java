package pl.touk.widerest.security.config;

import org.broadleafcommerce.openadmin.server.security.service.AdminUserDetailsServiceImpl;
import org.broadleafcommerce.profile.core.service.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.servlet.configuration.EnableWebMvcSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.web.bind.annotation.SessionAttributes;
import pl.touk.widerest.security.authentication.BackofficeAuthenticationToken;
import pl.touk.widerest.security.authentication.CustomAuthenticationProvider;
import pl.touk.widerest.security.authentication.CustomFormLoginConfigurer;
import pl.touk.widerest.security.authentication.SiteAuthenticationToken;

@Configuration
@EnableWebMvcSecurity
@SessionAttributes("authorizationRequest")
@Order(-10)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    public WebSecurityConfig() {
        super(false);
    }

    @Autowired
    private AdminUserDetailsServiceImpl backofficeUserDetailsService;

    @Autowired
    private UserDetailsServiceImpl siteUserDetailsService;

    @Bean
    public AuthenticationProvider backofficeAuthenticationProvider() {
        DaoAuthenticationProvider provider = new CustomAuthenticationProvider<>(BackofficeAuthenticationToken.class);
        provider.setUserDetailsService(backofficeUserDetailsService);
        return provider;
    }

    @Bean
    public AuthenticationProvider siteAuthenticationProvider() {
        DaoAuthenticationProvider provider = new CustomAuthenticationProvider<>(SiteAuthenticationToken.class);
        provider.setUserDetailsService(siteUserDetailsService);
        return provider;
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth
                .authenticationProvider(backofficeAuthenticationProvider())
                .authenticationProvider(siteAuthenticationProvider())
        ;
    }

    @Override
    @Bean(name = "authenticationManager")
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                    .antMatchers("/webjars/**", "/css/**").permitAll()
                    .anyRequest().permitAll()
                    .and()
                .formLogin().disable()
                .apply(new CustomFormLoginConfigurer<HttpSecurity>()).loginPage("/login").permitAll().and()
                .logout().permitAll().and()
                .anonymous()
                .and().csrf().disable();
    }

}
