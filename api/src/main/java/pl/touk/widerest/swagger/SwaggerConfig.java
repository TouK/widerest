package pl.touk.widerest.swagger;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ApiKey;
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
import springfox.documentation.swagger.web.SecurityConfiguration;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import static com.google.common.base.Predicates.or;
import static springfox.documentation.builders.PathSelectors.regex;

@Configuration
@EnableSwagger2
public class SwaggerConfig {

    @Bean
    public Docket swaggerSpringMvcPlugin() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select().paths(paths()).build()
                .consumes(Sets.newHashSet(MediaType.APPLICATION_JSON_VALUE))
                .produces(Sets.newHashSet(MediaType.APPLICATION_JSON_VALUE))
                .securityContexts(Lists.newArrayList(securityContext()))
                .securitySchemes(Lists.newArrayList(oAuthScheme(), apiKeyScheme()));
    }

    private SecurityScheme apiKeyScheme() {
        return new ApiKey("api_key", "Tenant-Token", "header");
    }

    private SecurityScheme oAuthScheme() {
        AuthorizationScope authorizationScope = new AuthorizationScope("test", "test");
        LoginEndpoint loginEndpoint = new LoginEndpoint("/oauth/authorize");
        GrantType grantType = new ImplicitGrant(loginEndpoint, "access_token");
        return new OAuth("oauth", Lists.newArrayList(authorizationScope), Lists.newArrayList(grantType));
    }

    private SecurityContext securityContext() {
        return SecurityContext.builder()
                .securityReferences(
                        Lists.newArrayList(
                                new SecurityReference("oauth", new AuthorizationScope[]{new AuthorizationScope("test", "test")}),
                                new SecurityReference("api_key", new AuthorizationScope[0])
                        )                )
                .forPaths(PathSelectors.regex("/.*"))
                .build();
    }

    private Predicate<String> paths() {
        return or(regex("/catalog/.*"), regex("/orders.*"),
                  regex("/customers.*"), regex("/paypal.*"),
                  regex("/tenant.*"), regex("/properties.*"));
    }

    @Bean
    public SecurityConfiguration security() {
        return new SecurityConfiguration(
                "test",
                "test-app-realm",
                "test-app",
                "api-key");
    }

    private ApiInfo apiInfo() {
        ApiInfo apiInfo = new ApiInfo(
                "Widerest",
                "RESTful API for Broadleaf Commerce - an open source eCommerce platform based on the Spring Framework",
                "0.0.1",
                "Widerest terms of service",
                "info@touk.pl",
                "Widerest Licence",
                "license.html"
        );
        return apiInfo;
    }


}
