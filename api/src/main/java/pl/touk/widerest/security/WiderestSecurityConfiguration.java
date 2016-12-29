package pl.touk.widerest.security;

import org.broadleafcommerce.openadmin.server.security.service.AdminUserDetailsServiceImpl;
import org.broadleafcommerce.profile.core.service.UserDetailsServiceImpl;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;
import pl.touk.widerest.security.authentication.BackofficeAuthenticationToken;
import pl.touk.widerest.security.authentication.PrefixBasedAuthenticationManager;
import pl.touk.widerest.security.authentication.SiteAuthenticationToken;
import pl.touk.widerest.security.authentication.TokenTypeSelectedAuthenticationProvider;
import pl.touk.widerest.security.authentication.UsertypeFormLoginConfigurer;

import javax.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class WiderestSecurityConfiguration extends WebSecurityConfigurerAdapter implements BeanFactoryAware {

    private AutowireCapableBeanFactory beanFactory;

    @Autowired(required = false)
    private AdminUserDetailsServiceImpl backofficeUserDetailsService;

    @Autowired(required = false)
    private PasswordEncoder blAdminPasswordEncoder;

    @Autowired(required = false)
    private UserDetailsServiceImpl siteUserDetailsService;

    @Autowired(required = false)
    private PasswordEncoder blPasswordEncoder;

    public WiderestSecurityConfiguration() {
        super(false);
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = (AutowireCapableBeanFactory) beanFactory;
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {

        if (backofficeUserDetailsService != null) {
            DaoAuthenticationProvider provider = new TokenTypeSelectedAuthenticationProvider<>(BackofficeAuthenticationToken.class);
            provider.setUserDetailsService(backofficeUserDetailsService);
            provider.setPasswordEncoder(blAdminPasswordEncoder);
            auth.authenticationProvider((AuthenticationProvider) beanFactory.initializeBean(provider, "backofficeAuthenticationProvider"));
        }
        if (siteUserDetailsService != null) {
            DaoAuthenticationProvider provider = new TokenTypeSelectedAuthenticationProvider<>(SiteAuthenticationToken.class);
            provider.setUserDetailsService(siteUserDetailsService);
            provider.setPasswordEncoder(blPasswordEncoder);
            auth.authenticationProvider((AuthenticationProvider) beanFactory.initializeBean(provider, "siteAuthenticationProvider"));
        }
    }

    @Override
    protected AuthenticationManager authenticationManager() throws Exception {
        return new PrefixBasedAuthenticationManager(super.authenticationManager());
    }

    @Override
    @Bean(name = "widerestAuthenticationManager")
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .headers().frameOptions().disable().and()
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
