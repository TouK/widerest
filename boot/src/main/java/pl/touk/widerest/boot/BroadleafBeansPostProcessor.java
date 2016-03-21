package pl.touk.widerest.boot;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.boot.context.embedded.ServletRegistrationBean;

import java.util.Arrays;

public class BroadleafBeansPostProcessor implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory bf) throws BeansException {

        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) bf;

        disableAutomaticBroadleafFiltersRegistration(beanFactory);
        registerDatasourceAliases(beanFactory);
        removeDuplicatedJpaAdapter(beanFactory);
    }

    protected void registerDatasourceAliases(DefaultListableBeanFactory beanFactory) {
        beanFactory.registerAlias("dataSource", "webDS");
        beanFactory.registerAlias("dataSource", "webStorageDS");
        beanFactory.registerAlias("dataSource", "webSecureDS");
    }

    protected void disableAutomaticBroadleafFiltersRegistration(DefaultListableBeanFactory beanFactory) {
        Arrays.stream(beanFactory.getBeanNamesForType(javax.servlet.Filter.class))
                .filter(name -> name.startsWith("bl"))
                .forEach(name -> {
                    BeanDefinition definition = BeanDefinitionBuilder
                            .genericBeanDefinition(FilterRegistrationBean.class)
                            .setScope(BeanDefinition.SCOPE_SINGLETON)
                            .addConstructorArgReference(name)
                            .addConstructorArgValue(new ServletRegistrationBean[]{})
                            .addPropertyValue("enabled", false)
                            .getBeanDefinition();

                    beanFactory.registerBeanDefinition(name + "FilterRegistrationBean",
                            definition);
                });

    }

    protected void removeDuplicatedJpaAdapter(DefaultListableBeanFactory beanFactory) {
        if(beanFactory.containsBeanDefinition("jpaVendorAdapter")) {
            ((DefaultListableBeanFactory)beanFactory).removeBeanDefinition("blJpaVendorAdapter");
            beanFactory.registerAlias("jpaVendorAdapter", "blJpaVendorAdapter");
        }
    }

}
