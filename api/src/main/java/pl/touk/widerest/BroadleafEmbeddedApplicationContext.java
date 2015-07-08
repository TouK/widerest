package pl.touk.widerest;

import org.apache.commons.lang.ArrayUtils;
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
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class BroadleafEmbeddedApplicationContext extends AnnotationConfigEmbeddedWebApplicationContext {

    private static final Log LOG = LogFactory.getLog(BroadleafEmbeddedApplicationContext.class);

    private String patchLocation;
    private String shutdownBean;
    private String shutdownMethod;
    private int standardLocationTypes = StandardConfigLocations.SERVICECONTEXTTYPE;

    private final XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this);

    public BroadleafEmbeddedApplicationContext() {
        setPatchLocation(
                        "classpath:/bl-open-admin-contentClient-applicationContext.xml\n" +
                        "classpath:/bl-open-admin-contentCreator-applicationContext.xml\n" +
                        "classpath:/bl-cms-contentClient-applicationContext.xml\n" +
                        //"classpath*:/blc-config/site/bl-*-applicationContext.xml\n" +
                        "classpath:/applicationContext.xml\n" +
                        "classpath:/applicationContext-email.xml\n" +
                        "classpath:/applicationContext-security.xml\n"
                        );

    }

    /**
     * Load the bean definitions with the given XmlBeanDefinitionReader.
     * <p>The lifecycle of the bean factory is handled by the refreshBeanFactory method;
     * therefore this method is just supposed to load and/or register bean definitions.
     * <p>Delegates to a ResourcePatternResolver for resolving location patterns
     * into Resource instances.
     * @throws org.springframework.beans.BeansException in case of bean registration errors
     * @throws java.io.IOException if the required XML document isn't found
     * @see #refreshBeanFactory
     * @see #getConfigLocations
     * @see #getResources
     * @see #getResourcePatternResolver
     */
    protected void loadBeanDefinitions(XmlBeanDefinitionReader reader) throws BeansException, IOException {
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
        for (int i = 0; i < patchLocations.length; i++) {
            ResourceInputStream patch;
            if (patchLocations[i].startsWith("classpath")) {
                InputStream is = MergeXmlWebApplicationContext.class.getClassLoader().getResourceAsStream(patchLocations[i].substring("classpath*:".length(), patchLocations[i].length()));
                patch = new ResourceInputStream(is, patchLocations[i]);
            } else {
                Resource resource = getResourceByPath(patchLocations[i]);
                patch = new ResourceInputStream(resource.getInputStream(), patchLocations[i]);
            }
            if (patch == null || patch.available() <= 0) {
                patchList.addAll(getResourcesFromPatternResolver(patchLocations[i]));
            } else {
                patchList.add(patch);
            }
        }

        ImportProcessor importProcessor = new ImportProcessor(this);
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

    private List<ResourceInputStream> getResourcesFromPatternResolver(String patchLocation) throws IOException {
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
            if (resolverPatch == null || resolverPatch.available() <= 0) {
                throw new IOException("Unable to open an input stream on specified application context resource: " + patchLocation);
            }
            resolverList.add(resolverPatch);
        }

        return resolverList;
    }

    /* (non-Javadoc)
     * @see org.springframework.context.support.AbstractApplicationContext#doClose()
     */
    @Override
    protected void doClose() {
        if (getShutdownBean() != null && getShutdownMethod() != null) {
            try {
                Object shutdownBean = getBean(getShutdownBean());
                Method shutdownMethod = shutdownBean.getClass().getMethod(getShutdownMethod(), new Class[]{});
                shutdownMethod.invoke(shutdownBean, new Object[]{});
            } catch (Throwable e) {
                LOG.error("Unable to execute custom shutdown call", e);
            }
        }
        super.doClose();
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

    /**
     * Sets the type of standard Broadleaf context locations that should be merged. For possible values see
     * {@link StandardConfigLocations#APPCONTEXTTYPE}
     */
    public void setStandardLocationTypes(int standardLocationTypes) {
        this.standardLocationTypes = standardLocationTypes;
    }

    /**
     * @return the shutdownBean
     */
    public String getShutdownBean() {
        return shutdownBean;
    }

    /**
     * @param shutdownBean the shutdownBean to set
     */
    public void setShutdownBean(String shutdownBean) {
        this.shutdownBean = shutdownBean;
    }

    /**
     * @return the shutdownMethod
     */
    public String getShutdownMethod() {
        return shutdownMethod;
    }

    /**
     * @param shutdownMethod the shutdownMethod to set
     */
    public void setShutdownMethod(String shutdownMethod) {
        this.shutdownMethod = shutdownMethod;
    }

    @Override
    protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        try {
            loadBeanDefinitions(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.postProcessBeanFactory(beanFactory);
    }


}
