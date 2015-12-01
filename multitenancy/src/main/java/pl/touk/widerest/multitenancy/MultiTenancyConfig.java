package pl.touk.widerest.multitenancy;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.swagger.models.auth.OAuth2Definition;
import io.swagger.models.auth.SecuritySchemeDefinition;
import liquibase.integration.spring.MultiTenantSpringLiquibase;
import liquibase.integration.spring.SpringLiquibase;
import liquibase.servicelocator.ServiceLocator;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.service.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.data.jpa.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.liquibase.CommonsLoggingLiquibaseLogger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.AbstractJpaVendorAdapter;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.util.Assert;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.GrantType;
import springfox.documentation.service.ImplicitGrant;
import springfox.documentation.service.LoginEndpoint;
import springfox.documentation.service.OAuth;
import springfox.documentation.service.ResourceListing;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.service.SecurityScheme;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.mappers.SecurityMapper;
import springfox.documentation.swagger2.mappers.SecurityMapperImpl;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static springfox.documentation.builders.PathSelectors.regex;

@Configuration
@Slf4j
public class MultiTenancyConfig {

    public static final String TENANT_IDENTIFIER_REQUEST_ATTRIBUTE = MultiTenancyConfig.class.getPackage().getName() + ".Tenant";
    public static final String DEFAULT_TENANT_IDENTIFIER = "default";
    public static final String TENANT_SCHEMA_PREFIX = "TENANT";

    @Bean
    public SpringLiquibase springLiquibase() {
        return null;
    }

    @ConditionalOnMissingBean(IdentifierTool.class)
    @Bean
    public IdentifierTool identifierTool() {
        return new IdentifierTool();
    }

    @Configuration
    @ConditionalOnMissingClass(MultiTenantSpringLiquibase.class)
    public static class SchemaExportConfiguration {
        @Bean
        public SchemaExportMultiTenancyService schemaExportMultiTenancyService() {
            return new SchemaExportMultiTenancyService();
        }
    }

    @Configuration
    @ConditionalOnClass(MultiTenantSpringLiquibase.class)
    @EnableConfigurationProperties(LiquibaseProperties.class)
    @Import(LiquibaseJpaDependencyConfiguration.class)
    public static class LiquibaseConfiguration {

        @Autowired
        private LiquibaseProperties properties = new LiquibaseProperties();

        @Autowired
        private ResourceLoader resourceLoader = new DefaultResourceLoader();

        @Autowired
        private DataSource dataSource;

        @PostConstruct
        public void checkChangelogExists() {
            if (this.properties.isCheckChangeLogLocation()) {
                org.springframework.core.io.Resource resource = this.resourceLoader.getResource(this.properties
                        .getChangeLog());
                Assert.state(resource.exists(), "Cannot find db.changelog location: "
                        + resource + " (please add db.changelog or check your Liquibase "
                        + "configuration)");
            }
            ServiceLocator serviceLocator = ServiceLocator.getInstance();
            serviceLocator.addPackageToScan(CommonsLoggingLiquibaseLogger.class
                    .getPackage().getName());
        }

        @Bean
        public MultiTenantSpringLiquibase liquibase() throws SQLException {
            List<String> schemas = Lists.newArrayList("default");
            ResultSet rs = getDataSource().getConnection().getMetaData().getSchemas(null, TENANT_SCHEMA_PREFIX + "%");
            try {
                while (rs.next()) {
                    schemas.add(rs.getString(1));
                }
            } finally {
                rs.close();
            }

            MultiTenantSpringLiquibase liquibase = new MultiTenantSpringLiquibase();
            liquibase.setChangeLog(this.properties.getChangeLog());
            liquibase.setContexts(this.properties.getContexts());
            liquibase.setDataSource(getDataSource());
            liquibase.setDropFirst(this.properties.isDropFirst());
            liquibase.setShouldRun(this.properties.isEnabled());
            liquibase.setSchemas(schemas);
            return liquibase;
        }

        private DataSource getDataSource() {
            if (this.properties.getUrl() == null) {
                return this.dataSource;
            }
            return DataSourceBuilder.create().url(this.properties.getUrl())
                    .username(this.properties.getUser())
                    .password(this.properties.getPassword()).build();
        }

        @Bean
        public LiquibaseMultiTenancyService liquibaseMultiTenancyService() {
            return new LiquibaseMultiTenancyService();
        }

    }

    /**
     * Additional configuration to ensure that {@link EntityManagerFactory} beans
     * depend-on the liquibase bean.
     */
    @Configuration
    @ConditionalOnClass(LocalContainerEntityManagerFactoryBean.class)
    @ConditionalOnBean(LiquibaseMultiTenancyService.class)
    protected static class LiquibaseJpaDependencyConfiguration extends
            EntityManagerFactoryDependsOnPostProcessor {

        public LiquibaseJpaDependencyConfiguration() {
            super("liquibase");
        }

    }

    @Bean
    public JpaVendorAdapter jpaVendorAdapter(
            JpaProperties jpaProperties,
            CurrentTenantIdentifierResolver tenantIdentifierResolver,
            MultiTenantConnectionProvider multiTenantConnectionProvider)
    {
        AbstractJpaVendorAdapter adapter = new HibernateJpaVendorAdapter() {
            @Override
            public Map<String, Object> getJpaPropertyMap() {
                Map<String, Object> jpaPropertyMap = super.getJpaPropertyMap();
                jpaPropertyMap.put(AvailableSettings.MULTI_TENANT, MultiTenancyStrategy.SCHEMA);
                jpaPropertyMap.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, tenantIdentifierResolver);
                jpaPropertyMap.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, multiTenantConnectionProvider);
                return jpaPropertyMap;
            }
        };
        adapter.setShowSql(jpaProperties.isShowSql());
        adapter.setDatabase(jpaProperties.getDatabase());
        adapter.setDatabasePlatform(jpaProperties.getDatabasePlatform());
        adapter.setGenerateDdl(jpaProperties.isGenerateDdl());
        return adapter;
    }

    @Configuration
    @ConditionalOnClass(Docket.class)
    public static class SpringFoxConfiguration {

        public static final String TENANT_REFERENCE = "tenantImplicit";

        @Autowired(required = false)
        private ApiInfo apiInfo;

        @Bean
        public Docket tenantDocket() {
            return new Docket(DocumentationType.SWAGGER_2)
                    .groupName("tenant")
                    .apiInfo(apiInfo)
                    .select().paths(regex("/tenant")).build()
                    .consumes(Sets.newHashSet(MediaType.APPLICATION_JSON_VALUE))
                    .produces(Sets.newHashSet(MediaType.APPLICATION_JSON_VALUE))
                    .securityContexts(Lists.newArrayList(tenantSecurityContext()))
                    .securitySchemes(Lists.newArrayList(tenantImplicitScheme()));
        }

        private SecurityScheme tenantImplicitScheme() {
            List<AuthorizationScope> authorizationScopes = Arrays.asList();
            LoginEndpoint loginEndpoint = new LoginEndpoint("https://touk-io.eu.auth0.com/authorize");
            GrantType grantType = new ImplicitGrant(loginEndpoint, "id_token");
            return new OAuth(TENANT_REFERENCE, authorizationScopes, Lists.newArrayList(grantType));
        }

        private SecurityContext tenantSecurityContext() {
            return SecurityContext.builder()
                    .securityReferences(
                            Lists.newArrayList(
                                    new SecurityReference(TENANT_REFERENCE, new AuthorizationScope[0])
                            )
                    )
                    .forPaths(PathSelectors.regex("/.*"))
                    .build();
        }

        @Bean
        @Primary
        SecurityMapper securityMapperWorkaround() {
            return new SecurityMapperImpl() {
                @Override
                public Map<String, SecuritySchemeDefinition> toSecuritySchemeDefinitions(ResourceListing from) {
                    Map<String, SecuritySchemeDefinition> def = super.toSecuritySchemeDefinitions(from);
                    SecuritySchemeDefinition old = def.get(TENANT_REFERENCE);
                    if (old != null) {
                        OAuth2Definition updated = new OAuth2Definition() {
                            public String tokenName = "id_token";
                        };
                        BeanUtils.copyProperties(old, updated);
                        def = Maps.newHashMap(def);
                        def.put(TENANT_REFERENCE, updated);
                    }

                    return def;
                }
            };
        }
    };

}
