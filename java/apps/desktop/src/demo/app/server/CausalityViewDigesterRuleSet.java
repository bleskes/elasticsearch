package demo.app.server;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.RuleSetBase;

/**
 * Apache Commons Digester RuleSet, defining rules for parsing the
 * XML configuration of Causality Views.
 * @author Pete Harverson
 */
public class CausalityViewDigesterRuleSet extends RuleSetBase
{

	private String m_Prefix;
	
	/**
	 * Creates a new Rule Set for Causality Views, with 'causalityView' to be
	 * used as the prefix of the matching pattern.
	 */
	public CausalityViewDigesterRuleSet()
	{
		this("causalityView");
	}


	/**
	 * Creates a new Rule Set for Causality Views, with the specified String to be
	 * used as the leading portion of the matching pattern.
	 * @param prefix leading portion for the matching pattern.
	 */
	public CausalityViewDigesterRuleSet(String prefix)
	{
		super();
		m_Prefix = prefix;
	}
	

	/**
	 * Add the set of Rule instances defined in this RuleSet for Causality Views to the 
	 * specified Digester instance.
	 * This method should only be called by a Digester instance. 
	 * @param digester Digester instance to which the new Rule instances should be added.
	 */
	public void addRuleInstances(Digester digester)
	{
		digester.addObjectCreate(m_Prefix, "demo.app.data.CausalityView");
		
		digester.addCallMethod(m_Prefix + "/name", "setName", 0);
		digester.addCallMethod(m_Prefix + "/styleId", "setStyleId", 0);
		digester.addBeanPropertySetter(m_Prefix + "/desktopShortcut");
		digester.addBeanPropertySetter(m_Prefix + "/displayAsEpisodes");
		
		// Parse the context menu items.
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
	}

}
