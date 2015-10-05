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
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private PasswordEncoder blAdminPasswordEncoder;

    @Bean
    public AuthenticationProvider backofficeAuthenticationProvider() {
        DaoAuthenticationProvider provider = new CustomAuthenticationProvider<>(BackofficeAuthenticationToken.class);
        provider.setUserDetailsService(backofficeUserDetailsService);
        provider.setPasswordEncoder(blAdminPasswordEncoder);
        return provider;
    }

    @Autowired
    private UserDetailsServiceImpl siteUserDetailsService;

    @Autowired
    private PasswordEncoder blPasswordEncoder;

    @Bean
    public AuthenticationProvider siteAuthenticationProvider() {
        DaoAuthenticationProvider provider = new CustomAuthenticationProvider<>(SiteAuthenticationToken.class);
        provider.setUserDetailsService(siteUserDetailsService);
        provider.setPasswordEncoder(blPasswordEncoder);
        return provider;
    }

//    @Bean
//    public AuthenticationProvider prefixBasedAuthenticationProvider() {
//        PrefixBasedAuthenticationProvider provider = new PrefixBasedAuthenticationProvider();
//
//        provider.addProvider("site",
//                siteAuthenticationProvider(),
//                (username, credentials) -> new SiteAuthenticationToken(username, credentials)
//        );
//        provider.addProvider("backoffice",
//                backofficeAuthenticationProvider(),
//                (username, credentials) -> new BackofficeAuthenticationToken(username, credentials)
//        );
//        return provider;
//    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth
                .authenticationProvider(backofficeAuthenticationProvider())
                .authenticationProvider(siteAuthenticationProvider())
                //.authenticationProvider(prefixBasedAuthenticationProvider())
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
                .requestMatchers()
                    .antMatchers("/oauth/authorize", "/login", "/logout", "/webjars/**", "/css/**")
                    .and()
                .authorizeRequests()
                    .antMatchers("/webjars/**", "/css/**").permitAll()
                    .anyRequest().permitAll()
                    .and()
                .formLogin().disable()
                .apply(new CustomFormLoginConfigurer<HttpSecurity>()).loginPage("/login").permitAll().and()
                .logout().permitAll().and()
                .anonymous().and()
                .httpBasic().and()
                .csrf().disable();
    }

}
