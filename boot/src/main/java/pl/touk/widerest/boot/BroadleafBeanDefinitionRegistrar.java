package pl.touk.widerest.boot;

import javaslang.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.broadleafcommerce.common.extensibility.context.MergeApplicationContextXmlConfigResource;
import org.broadleafcommerce.common.extensibility.context.ResourceInputStream;
import org.broadleafcommerce.common.extensibility.context.StandardConfigLocations;
import org.broadleafcommerce.common.extensibility.context.merge.ImportProcessor;
import org.broadleafcommerce.common.extensibility.context.merge.exceptions.MergeException;
import org.broadleafcommerce.common.web.extensibility.MergeXmlWebApplicationContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@Slf4j
public abstract class BroadleafBeanDefinitionRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);
        ImportProcessor importProcessor = new ImportProcessor(new DefaultResourceLoader());
        try {
            loadBeanDefinitions(reader, importProcessor);
        } catch (IOException e) {
            log.error("Error while merging bean defininitions", e);
        }
        //        applicationContext.registerBeanDefinition("blStaticAssetURLHandlerMapping", new RootBeanDefinition(BroadleafCmsSimpleUrlHandlerMapping.class));
//        applicationContext.registerBeanDefinition("blConfiguration", new RootBeanDefinition(RuntimeEnvironmentPropertiesConfigurer.class));

//        registry.removeBeanDefinition("blStaticAssetURLHandlerMapping");
//        registry.removeBeanDefinition("blConfiguration");
    }

    abstract protected String getPatchLocation();


    protected int loadBeanDefinitions(XmlBeanDefinitionReader reader, ImportProcessor importProcessor) throws BeansException, IOException {
        final int standardLocationTypes = StandardConfigLocations.APPCONTEXTTYPE;

        String[] broadleafConfigLocations = StandardConfigLocations.retrieveAll(standardLocationTypes);

        final ResourceInputStream[] filteredSources = Arrays.stream(StandardConfigLocations.retrieveAll(standardLocationTypes))
                .map(location -> {
                    final InputStream is = MergeXmlWebApplicationContext.class.getClassLoader().getResourceAsStream(
                            location);
                    return new ResourceInputStream(is, location);
                })
                .toArray(ResourceInputStream[]::new);

        final List<ResourceInputStream> patchList = Arrays.stream(StringUtils.tokenizeToStringArray(getPatchLocation(), ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS))
                .flatMap(l -> Try.of(() -> {
                    if (!l.startsWith("classpath")) {
                        throw new NotImplementedException("Only classpath resources merge implemented");
                    }

                    final InputStream is = MergeXmlWebApplicationContext.class.getClassLoader().getResourceAsStream(
                            l.substring("classpath*:".length(), l.length()));
                    final ResourceInputStream patch = new ResourceInputStream(is, l);

                    return patch.available() <= 0 ? getResourcesFromPatternResolver(l).stream() : Stream.of(patch);

                }).get()).collect(toList());

        try {
            final ResourceInputStream[] extractedSources = importProcessor.extract(filteredSources);
            final ResourceInputStream[] patchArray = importProcessor.extract(
                    patchList.toArray(new ResourceInputStream[patchList.size()]));

            final org.springframework.core.io.Resource[] resources = new MergeApplicationContextXmlConfigResource().getConfigResources(
                    extractedSources, patchArray);

            return reader.loadBeanDefinitions(resources);
        } catch (MergeException e) {
            throw new FatalBeanException("Unable to merge source and patch locations", e);
        }
    }

    protected List<ResourceInputStream> getResourcesFromPatternResolver(String patchLocation) throws IOException {
        ResourceInputStream resolverPatch;
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        org.springframework.core.io.Resource[] resources = resolver.getResources(patchLocation);
        List<ResourceInputStream> resolverList = new ArrayList<ResourceInputStream>();

        if (ArrayUtils.isEmpty(resources)) {
            log.warn(
                    "Unable to use automatic applicationContext loading. To avoid this, upgrade your poms to reference the latest versions of all modules.");
            return resolverList;
        }

        for (org.springframework.core.io.Resource resource : resources) {
            resolverPatch = new ResourceInputStream(resource.getInputStream(), patchLocation);
            if (resolverPatch.available() <= 0) {
                throw new IOException(
                        "Unable to open an input stream on specified application context resource: " + patchLocation);
            }
            resolverList.add(resolverPatch);
        }

        return resolverList;
    }

}
