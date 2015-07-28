import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;

@Component
public class BroadleafBeansPostProcessor implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        registerAliases(beanFactory);
        removeWebFilters((DefaultListableBeanFactory) beanFactory);
    }

    private void registerAliases(ConfigurableListableBeanFactory beanFactory) {
        beanFactory.registerAlias("dataSource", "webDS");
        beanFactory.registerAlias("dataSource", "webStorageDS");
        beanFactory.registerAlias("dataSource", "webSecureDS");
    }

    private void removeWebFilters(DefaultListableBeanFactory beanFactory) {
        for (String beanName : beanFactory.getBeanNamesForType(Filter.class)) {
            BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
            String beanClassName = beanDefinition.getBeanClassName();
            if (beanClassName != null && beanClassName.startsWith("org.broadleafcommerce")) {
                beanFactory.removeBeanDefinition(beanName);
            }
        }

    }
}
