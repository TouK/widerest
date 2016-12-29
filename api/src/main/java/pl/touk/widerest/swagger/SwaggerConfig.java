package pl.touk.widerest.swagger;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import pl.touk.widerest.security.oauth2.Scope;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.GrantType;
import springfox.documentation.service.ImplicitGrant;
import springfox.documentation.service.LoginEndpoint;
import springfox.documentation.service.OAuth;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.service.SecurityScheme;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger.web.UiConfiguration;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static springfox.documentation.builders.PathSelectors.regex;

@Configuration
@EnableSwagger2
public class SwaggerConfig {

    public static final String API_REFERENCE = "apiImplicit";

    private final List<AuthorizationScope> authorizationScopes =
            Arrays.stream(Scope.values())
                    .map(s -> new AuthorizationScope(s.toString(), s.toString()))
                    .collect(Collectors.toList());

    @Bean
    public Docket apiDocket(ApiInfo apiInfo) {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("api")
                .apiInfo(apiInfo)
                .select().paths(regex("/v1/.*")).build()
                .consumes(Sets.newHashSet(MediaType.APPLICATION_JSON_VALUE))
                //.produces(Sets.newHashSet(MediaType.APPLICATION_JSON_VALUE))
                .produces(Sets.newHashSet(MediaTypes.HAL_JSON_VALUE))
                .securityContexts(Lists.newArrayList(apiSecurityContext()))
                .securitySchemes(Lists.newArrayList(apiImplicitScheme()));

    }

    private SecurityScheme apiImplicitScheme() {
        final LoginEndpoint loginEndpoint = new LoginEndpoint("/oauth/authorize") {
            @Override
            public String getUrl() {
                return Optional.ofNullable((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                        .map(ServletRequestAttributes::getRequest)
                        .map(ServletServerHttpRequest::new)
                        .map(UriComponentsBuilder::fromHttpRequest)
                        .map(uriComponentsBuilder -> uriComponentsBuilder.replacePath(super.getUrl()).replaceQuery(null))
                        .map(UriComponentsBuilder::build)
                        .map(UriComponents::toUriString)
                        .orElseGet(super::getUrl);
            }
        };
        final GrantType grantType = new ImplicitGrant(loginEndpoint, "access_token");

        return new OAuth(API_REFERENCE, authorizationScopes, Lists.newArrayList(grantType));
    }

    private SecurityContext apiSecurityContext() {
        return SecurityContext.builder()
                .securityReferences(singletonList(new SecurityReference(
                        API_REFERENCE,
                        authorizationScopes.toArray(new AuthorizationScope[authorizationScopes.size()])
                )))
                .forPaths(PathSelectors.regex("/.*"))
                .build();
    }

    @Bean
    public UiConfiguration uiConfiguration() {
        return new UiConfiguration("https://online.swagger.io/validator/");
    }

    @Bean
    public ApiInfo apiInfo() {
        return new ApiInfo(
                "Widerest",
                "RESTful API for Broadleaf Commerce - an open source eCommerce platform based on the Spring Framework",
                String.valueOf(getClass().getPackage().getImplementationVersion()),
                "Widerest terms of service",
                "info@touk.pl",
                "Widerest Licence",
                "license.html"
        );
    }


}
