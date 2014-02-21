package demo.app.server;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.RuleSetBase;

/**
 * Apache Commons Digester RuleSet, defining rules for parsing the
 * XML configuration of generic List Views.
 * @author Pete Harverson
 */
public class ListViewDigesterRuleSet extends RuleSetBase
{
	private String m_Prefix;
	
	/**
	 * Creates a new Rule Set for List Views, with 'listView' to be
	 * used as the prefix of the matching pattern.
	 */
	public ListViewDigesterRuleSet()
	{
		this("listView");
	}


	/**
	 * Creates a new Rule Set for List Views, with the specified String to be
	 * used as the leading portion of the matching pattern.
	 * @param prefix leading portion for the matching pattern.
	 */
	public ListViewDigesterRuleSet(String prefix)
	{
		super();
		m_Prefix = prefix;
	}
	
	
	/**
	 * Sets the prefix to be used as the leading portion of the matching pattern
	 * when matching on property names.
	 * @param prefix leading portion for the matching pattern.
	 */
	public void setPrefix(String prefix)
	{
		m_Prefix = prefix;
	}
	
	
	/**
	 * Returns the prefix to be used as the leading portion of the matching pattern
	 * when matching on property names.
	 * @return leading portion for the matching pattern.
	 */
	public String getPrefix()
	{
		return m_Prefix;
	}
	
	
	/**
	 * Add the set of Rule instances defined in this RuleSet for List Views to the 
	 * specified Digester instance.
	 * This method should only be called by a Digester instance. 
	 * @param digester Digester instance to which the new Rule instances should be added.
	 */
	public void addRuleInstances(Digester digester)
	{
		addRuleInstances(digester, "demo.app.data.ListView");
	}
	
	
	/**
	 * Add the set of Rule instances defined in this RuleSet for List Views to the 
	 * specified Digester instance.
	 * This method should only be called by a Digester instance. 
	 * @param digester Digester instance to which the new Rule instances should be added.
	 * @param classToCreate name of Java class to be created.
	 */
	public void addRuleInstances(Digester digester, String classToCreate)
	{
		digester.addObjectCreate(m_Prefix, classToCreate);
		
		digester.addCallMethod(m_Prefix + "/name", "setName", 0);
		digester.addBeanPropertySetter(m_Prefix + "/dataType");
		digester.addCallMethod(m_Prefix + "/styleId", "setStyleId", 0);
		digester.addBeanPropertySetter(m_Prefix + "/desktopShortcut");
		digester.addCallMethod(m_Prefix + "/dbView", "setDatabaseView", 0);
		digester.addBeanPropertySetter(m_Prefix + "/filterAttribute");
		digester.addBeanPropertySetter(m_Prefix + "/filterValue");
		digester.addBeanPropertySetter(m_Prefix + "/doubleClickTool");
		
		digester.addObjectCreate(m_Prefix + "/defaultOrderBy", "java.util.ArrayList");
		digester.addSetNext(m_Prefix + "/defaultOrderBy", "setDefaultOrderBy", "java.util.ArrayList");	
		
		digester.addObjectCreate(m_Prefix + "/defaultOrderBy/sortInfo", "demo.app.data.SortInformation");
		digester.addSetNext(m_Prefix + "/defaultOrderBy/sortInfo", "add", "demo.app.data.SortInformation");	
		digester.addCallMethod(m_Prefix + "/defaultOrderBy/sortInfo/columnName", "setColumnName", 0);
		digester.addCallMethod(m_Prefix + "/defaultOrderBy/sortInfo/sortDirection", "setSortDirection", 0);
		
		// Parse the List View menu items.
		digester.addObjectCreate(m_Prefix + "/contextMenu", "java.util.ArrayList");
		digester.addSetNext(m_Prefix + "/contextMenu", "setContextMenuItems", "java.util.ArrayList");
		
		digester.addObjectCreate(m_Prefix + "/contextMenu/menuItem", 
									"demo.app.data.Tool", "toolClass");
		digester.addSetNext(m_Prefix + "/contextMenu/menuItem", "add", "demo.app.data.Tool");
		
		// Generic ViewTool properties.
		digester.addBeanPropertySetter(m_Prefix + "/contextMenu/menuItem/name");
		digester.addBeanPropertySetter(m_Prefix + "/contextMenu/menuItem/viewToOpen");
		
		// Properties for tools for opening List Views.
		digester.addBeanPropertySetter(m_Prefix + "/contextMenu/menuItem/filterAttribute");
		digester.addBeanPropertySetter(m_Prefix + "/contextMenu/menuItem/filterArg");
		
		// Properties for tools for opening Usage and History Views.
		digester.addBeanPropertySetter(m_Prefix + "/contextMenu/menuItem/metric");
		digester.addCallMethod(m_Prefix + "/contextMenu/menuItem/timeFrame", "setTimeFrame", 0);
		digester.addBeanPropertySetter(m_Prefix + "/contextMenu/menuItem/sourceArg");
		digester.addBeanPropertySetter(m_Prefix + "/contextMenu/menuItem/attributeName");
		digester.addBeanPropertySetter(m_Prefix + "/contextMenu/menuItem/attributeValueArg");
		digester.addBeanPropertySetter(m_Prefix + "/contextMenu/menuItem/timeArg");
		
		// Properties for tools for opening Show Info windows.
		digester.addCallMethod(m_Prefix + "/contextMenu/menuItem/activeAttribute", 
				"addActiveAttribute", 0);
	}

}
