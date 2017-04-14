/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.components;

import org.apache.log4j.Logger;
import org.dspace.app.cris.discovery.CrisSearchService;
import org.dspace.app.cris.integration.statistics.IStatsComponent;
import org.dspace.app.webui.cris.components.statistics.CrisStatBitstreamTopObjectComponent;
import org.dspace.app.webui.cris.components.statistics.CrisStatTopObjectComponent;
import org.dspace.browse.BrowsableDSpaceObject;
import org.dspace.browse.BrowseDSpaceObject;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverResult;

public class ItemsConfigurerComponent extends
        AFacetedQueryConfigurerComponent<BrowseDSpaceObject>
{

    /** log4j logger */
    private static Logger log = Logger
            .getLogger(ItemsConfigurerComponent.class);

    @Override
    public BrowseDSpaceObject[] getObjectFromSolrResult(DiscoverResult docs,
            Context context) throws Exception
    {
    	BrowseDSpaceObject[] result = new BrowseDSpaceObject[docs.getDspaceObjects().size()];
        int i = 0;
        for (BrowsableDSpaceObject obj : docs.getDspaceObjects())
        {
            result[i] = new BrowseDSpaceObject(context, obj);
            i++;
        }
        return result;
    }
    
    @Override
    public IStatsComponent getStatsDownloadComponent()
    {
        CrisStatBitstreamTopObjectComponent component = new CrisStatBitstreamTopObjectComponent();

        BeanComponent bean = new BeanComponent();
        bean.setQuery(getRelationConfiguration().getQuery());
        for(String key : getTypes().keySet()) {
            bean.getSubQueries().put(key, getTypes().get(key).getFacetQuery());
        }
        component.setBean(bean);
        component.setTargetObjectClass(getTarget());
        component.setRelationObjectClass(getRelationObjectClass());
        component.setCrisSearchService((CrisSearchService)getSearchService());
        return component;

    }

    @Override
    public IStatsComponent getStatsViewComponent()
    {
        CrisStatTopObjectComponent component = new CrisStatTopObjectComponent();

        BeanComponent bean = new BeanComponent();
        bean.setQuery(getRelationConfiguration().getQuery());
        
        for(String key : getTypes().keySet()) {
            bean.getSubQueries().put(key, getTypes().get(key).getFacetQuery());
        }
        component.setBean(bean);
        component.setTargetObjectClass(getTarget());
        component.setRelationObjectClass(getRelationObjectClass());

        return component;
    }

    @Override
    public Class getRelationObjectClass()
    {
        return getRelationConfiguration().getRelationClass();
    }
    
    @Override
    public Integer getRelationObjectType()
    {     
       return Constants.ITEM;
    }

}
