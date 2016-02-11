package pl.touk.widerest.security.authentication;

import java.util.Optional;

import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configurers.AbstractAuthenticationFilterConfigurer;
import org.springframework.security.web.authentication.ui.DefaultLoginPageGeneratingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

public final class UsertypeFormLoginConfigurer<H extends HttpSecurityBuilder<H>> extends AbstractAuthenticationFilterConfigurer<H,UsertypeFormLoginConfigurer<H>,UsertypeFormLoginAuthenticationFilter> {

    public UsertypeFormLoginConfigurer() {
        super(new UsertypeFormLoginAuthenticationFilter(),null);
        usernameParameter("username");
        usertypeParameter("usertype");
        passwordParameter("password");
    }

    public UsertypeFormLoginConfigurer<H> loginPage(String loginPage) {
        return super.loginPage(loginPage);
    }

    public UsertypeFormLoginConfigurer<H> usernameParameter(String usernameParameter) {
        getAuthenticationFilter().setUsernameParameter(usernameParameter);
        return this;
    }

    public UsertypeFormLoginConfigurer<H> usertypeParameter(String usertypeParameter) {
        getAuthenticationFilter().setUsertypeParameter(usertypeParameter);
        return this;
    }

    public UsertypeFormLoginConfigurer<H> passwordParameter(String passwordParameter) {
        getAuthenticationFilter().setPasswordParameter(passwordParameter);
        return this;
    }

    @Override
    public void init(H http) throws Exception {
        super.init(http);
        initDefaultLoginFilter(http);
    }

    @Override
    protected RequestMatcher createLoginProcessingUrlMatcher(
            String loginProcessingUrl) {
        return new AntPathRequestMatcher(loginProcessingUrl, "POST");
    }

    private String getUsernameParameter() {
        return getAuthenticationFilter().getUsernameParameter();
    }

    private String getPasswordParameter() {
        return getAuthenticationFilter().getPasswordParameter();
    }

    private void initDefaultLoginFilter(H http) {
        Optional.ofNullable(http.getSharedObject(DefaultLoginPageGeneratingFilter.class))
                .filter(f -> !isCustomLoginPage())
                .ifPresent(f -> {
                    f.setFormLoginEnabled(true);
                    f.setUsernameParameter(getUsernameParameter());
                    f.setPasswordParameter(getPasswordParameter());
                    f.setLoginPageUrl(getLoginPage());
                    f.setFailureUrl(getFailureUrl());
                    f.setAuthenticationUrl(getLoginProcessingUrl());
                });
    }
}