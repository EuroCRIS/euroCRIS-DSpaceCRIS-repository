/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.jsptag;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

import org.dspace.app.util.CollectionDropDown;
import org.dspace.app.util.CollectionUtils;
import org.dspace.app.util.CollectionsTree;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Renders select element to select collection with parent community
 * object.
 * 
 * @author Keiji Suzuki
 */
public class SelectCollectionTag extends TagSupport
{

    /** the class description */
    private String klass;

    /** the name description */
    private String name;

    /** the id description */
    private String id;

    /** the collection id */
    private String collection = null;

    private final transient ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    public SelectCollectionTag()
    {
        super();
    }

    public int doStartTag() throws JspException
    {
        JspWriter out = pageContext.getOut();
        StringBuffer sb = new StringBuffer();

        try
        {
            HttpServletRequest hrq = (HttpServletRequest) pageContext.getRequest();
            Context context = UIUtil.obtainContext(hrq);
            List<Collection> collections = (List<Collection>) hrq.getAttribute("collections");
            
            CollectionsTree tree = CollectionUtils.getCollectionsTree(context, collections, false);

            sb.append("<select");
            if (name != null)
            {
                sb.append(" name=\"").append(name).append("\"");
            }
            if (klass != null)
            {
                sb.append(" class=\"").append(klass).append("\"");
            }
            if (id != null)
            {
                sb.append(" id=\"").append(id).append("\"");
            }
            sb.append(">\n");

            ResourceBundle msgs = ResourceBundle.getBundle("Messages", context.getCurrentLocale());
            String firstOption = msgs.getString("jsp.submit.start-lookup-submission.select.collection.defaultoption");
         
            
            sb.append("<option value=\"-1\"");
            if (collection == null) sb.append(" selected=\"selected\"");
            sb.append(">").append(firstOption).append("</option>\n");

            out.print(sb.toString());
            if (!configurationService
                    .getBooleanProperty("webui.collection.dropdown.default"))
            {
                collectionSelect(out, tree);
            }
            else
            {
                for (Collection coll : collections)
                {
                    sb.append("<option value=\"").append(coll.getID())
                            .append("\"");
                    if (collection.equals(coll.getID().toString()))
                    {
                        sb.append(" selected=\"selected\"");
                    }
                    sb.append(">").append(
                            CollectionDropDown.collectionPath(context, coll))
                            .append("</option>\n");
                }
            }
            sb.append("</select>\n");

        }
        catch (IOException e)
        {
            throw new JspException(e);
        }
        catch (SQLException e)
        {
            throw new JspException(e);
        }
        
        return SKIP_BODY;
    }
    
    
	private void collectionSelect(JspWriter out, CollectionsTree tree ) throws IOException 
	{
		
		if(tree==null){
			return;
		}
		if (tree.getCurrent() != null)
		{
			out.print("<optgroup label=\""+tree.getCurrent().getName()+"\">");
		}
		if (tree.getCollections() != null){
			for (Collection col : tree.getCollections())
			{
				String selected= col.getID()==UUID.fromString(collection) ? "selected":"";
				out.print("<option value=\""+col.getID()+"\" "+ selected + ">"+col.getName()+"</option>");	
			}
		}
		if (tree.getSubTree() != null)
		{
			for (CollectionsTree subTree: tree.getSubTree())
			{
				collectionSelect(out, subTree);
			}
		}
		if (tree.getCurrent() != null)
		{
			out.print("</optgroup>");
		}
		
	}    

    public String getKlass()
    {
        return klass;
    }

    public void setKlass(String klass)
    {
        this.klass = klass;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getCollection()
    {
        return collection;
    }

    public void setCollection(String collection)
    {
        this.collection = collection;
    }


    public void release()
    {
        klass = null;
        name = null;
        id = null;
        collection = null;
    }
}

