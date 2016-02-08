package pl.touk.widerest.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.StringUtils;

public class BroadleafApplicationContextInitializer implements ApplicationContextInitializer<GenericApplicationContext> {

    private static final Log LOG = LogFactory.getLog(BroadleafApplicationContextInitializer.class);

    private String patchLocation;
    private int standardLocationTypes = StandardConfigLocations.SERVICECONTEXTTYPE;

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

    protected void loadBeanDefinitions(XmlBeanDefinitionReader reader, ImportProcessor importProcessor) throws BeansException, IOException {
        String[] broadleafConfigLocations = StandardConfigLocations.retrieveAll(standardLocationTypes);

        ArrayList<ResourceInputStream> sources = new ArrayList<ResourceInputStream>(20);
        for (String location : broadleafConfigLocations) {
            InputStream source = MergeXmlWebApplicationContext.class.getClassLoader().getResourceAsStream(location);
            if (source != null) {
                sources.add(new ResourceInputStream(source, location));
            }
        }
        ResourceInputStream[] filteredSources = new ResourceInputStream[]{};
        filteredSources = sources.toArray(filteredSources);
        String patchLocation = getPatchLocation();
        String[] patchLocations = StringUtils.tokenizeToStringArray(patchLocation, ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);
        List<ResourceInputStream> patchList = new ArrayList<ResourceInputStream>();
        for (String patchLocation1 : patchLocations) {
            ResourceInputStream patch;
            if (patchLocation1.startsWith("classpath")) {
                InputStream is = MergeXmlWebApplicationContext.class.getClassLoader().getResourceAsStream(
                        patchLocation1.substring("classpath*:".length(), patchLocation1.length()));
                patch = new ResourceInputStream(is, patchLocation1);
            } else {
                throw new NotImplementedException("Only classpath resources merge implemented");
            }
            if (patch.available() <= 0) {
                patchList.addAll(getResourcesFromPatternResolver(patchLocation1));
            } else {
                patchList.add(patch);
            }
        }

        ResourceInputStream[] patchArray;
        try {
            filteredSources = importProcessor.extract(filteredSources);
            patchArray = importProcessor.extract(patchList.toArray(new ResourceInputStream[patchList.size()]));
        } catch (MergeException e) {
            throw new FatalBeanException("Unable to merge source and patch locations", e);
        }

        Resource[] resources = new MergeApplicationContextXmlConfigResource().getConfigResources(filteredSources, patchArray);
        reader.loadBeanDefinitions(resources);
    }

    protected List<ResourceInputStream> getResourcesFromPatternResolver(String patchLocation) throws IOException {
        ResourceInputStream resolverPatch;
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources(patchLocation);
        List<ResourceInputStream> resolverList = new ArrayList<ResourceInputStream>();

        if (ArrayUtils.isEmpty(resources)) {
            LOG.warn("Unable to use automatic applicationContext loading. To avoid this, upgrade your poms to reference the latest versions of all modules.");
            return resolverList;
        }

        for (Resource resource : resources) {
            resolverPatch = new ResourceInputStream(resource.getInputStream(), patchLocation);
            if (resolverPatch.available() <= 0) {
                throw new IOException("Unable to open an input stream on specified application context resource: " + patchLocation);
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
            e.printStackTrace();
        }
    }
}
