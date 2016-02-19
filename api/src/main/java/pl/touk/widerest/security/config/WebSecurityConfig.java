package pl.touk.widerest.security.config;

import org.broadleafcommerce.openadmin.server.security.service.AdminUserDetailsServiceImpl;
import org.broadleafcommerce.profile.core.service.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.servlet.configuration.EnableWebMvcSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;
import pl.touk.widerest.security.authentication.BackofficeAuthenticationToken;
import pl.touk.widerest.security.authentication.PrefixBasedAuthenticationManager;
import pl.touk.widerest.security.authentication.SiteAuthenticationToken;
import pl.touk.widerest.security.authentication.TokenTypeSelectedAuthenticationProvider;
import pl.touk.widerest.security.authentication.UsertypeFormLoginConfigurer;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;

@Configuration
@EnableWebMvcSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    public WebSecurityConfig() {
        super(false);
    }

    @Configuration
    @ConditionalOnBean(AdminUserDetailsServiceImpl.class)
    public static class BackofficeAuthenticationConfig {

        @Autowired
        private AdminUserDetailsServiceImpl backofficeUserDetailsService;

        @Autowired
        private PasswordEncoder blAdminPasswordEncoder;

        @Bean
        public AuthenticationProvider backofficeAuthenticationProvider() {
            DaoAuthenticationProvider provider = new TokenTypeSelectedAuthenticationProvider<>(BackofficeAuthenticationToken.class);
            provider.setUserDetailsService(backofficeUserDetailsService);
            provider.setPasswordEncoder(blAdminPasswordEncoder);
            return provider;
        }
    }

    @Configuration
    @ConditionalOnBean(UserDetailsServiceImpl.class)
    public static class SiteAuthenticationConfig {

        @Autowired
        private UserDetailsServiceImpl siteUserDetailsService;

        @Autowired
        private PasswordEncoder blPasswordEncoder;

        @Bean
        public AuthenticationProvider siteAuthenticationProvider() {
            DaoAuthenticationProvider provider = new TokenTypeSelectedAuthenticationProvider<>(SiteAuthenticationToken.class);
            provider.setUserDetailsService(siteUserDetailsService);
            provider.setPasswordEncoder(blPasswordEncoder);
            return provider;
        }
    }

    @Resource
    Collection<AuthenticationProvider> authenticationProviders;

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        authenticationProviders.stream().forEach(auth::authenticationProvider);
    }

    @Override
    protected AuthenticationManager authenticationManager() throws Exception {
        return new PrefixBasedAuthenticationManager(super.authenticationManager());
    }

    @Override
    @Bean(name = "authenticationManager")
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .headers().frameOptions().disable()
                .authorizeRequests()
                    .anyRequest().permitAll()
                    .and()
                .formLogin().disable().apply(new UsertypeFormLoginConfigurer<HttpSecurity>())
                    .loginPage("/login")
                    .permitAll()
                    .and()
                .logout().permitAll().and()
                .anonymous().and()
                .csrf().disable()
                .exceptionHandling()
                    .defaultAuthenticationEntryPointFor(
                            (request, response, authException) ->
                                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, authException.getMessage()),
                            new RequestHeaderRequestMatcher("X-Requested-With", "XMLHttpRequest")
                    );
    }

}
