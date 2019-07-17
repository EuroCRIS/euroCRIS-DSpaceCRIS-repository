/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.util;

import com.lyncode.xoai.dataprovider.xml.xoai.Element;
import com.lyncode.xoai.dataprovider.xml.xoai.Element.Field;
import com.lyncode.xoai.dataprovider.xml.xoai.Metadata;
import com.lyncode.xoai.util.Base64Utils;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.dspace.app.cris.integration.CRISAuthority;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.util.FirstNames;
import org.dspace.app.cris.util.MetadatumAuthorityDecorator;
import org.dspace.app.cris.util.UtilsCrisMetadata;
import org.dspace.app.util.MetadataExposure;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Metadatum;
import org.dspace.content.Item;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.ChoiceAuthorityManager;
import org.dspace.content.authority.Choices;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.utils.DSpace;
import org.dspace.xoai.data.DSpaceItem;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author Lyncode Development Team <dspace@lyncode.com>
 */
public class ItemUtils
{
    private static Logger log = LogManager
            .getLogger(ItemUtils.class);
    
    public static Integer MAX_DEEP = 2;

    private static Element getElement(List<Element> list, String name)
    {
        for (Element e : list)
            if (name.equals(e.getName()))
                return e;

        return null;
    }
    private static Element create(String name)
    {
        Element e = new Element();
        e.setName(name);
        return e;
    }

    private static Element.Field createValue(
            String name, String value)
    {
        Element.Field e = new Element.Field();
        e.setValue(value);
        e.setName(name);
        return e;
    }
    
    private static Field getField(List<Field> list, String name)
    {
        for (Field f : list)
            if (name.equals(f.getName()))
                return f;

        return null;
    }
    
    private static Element writeMetadata(Element  schema,Metadatum val) {
    	return writeMetadata(schema, val, null, null, null, false);
    }
    
    /***
     * Write metadata into a Element structure.
     * Add id and group identifier to group metadata values.
     * 
     * @param schema The reference schema
     * @param val The metadata value
     * @param group the group name (usually a relation metadata)
     * @param id The grouping id
     * @param relid The relation id
     * @param allowMultipleValue
     * @return
     */
    private static Element writeMetadata(Element  schema,Metadatum val, String group, String id, String relid, boolean allowMultipleValue) {
    	
        Element valueElem = null;
        valueElem = schema;

        // Has element.. with XOAI one could have only schema and value
        if (val.element != null && !val.element.equals(""))
        {
            Element element = getElement(schema.getElement(),
                    val.element);
            if (element == null)
            {
                element = create(val.element);
                schema.getElement().add(element);
            }
            valueElem = element;

            // Qualified element?
            if (val.qualifier != null && !val.qualifier.equals(""))
            {
                Element qualifier = getElement(element.getElement(),
                        val.qualifier);
                if (qualifier == null)
                {
                    qualifier = create(val.qualifier);
                    element.getElement().add(qualifier);
                }
                valueElem = qualifier;
            }
        }
        Element qualifier = valueElem;
        
        // Language?
        if (val.language != null && !val.language.equals(""))
        {
            Element language = getElement(valueElem.getElement(),
                    val.language);
            if (language == null || allowMultipleValue)
            {
                language = create(val.language);
                valueElem.getElement().add(language);
            }
            valueElem = language;
        }
        else
        {
            Element language = getElement(valueElem.getElement(),
                    "none");
            if (language == null || allowMultipleValue)
            {
                language = create("none");
                valueElem.getElement().add(language);
            }
            valueElem = language;
        }
    	
        valueElem.getField().add(createValue("value", val.value));
        if (val.authority != null) {
            valueElem.getField().add(createValue("authority", val.authority));
            if (val.confidence != Choices.CF_NOVALUE)
                valueElem.getField().add(createValue("confidence", val.confidence + ""));
        }
        if (id != null && group != null && relid != null) {
        	valueElem.getField().add(createValue("id", id));
        	valueElem.getField().add(createValue("group", group));
        	valueElem.getField().add(createValue("relid", relid));
        }
        return valueElem;

    }
    public static Metadata retrieveMetadata (Context context, Item item) {
    	return retrieveMetadata(context, item, false, null, null, null, true);
    }
    
    /***
     * Retrieve all metadata in a XML fragment.
     * 
     * Group and id and relid are used to group metadata of an item, or relations, inside xsl. 
     * 
     * @param context The context
     * @param item The cris item
     * @param skipAutority is used to disable relation metadata inclusion.
     * @param group The group name
     * @param id The id
     * @param relid The relation id
     * @param allowMultipleValue is used to enabled metadata with multiple value
     * @return
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
	public static Metadata retrieveMetadata (Context context, Item item, boolean skipAutority, String group, String id, String relid, boolean allowMultipleValue) {
        Metadata metadata;
        
        // read all metadata into Metadata Object
        metadata = new Metadata();
        
        Metadatum[] vals = item.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
        // add defaults
        {
        	Metadatum[] defaults = new Metadatum[1];
        	Metadatum metadatum = new Metadatum();
        	
        	metadatum.schema = "item";
        	metadatum.element = "vprop";
        	metadatum.qualifier = "id";
        	metadatum.authority = null;
        	metadatum.value = Integer.toString(item.getID());
        	defaults[0] = metadatum;
        	
        	vals = ArrayUtils.addAll(vals, defaults);
        }
        Map<String, Element> root_indexed = new HashMap<String, Element>();
        for (Metadatum val : vals)
        {
            // Don't expose fields that are hidden by configuration
            try {
                if (MetadataExposure.isHidden(context,
                        val.schema,
                        val.element,
                        val.qualifier))
                {
                    continue;
                }
            } catch(SQLException se) {
                throw new RuntimeException(se);
            }

            // mapping metadata in index only            
            Element schema = getElement(metadata.getElement(), val.schema);

            if (schema == null)
            {
                schema = create(val.schema);
                metadata.getElement().add(schema);
            }
            Element element = writeMetadata(schema, val, group, id, relid, allowMultipleValue);
            //metadata.getElement().add(element);
            
            // use original value for relation
            if (!skipAutority && val.authority != null) {
            	String m = val.schema + "." + val.element;
            	
                if (val.qualifier != null && !val.qualifier.equals("")) {
                	m += "." + val.qualifier;
                }
                // add metadata of related cris object, using authority to get it
                boolean metadataAuth = ConfigurationManager.getBooleanProperty("oai", "oai.authority." + m);
                if (metadataAuth) {
                	try {
                		ChoiceAuthorityManager choicheAuthManager = ChoiceAuthorityManager.getManager();
                		ChoiceAuthority choicheAuth = choicheAuthManager.getChoiceAuthority(m);
                		Element root_idx = root_indexed.get(m);
                		if (choicheAuth != null && choicheAuth instanceof CRISAuthority) {
							CRISAuthority crisAuthoriy = (CRISAuthority) choicheAuth;
							ACrisObject o = getApplicationService().getEntityByCrisId(val.authority, crisAuthoriy.getCRISTargetClass());
                			
                			Metadata crisMetadata = retrieveMetadata(context, o, skipAutority, m, ((ACrisObject) o).getUuid(), Integer.toString(item.getID()), 0);
                			if (crisMetadata != null && !crisMetadata.getElement().isEmpty()) {
                				// optimize element generation using only one root
                				if (root_idx == null) {
	                				Element root = create(m);
	                				metadata.getElement().add(root);
	                				root_indexed.put(m, root);
	                				
	                				for (Element crisElement : crisMetadata.getElement()) {
	                					root.getElement().add(crisElement);
	                				}
                				}
                				else {
                    				// schema, remap elements
                    				remap(root_idx, crisMetadata.getElement());
                				}
                			}
                		} else {
                			log.warn("No choices plugin (CRISAuthority plugin) was configured for field " + m);
                		}
            		} catch (Exception e) {
            			log.error("Error during retrieving choices plugin (CRISAuthority plugin) for field " + m + ". " + e.getMessage(), e);
            		}
                }
            }
        }

        // Done! Metadata has been read!
        // Now adding bitstream info
        Element bundles = create("bundles");
        metadata.getElement().add(bundles);

        Bundle[] bs;
        try
        {
            bs = item.getBundles();
            for (Bundle b : bs)
            {
                Element bundle = create("bundle");
                bundles.getElement().add(bundle);
                bundle.getField()
                        .add(createValue("name", b.getName()));

                Element bitstreams = create("bitstreams");
                bundle.getElement().add(bitstreams);
                Bitstream[] bits = b.getBitstreams();
                for (Bitstream bit : bits)
                {
                    Element bitstream = create("bitstream");
                    bitstreams.getElement().add(bitstream);
                    
                    Metadatum[] bVals = bit.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
                    for(Metadatum bVal : bVals) {
                        Element bSchema = getElement(bitstream.getElement(), bVal.schema);
                        if (bSchema == null)
                        {
                            bSchema = create(bVal.schema);
                            bitstream.getElement().add(bSchema);
                        }
                    	Element bElement = writeMetadata(bSchema, bVal);
                        bitstream.getElement().add(bElement);
                    }
                    
                    String url = "";
                    String bsName = bit.getName();
                    String bitID= Integer.toString(bit.getID());
                    String sid = String.valueOf(bit.getSequenceID());
                    String baseUrl = ConfigurationManager.getProperty("oai",
                            "bitstream.baseUrl");
                    String handle = null;
                    // get handle of parent Item of this bitstream, if there
                    // is one:
                    Bundle[] bn = bit.getBundles();
                    if (bn.length > 0)
                    {
                        Item bi[] = bn[0].getItems();
                        if (bi.length > 0)
                        {
                            handle = bi[0].getHandle();
                        }
                    }
                    if (bsName == null)
                    {
                        String ext[] = bit.getFormat().getExtensions();
                        bsName = "bitstream_" + sid
                                + (ext.length > 0 ? ext[0] : "");
                    }
                    if (handle != null && baseUrl != null)
                    {
                        url = baseUrl + "/bitstream/"
                                + handle + "/"
                                + sid + "/"
                                + URLUtils.encode(bsName);
                    }
                    else
                    {
                        url = URLUtils.encode(bsName);
                    }

                    String cks = bit.getChecksum();
                    String cka = bit.getChecksumAlgorithm();
                    String oname = bit.getSource();
                    String name = bit.getName();
                    String description = bit.getDescription();

                    bitstream.getField().add(createValue("id", bitID));
                    if (name != null)
                        bitstream.getField().add(
                                createValue("name", name));
                    if (oname != null)
                        bitstream.getField().add(
                                createValue("originalName", name));
                    if (description != null)
                        bitstream.getField().add(
                                createValue("description", description));
                    bitstream.getField().add(
                            createValue("format", bit.getFormat()
                                    .getMIMEType()));
                    bitstream.getField().add(
                            createValue("size", "" + bit.getSize()));
                    bitstream.getField().add(createValue("url", url));
                    bitstream.getField().add(
                            createValue("checksum", cks));
                    bitstream.getField().add(
                            createValue("checksumAlgorithm", cka));
                    bitstream.getField().add(
                            createValue("sid", bit.getSequenceID()
                                    + ""));
                }
            }
        }
        catch (SQLException e1)
        {
            e1.printStackTrace();
        }
        

        // Other info
        Element other = create("others");

        other.getField().add(
                createValue("handle", item.getHandle()));
        other.getField().add(
                createValue("identifier", DSpaceItem.buildIdentifier(item.getHandle())));
        other.getField().add(
                createValue("lastModifyDate", item
                        .getLastModified().toString()));
        other.getField().add(
                createValue("type", "item"));
        metadata.getElement().add(other);

        // Repository Info
        Element repository = create("repository");
        repository.getField().add(
                createValue("name",
                        ConfigurationManager.getProperty("dspace.name")));
        repository.getField().add(
                createValue("mail",
                        ConfigurationManager.getProperty("mail.admin")));
        metadata.getElement().add(repository);

        // Licensing info
        Element license = create("license");
        Bundle[] licBundles;
        try
        {
            licBundles = item.getBundles(Constants.LICENSE_BUNDLE_NAME);
            if (licBundles.length > 0)
            {
                Bundle licBundle = licBundles[0];
                Bitstream[] licBits = licBundle.getBitstreams();
                if (licBits.length > 0)
                {
                    Bitstream licBit = licBits[0];
                    InputStream in;
                    try
                    {
                        in = licBit.retrieve();
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        Utils.bufferedCopy(in, out);
                        license.getField().add(
                                createValue("bin",
                                        Base64Utils.encode(out.toString())));
                        metadata.getElement().add(license);
                    }
                    catch (AuthorizeException e)
                    {
                        log.warn(e.getMessage(), e);
                    }
                    catch (IOException e)
                    {
                        log.warn(e.getMessage(), e);
                    }
                    catch (SQLException e)
                    {
                        log.warn(e.getMessage(), e);
                    }

                }
            }
        }
        catch (SQLException e1)
        {
            log.warn(e1.getMessage(), e1);
        }
        
        return metadata;
    }
    
    /***
     * Map XML fragment of elements in an given root element.
     * 
     * @param root_idx The root element
     * @param elements The list of elements to remap
     */
    private static void remap(Element root_idx, List<Element> elements) {
    	Element father = root_idx;
    	Element remapFather = null;		// to avoid concurrent modification exception
    	
		for (Element s: elements) {
			if (remapFather != null) {
				father.getElement().add(remapFather);
				remapFather = null;
			}
			
			Element idx_schema = getElement(father.getElement(), s.getName());
			if (idx_schema == null) {
				father.getElement().add(s);
			}
			else if (idx_schema.getElement().isEmpty()) {
				// check the value
				boolean remapValue = true;
				if (!idx_schema.getField().isEmpty() && idx_schema.getField().size() == s.getField().size()) {
					remapValue = false;
					for (Field idxf : idx_schema.getField()) {
						Field sf = getField(s.getField(), idxf.getName());
						
						if (sf == null || (sf.getValue() != null && !sf.getValue().equals(idxf.getValue()) || (sf.getValue() == null && sf.getValue() != idxf.getValue()))) {
							remapValue = true;
							break;
						}
					}
		        }
    			if (remapValue)
    				remapFather = s;
			 }
			 else {
				remap(idx_schema, s.getElement());
			 }
		}
		
		if (remapFather != null) {
			father.getElement().add(remapFather);
			remapFather = null;
		}	
    }
    
    @SuppressWarnings("rawtypes")
	public static Metadata retrieveMetadata (Context context, ACrisObject item) {
    	return retrieveMetadata(context, item, false, null, null, null, 0);
    }
    
    /***
     * Retrieve all metadata in a XML fragment.
     * 
     * Group and id are used to group metadata of an item, or relation, inside xsl. 
     * 
     * @param context The context
     * @param item The cris item
     * @param group The group name
     * @param id The id
     * @param relid The relation id
     * @param deep 
     * @return
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
	public static Metadata retrieveMetadata (Context context, ACrisObject item, boolean skipAutority, String group, String id, String relid, int deep) {
        Metadata metadata;
        
        // read all metadata into Metadata Object
        metadata = new Metadata();
        
		MetadatumAuthorityDecorator[] vals = ItemUtils.getAllMetadata(item, true, true, "oai");
        if (vals != null)
        {
        	Map<String, Element> root_indexed = new HashMap<String, Element>();
            for (MetadatumAuthorityDecorator valAuthDec : vals)
            {
            	Metadatum val = valAuthDec.getMetadatum();

            	// mapping metadata in index only
                Element schema = getElement(metadata.getElement(), val.schema);
                
                if (schema == null)
                {
                    schema = create(val.schema);
                    metadata.getElement().add(schema);
                }
                Element element = writeMetadata(schema, val, group, id, relid, true);
                //metadata.getElement().add(element);
                
                // create relation (use full metadata value as relation name)
                if (!skipAutority && val.authority != null && val.authority.trim().length() > 0) {
                	String m = val.schema + "." + val.element;
                	
                    if (val.qualifier != null && !val.qualifier.equals("")) {
                    	m += "." + val.qualifier;
                    }          
                    // compute deep
                    int authorityDeep = ConfigurationManager.getIntProperty("oai", "oai.authority." + m + ".deep");
                    if (authorityDeep <= 0) {
                    	authorityDeep = ConfigurationManager.getIntProperty("oai", "oai.authority.deep");
                    	
                    	if (authorityDeep <= 0)
                    		authorityDeep = MAX_DEEP;
                    }
                    
                    // add metadata of related cris object, using authority to get it
                    boolean metadataAuth = ConfigurationManager.getBooleanProperty("oai", "oai.authority." + m);
	                if (metadataAuth && (deep < authorityDeep) && (!valAuthDec.isClassNameNull() || !valAuthDec.isClassNameNull(val.authority))) {
	                	try {
	                		ACrisObject o = null;
	                		Element root_idx = root_indexed.get(m);
	                		
	                		if (!valAuthDec.isClassNameNull())
	                			o = getApplicationService().getEntityByCrisId(val.authority, valAuthDec.className());
	                		else
	                			o = getApplicationService().getEntityByCrisId(val.authority, valAuthDec.className(val.authority));
	                			
                			Metadata crisMetadata = retrieveMetadata(context, o, skipAutority, m, ((ACrisObject) o).getUuid(), item.getUuid(), deep + 1);
                			if (crisMetadata != null && !crisMetadata.getElement().isEmpty()) {
                				// optimize element generation using only one root
                				if (root_idx == null) {
	                				Element root = create(m);
	                				metadata.getElement().add(root);
	                				root_indexed.put(m, root);
                				
	                				for (Element crisElement : crisMetadata.getElement()) {
	                					root.getElement().add(crisElement);
	                				}
                				}
                				else {
                    				// schema, remap elements
                    				remap(root_idx, crisMetadata.getElement());
                				}
                			}		
	            		} catch (Exception e) {
	            			log.error("Error during retrieving choices plugin (CRISAuthority plugin) for field " + m + ". " + e.getMessage(), e);
	            		}
	                }
                }
            }
        }

        // Other info
        Element other = create("others");

        other.getField().add(
                createValue("handle", item.getHandle()));
        other.getField().add(
                createValue("identifier", DSpaceItem.buildIdentifier(item.getHandle())));
        Date m = new Date(item
                .getTimeStampInfo().getLastModificationTime().getTime());
        other.getField().add(
                createValue("lastModifyDate", m.toString()));
        other.getField().add(
                createValue("type", item.getPublicPath()));
        metadata.getElement().add(other);

        // Repository Info
        Element repository = create("repository");
        repository.getField().add(
                createValue("name",
                        ConfigurationManager.getProperty("dspace.name")));
        repository.getField().add(
                createValue("mail",
                        ConfigurationManager.getProperty("mail.admin")));
        metadata.getElement().add(repository);
        
        return metadata;
    }
    
    public static String DEFAULT_SCHEMA_NAME = "crisitem";
 	public static String DEFAULT_ELEMENT_NAME = "crisprop";
	public static String VIRTUAL_ELEMENT_NAME = "crisvprop";
    
    /***
     * Read all metadata of a cris object
     * 
     * Added mapping of virtual
     * 
     * @param item The cris item
     * @param onlyPub Set to true to read only public property
     * @param filterProperty Set to true to enable property filtering
     * @param module The config file name (module of config)
     * @return
     */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static MetadatumAuthorityDecorator[] getAllMetadata(ACrisObject item, boolean onlyPub, boolean filterProperty, String module) {
    	List<MetadatumAuthorityDecorator> results = new ArrayList<MetadatumAuthorityDecorator>();
    	
    	MetadatumAuthorityDecorator[] vals = UtilsCrisMetadata.getAllMetadata(item, onlyPub, filterProperty, module);
    	for (MetadatumAuthorityDecorator mad : vals) {
    		results.add(mad);
    	}
    	
    	// add default virtual
    	{
    		Metadatum metadatum = new Metadatum();
    		
    	    //crisitem.crisvprop.id        	(the id of the cris item)
    		metadatum.schema = DEFAULT_SCHEMA_NAME;
    		metadatum.element = VIRTUAL_ELEMENT_NAME;
    		metadatum.qualifier = "id";
    		metadatum.language = null;
    		
    		metadatum.authority = null;
    		metadatum.value = Integer.toString(item.getID());
    		results.add(new MetadatumAuthorityDecorator(metadatum.copy()));

    	    //crisitem.crisvprop.uuid      	(the uuid of the cris item)
    		metadatum.schema = DEFAULT_SCHEMA_NAME;
    		metadatum.element = VIRTUAL_ELEMENT_NAME;
    		metadatum.qualifier = "uuid";
    		metadatum.language = null;
    		
    		metadatum.authority = null;
    		metadatum.value = item.getUuid();
    		results.add(new MetadatumAuthorityDecorator(metadatum.copy()));
    		
    	    //crisitem.crisvprop.handle    	(the handle of the cris item)
    		metadatum.schema = DEFAULT_SCHEMA_NAME;
    		metadatum.element = VIRTUAL_ELEMENT_NAME;
    		metadatum.qualifier = "handle";
    		metadatum.language = null;
    		
    		metadatum.authority = null;
    		metadatum.value = item.getHandle();
    		results.add(new MetadatumAuthorityDecorator(metadatum.copy()));
    		
    		// crisitem.crisprop.objecttype
            metadatum.schema = DEFAULT_SCHEMA_NAME;
            metadatum.element = VIRTUAL_ELEMENT_NAME;
            metadatum.qualifier = "objecttype";
            metadatum.language = null;

            metadatum.authority = null;
            metadatum.value = item.getPublicPath();
            results.add(new MetadatumAuthorityDecorator(metadatum.copy()));
    	}
    	
    	// crisitem.crisvprop.fullname
    	Metadatum metadatum = new Metadatum();
    	MetadataMapper mapper = new MetadataMapper(module);
    	Metadatum fullName = mapper.map(DEFAULT_SCHEMA_NAME + "." + VIRTUAL_ELEMENT_NAME + ".fullname");
    	
    	for (MetadatumAuthorityDecorator mad : results) {	    		
    		Metadatum m = mad.getMetadatum();

    		if (m.schema != null && m.schema.equals(fullName.schema) 
    				&& m.element != null && m.element.equals(fullName.element) 
    				&& (m.qualifier == null || m.qualifier.trim().length() <= 0 || m.qualifier.equals(fullName.qualifier))) {
    			String firstName = null;
    			String familyName = null;

    			m.value = m.value.trim();
    			if (StringUtils.countMatches(m.value, ",") == 1) {
    				String[] t = m.value.split(",");

    				firstName = t[1].trim();
    				familyName = t[0].trim();
    			}
    			else {
    				firstName = FirstNames.getInstance(module).getFirstName(m.value);
    				familyName = m.value.substring((firstName != null) ? firstName.length() : 0).trim();
    			}
    			// crisitem.crisprop.firstname
    			metadatum.schema = DEFAULT_SCHEMA_NAME;
    			metadatum.element = VIRTUAL_ELEMENT_NAME;
    			metadatum.qualifier = "firstname";
    			metadatum.language = null;

    			metadatum.authority = null;
    			metadatum.value = firstName;
    			results.add(new MetadatumAuthorityDecorator(metadatum.copy()));

    			// crisitem.crisprop.familyname
    			metadatum.schema = DEFAULT_SCHEMA_NAME;
    			metadatum.element = VIRTUAL_ELEMENT_NAME;
    			metadatum.qualifier = "familyname";
    			metadatum.language = null;

    			metadatum.authority = null;
    			metadatum.value = familyName;
    			results.add(new MetadatumAuthorityDecorator(metadatum.copy()));
    			break;
    		}
        }
    	
    	// fixed values
    	{
    		//MetadataMapper mapper = new MetadataMapper(module);
    		List<Metadatum> fixedValues = mapper.fixedValues(item.getPublicPath());
    		for (Metadatum m : fixedValues) {
    			// add fixed value
    			results.add(new MetadatumAuthorityDecorator(m));
    		}
    	}

    	return results.toArray(new MetadatumAuthorityDecorator[results.size()]);
    }
    
    /***
     * Cris application service
     * @return
     */
    private static ApplicationService getApplicationService()
    {
    	return new DSpace().getServiceManager().getServiceByName(
    			"applicationService", ApplicationService.class);
    }
}
