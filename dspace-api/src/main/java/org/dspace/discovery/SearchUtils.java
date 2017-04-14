/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.dspace.browse.BrowsableDSpaceObject;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationService;
import org.dspace.discovery.configuration.DiscoveryMostViewedConfiguration;
import org.dspace.discovery.configuration.DiscoveryRecentSubmissionsConfiguration;
import org.dspace.discovery.configuration.DiscoveryViewAndHighlightConfiguration;
import org.dspace.kernel.ServiceManager;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Util methods used by discovery
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class SearchUtils {
    /** Cached search service **/
    private static SearchService searchService;


    public static SearchService getSearchService()
    {
        if(searchService ==  null){
            org.dspace.kernel.ServiceManager manager = DSpaceServicesFactory.getInstance().getServiceManager();
            searchService = manager.getServiceByName(SearchService.class.getName(),SearchService.class);
        }
        return searchService;
    }

    public static DiscoveryConfiguration getDiscoveryConfiguration(){
        return getDiscoveryConfiguration(null);
    }

    public static DiscoveryConfiguration getDiscoveryConfiguration(BrowsableDSpaceObject dso){
        return getDiscoveryConfigurationByName(dso!=null?dso.getHandle():null);
    }

    public static DiscoveryConfiguration getDiscoveryConfigurationByName(
            String configurationName)
    {
        DiscoveryConfigurationService configurationService = getConfigurationService();

        DiscoveryConfiguration result = null;
        if(configurationName == null){
            result = configurationService.getMap().get("site");
        }else{
            result = configurationService.getMap().get(configurationName);
        }

        if(result == null){
            //No specific configuration, get the default one
            result = configurationService.getMap().get("default");
        }

        return result;
    }

    public static DiscoveryConfigurationService getConfigurationService() {
        ServiceManager manager = DSpaceServicesFactory.getInstance().getServiceManager();
        return manager.getServiceByName(DiscoveryConfigurationService.class.getName(), DiscoveryConfigurationService.class);
    }

    public static List<String> getIgnoredMetadataFields(int type)
    {
        return getConfigurationService().getToIgnoreMetadataFields().get(type);
    }

    /**
     * Method that retrieves a list of all the configuration objects from the given item
     * A configuration object can be returned for each parent community/collection
     * @param item the DSpace item
     * @return a list of configuration objects
     */
    public static List<DiscoveryConfiguration> getAllDiscoveryConfigurations(Item item) throws SQLException {
        Map<String, DiscoveryConfiguration> result = new HashMap<String, DiscoveryConfiguration>();

        List<Collection> collections = item.getCollections();
        for (Collection collection : collections) {
            DiscoveryConfiguration configuration = getDiscoveryConfiguration(collection);
            if(!result.containsKey(configuration.getId())){
                result.put(configuration.getId(), configuration);
            }
        }

        //Also add one for the default
        DiscoveryConfiguration configuration = getDiscoveryConfiguration(null);
        if(!result.containsKey(configuration.getId())){
            result.put(configuration.getId(), configuration);
        }
        
        //Add special dspacebasic discoveryConfiguration
        DiscoveryConfiguration configurationExtra = getDiscoveryConfigurationByName("dspacebasic");
        if(!result.containsKey(configurationExtra.getId())){
            result.put(configurationExtra.getId(), configurationExtra);
        }

        DiscoveryConfiguration configurationDSpaceObject = null;
        if("item".equals(item.getTypeText())) {
        	configurationDSpaceObject = getDiscoveryConfigurationByName("dspaceitem");	
        }
        else {
        	configurationDSpaceObject = getDiscoveryConfigurationByName("dspace"+item.getTypeText());
        }
        if(configurationDSpaceObject!=null) {
            if(!result.containsKey(configurationDSpaceObject.getId())){
                result.put(configurationDSpaceObject.getId(), configurationDSpaceObject);
            }        	
        }
        
        //Add special global discoveryConfiguration
        configurationExtra = getDiscoveryConfigurationByName(DiscoveryConfiguration.GLOBAL_CONFIGURATIONNAME);
        if(!result.containsKey(configurationExtra.getId())){
            result.put(configurationExtra.getId(), configurationExtra);
        }
        return Arrays.asList(result.values().toArray(new DiscoveryConfiguration[result.size()]));
    }

    
    public static DiscoveryViewAndHighlightConfiguration getDiscoveryViewAndHighlightConfigurationByName(
            String configurationName)
    {
        return DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName(configurationName, DiscoveryViewAndHighlightConfiguration.class);
    }
    
	public static DiscoveryConfiguration getGlobalConfiguration() {
		boolean globalConfiguration = false;
        DiscoveryConfiguration configuration = SearchUtils.getDiscoveryConfigurationByName(DiscoveryConfiguration.GLOBAL_CONFIGURATIONNAME);
        
        if(DiscoveryConfiguration.GLOBAL_CONFIGURATIONNAME.equals(configuration.getId())) {
        	globalConfiguration = true;
        }
		return globalConfiguration?configuration:null;
	}

	public static boolean isGlobalConfiguration(DiscoveryConfiguration configuration) {
		return StringUtils.equals(configuration.getId(), DiscoveryConfiguration.GLOBAL_CONFIGURATIONNAME);
	}

    public static DiscoveryRecentSubmissionsConfiguration getRecentSubmissionConfiguration(
            String configurationName)
    {
        return getDiscoveryConfigurationByName(configurationName).getRecentSubmissionConfiguration();
    }

    public static DiscoveryMostViewedConfiguration getMostViewedConfiguration(
            String configurationName)
    {
        return getDiscoveryConfigurationByName(configurationName).getMostViewConfiguration();
    }
}
