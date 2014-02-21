package com.prelert.server;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.RuleSetBase;


/**
 * Apache Commons Digester RuleSet, defining rules for parsing the
 * XML configuration of Usage Views.
 * @author Pete Harverson
 */
public class UsageViewDigesterRuleSet extends RuleSetBase
{
	private String m_Prefix;
	
	
	/**
	 * Creates a new Rule Set for Usage Views, with 'usageView' to be
	 * used as the prefix of the matching pattern.
	 */
	public UsageViewDigesterRuleSet()
	{
		this("usageView");
	}


	/**
	 * Creates a new Rule Set for Usage Views, with the specified String to be
	 * used as the leading portion of the matching pattern.
	 * @param prefix leading portion for the matching pattern.
	 */
	public UsageViewDigesterRuleSet(String prefix)
	{
		super();
		m_Prefix = prefix;
	}
	
	
	/**
	 * Add the set of Rule instances defined in this RuleSet for Usage Views to the 
	 * specified Digester instance.
	 * This method should only be called by a Digester instance. 
	 * @param digester Digester instance to which the new Rule instances should be added.
	 */
	public void addRuleInstances(Digester digester)
	{
		// Rules for creating Usage Views.
		digester.addObjectCreate(m_Prefix, "com.prelert.data.UsageView");

		digester.addBeanPropertySetter(m_Prefix + "/name");
		digester.addBeanPropertySetter(m_Prefix + "/styleId");
		digester.addBeanPropertySetter(m_Prefix + "/desktopShortcut");
		digester.addBeanPropertySetter(m_Prefix + "/autoRefreshFrequency");
		digester.addBeanPropertySetter(m_Prefix + "/dataType");
		
		digester.addBeanPropertySetter(m_Prefix + "/labels/sourceField", "sourceFieldText");
		digester.addBeanPropertySetter(m_Prefix + "/labels/allSources", "allSourcesText");
		digester.addBeanPropertySetter(m_Prefix + "/labels/selectSource", "selectSourceText");
		digester.addBeanPropertySetter(m_Prefix + "/labels/userField", "userFieldText");
		digester.addBeanPropertySetter(m_Prefix + "/labels/allAttributeValues", "allAttributeValuesText");
		digester.addBeanPropertySetter(m_Prefix + "/labels/selectUser", "selectUserText");

		digester.addCallMethod(m_Prefix + "/attributes/attributeName", "addAttributeName", 0);
		digester.addCallMethod(m_Prefix + "/metrics/metricName", "addMetric", 0);
		
		// Parse the Usage View menu items.
		digester.addObjectCreate(m_Prefix + "/contextMenu", "java.util.ArrayList");
		digester.addSetNext(m_Prefix + "/contextMenu", "setContextMenuItems", "java.util.ArrayList");
		
		digester.addObjectCreate(m_Prefix + "/contextMenu/menuItem", 
									"com.prelert.data.Tool", "toolClass");
		digester.addSetNext(m_Prefix + "/contextMenu/menuItem", "add", "com.prelert.data.Tool");
		

		// Generic ViewTool properties.
		digester.addBeanPropertySetter(m_Prefix + "/contextMenu/menuItem/name");
		digester.addBeanPropertySetter(m_Prefix + "/contextMenu/menuItem/viewToOpen");
		
		// Properties for tools for opening Time Series Views.
		digester.addBeanPropertySetter(m_Prefix + "/contextMenu/menuItem/metric");
		digester.addCallMethod(m_Prefix + "/contextMenu/menuItem/timeFrame", "setTimeFrame", 0);
		digester.addBeanPropertySetter(m_Prefix + "/contextMenu/menuItem/sourceArg");
		digester.addBeanPropertySetter(m_Prefix + "/contextMenu/menuItem/attributeName");
		digester.addBeanPropertySetter(m_Prefix + "/contextMenu/menuItem/attributeValueArg");
		digester.addBeanPropertySetter(m_Prefix + "/contextMenu/menuItem/timeArg");
	}

}
