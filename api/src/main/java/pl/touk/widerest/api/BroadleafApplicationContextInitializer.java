package pl.touk.widerest.api;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.springframework.context.ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS;
import static org.springframework.util.StringUtils.tokenizeToStringArray;
import static pl.touk.throwing.ThrowingFunction.unchecked;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

public class BroadleafApplicationContextInitializer implements ApplicationContextInitializer<GenericApplicationContext> {

    private static final Log LOG = LogFactory.getLog(BroadleafApplicationContextInitializer.class);

    private String patchLocation;

    public BroadleafApplicationContextInitializer() {
        setPatchLocation(
                "classpath:/bl-open-admin-contentClient-applicationContext.xml\n" +
                        "classpath:/bl-open-admin-contentCreator-applicationContext.xml\n" +
                        "classpath:/bl-cms-contentClient-applicationContext.xml\n" +
                        "classpath:/bl-common-applicationContext.xml\n" +
                        //"classpath*:/blc-config/site/bl-*-applicationContext.xml\n" +
                        "classpath:/applicationContext.xml\n" +
                        "classpath:/applicationContext-email.xml\n" +
                        "classpath:/applicationContext-security.xml\n"
        );

    }

    protected int loadBeanDefinitions(XmlBeanDefinitionReader reader, ImportProcessor importProcessor) throws BeansException, IOException {
        final int standardLocationTypes = StandardConfigLocations.SERVICECONTEXTTYPE;

        final ResourceInputStream[] filteredSources = stream(StandardConfigLocations.retrieveAll(standardLocationTypes))
                .map(location -> {
                    final InputStream is = MergeXmlWebApplicationContext.class.getClassLoader().getResourceAsStream(
                            location);
                    return new ResourceInputStream(is, location);
                })
                .toArray(ResourceInputStream[]::new);

        final List<ResourceInputStream> patchList = stream(
                tokenizeToStringArray(getPatchLocation(), CONFIG_LOCATION_DELIMITERS))
                .flatMap(unchecked(l -> {
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
            LOG.warn(
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
            LOG.error(e);
        }
    }
}
