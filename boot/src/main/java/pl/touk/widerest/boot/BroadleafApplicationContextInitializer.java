package pl.touk.widerest.boot;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.broadleafcommerce.common.extensibility.context.MergeApplicationContextXmlConfigResource;
import org.broadleafcommerce.common.extensibility.context.ResourceInputStream;
import org.broadleafcommerce.common.extensibility.context.StandardConfigLocations;
import org.broadleafcommerce.common.extensibility.context.merge.ImportProcessor;
import org.broadleafcommerce.common.extensibility.context.merge.exceptions.MergeException;
import org.broadleafcommerce.common.web.extensibility.MergeXmlWebApplicationContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.StringUtils;
import pl.touk.throwing.ThrowingFunction;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@Slf4j
public class BroadleafApplicationContextInitializer implements ApplicationContextInitializer<GenericApplicationContext> {

    private String patchLocation;

    public BroadleafApplicationContextInitializer() {
    }

    public BroadleafApplicationContextInitializer(String patchLocation) {
        setPatchLocation(patchLocation);
    }

    protected int loadBeanDefinitions(XmlBeanDefinitionReader reader, ImportProcessor importProcessor) throws BeansException, IOException {
        final int standardLocationTypes = StandardConfigLocations.APPCONTEXTTYPE;

        final ResourceInputStream[] filteredSources = Arrays.stream(StandardConfigLocations.retrieveAll(standardLocationTypes))
                .map(location -> {
                    final InputStream is = MergeXmlWebApplicationContext.class.getClassLoader().getResourceAsStream(
                            location);
                    return new ResourceInputStream(is, location);
                })
                .toArray(ResourceInputStream[]::new);

        final List<ResourceInputStream> patchList = Arrays.stream(
                StringUtils.tokenizeToStringArray(getPatchLocation(), ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS))
                .flatMap(ThrowingFunction.unchecked(l -> {
                    if (!l.startsWith("classpath")) {
                        throw new NotImplementedException("Only classpath resources merge implemented");
                    }

                    final InputStream is = MergeXmlWebApplicationContext.class.getClassLoader().getResourceAsStream(
                            l.substring("classpath*:".length(), l.length()));
                    final ResourceInputStream patch = new ResourceInputStream(is, l);

                    return patch.available() <= 0 ? getResourcesFromPatternResolver(l).stream() : Stream.of(patch);

                })).collect(toList());

        try {
            final ResourceInputStream[] extractedSources = importProcessor.extract(filteredSources);
            final ResourceInputStream[] patchArray = importProcessor.extract(
                    patchList.toArray(new ResourceInputStream[patchList.size()]));

            final Resource[] resources = new MergeApplicationContextXmlConfigResource().getConfigResources(
                    extractedSources, patchArray);

            return reader.loadBeanDefinitions(resources);
        } catch (MergeException e) {
            throw new FatalBeanException("Unable to merge source and patch locations", e);
        }
    }

    protected List<ResourceInputStream> getResourcesFromPatternResolver(String patchLocation) throws IOException {
        ResourceInputStream resolverPatch;
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources(patchLocation);
        List<ResourceInputStream> resolverList = new ArrayList<ResourceInputStream>();

        if (ArrayUtils.isEmpty(resources)) {
            log.warn(
                    "Unable to use automatic applicationContext loading. To avoid this, upgrade your poms to reference the latest versions of all modules.");
            return resolverList;
        }

        for (Resource resource : resources) {
            resolverPatch = new ResourceInputStream(resource.getInputStream(), patchLocation);
            if (resolverPatch.available() <= 0) {
                throw new IOException(
                        "Unable to open an input stream on specified application context resource: " + patchLocation);
            }
            resolverList.add(resolverPatch);
        }

        return resolverList;
    }

    /**
     * @return the patchLocation
     */
    public String getPatchLocation() {
        return patchLocation;
    }

    /**
     * @param patchLocation the patchLocation to set
     */
    public void setPatchLocation(String patchLocation) {
        this.patchLocation = patchLocation;
    }

    @Override
    public void initialize(GenericApplicationContext applicationContext) {
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(applicationContext);
        ImportProcessor importProcessor = new ImportProcessor(applicationContext);
        try {
            loadBeanDefinitions(reader, importProcessor);
        } catch (IOException e) {
            log.error("Error while merging bean defininitions", e);
        }
    }
}
