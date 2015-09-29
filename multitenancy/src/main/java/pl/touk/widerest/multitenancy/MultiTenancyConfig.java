package pl.touk.widerest.multitenancy;

import com.google.common.collect.Lists;
import liquibase.integration.spring.MultiTenantSpringLiquibase;
import liquibase.servicelocator.ServiceLocator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.data.jpa.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.liquibase.CommonsLoggingLiquibaseLogger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.security.jwt.crypto.sign.MacSigner;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Configuration
@Slf4j
public class MultiTenancyConfig {

    public static final String TENANT_REQUEST_ATTRIBUTE = MultiTenancyConfig.class.getPackage().getName() + ".Tenant";
    public static final String DEFAULT_TENANT_IDENTIFIER ="DEFAULT";
    public static final String TENANT_SCHEMA_PREFIX = "TENANT";

    @Value("${mulititenacy.tokenSecret:secret}")
    private String tenantTokenSecret;

    @Bean
    public MacSigner macSigner() {
        return new MacSigner(tenantTokenSecret);
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

}
