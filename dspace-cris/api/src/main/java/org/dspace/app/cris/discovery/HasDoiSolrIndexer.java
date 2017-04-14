/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.discovery;

import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;
import org.dspace.discovery.SolrServiceIndexPlugin;

public class HasDoiSolrIndexer implements SolrServiceIndexPlugin {

	Logger log = Logger.getLogger(HasDoiSolrIndexer.class);

	@Override
	public void additionalIndex(Context context, DSpaceObject dso, SolrInputDocument document) {
		if (dso instanceof Item) {
			Item item = (Item) dso;
			List<MetadataValue> dois = item.getItemService().getMetadataByMetadataString(item, "dc.identifier.doi");
			if (dois != null && dois.size() > 0) {
				document.addField("hasDoi", true);
			}
		}

	}
}
