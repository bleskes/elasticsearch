package com.prelert.server;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.RuleSetBase;


/**
 * Apache Commons Digester RuleSet, defining rules for parsing the
 * XML configuration of History Views.
 * @author Pete Harverson
 */
public class HistoryViewDigesterRuleSet extends RuleSetBase
{
	private String m_Prefix;
	
	
	/**
	 * Creates a new Rule Set for History Views, with 'historyView' to be
	 * used as the prefix of the matching pattern.
	 */
	public HistoryViewDigesterRuleSet()
	{
		this("historyView");
	}


	/**
	 * Creates a new Rule Set for History Views, with the specified String to be
	 * used as the leading portion of the matching pattern.
	 * @param prefix leading portion for the matching pattern.
	 */
	public HistoryViewDigesterRuleSet(String prefix)
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
		// Add rule set for digesting the nested evidence views.
		digester.addRuleSet(new EvidenceViewDigesterRuleSet(m_Prefix + "/evidenceView"));
		
		digester.addObjectCreate(m_Prefix, "com.prelert.data.HistoryView");
		digester.addSetNext(m_Prefix + "/evidenceView", "addEvidenceView", "com.prelert.data.EvidenceView");
		digester.addBeanPropertySetter(m_Prefix + "/name");
		digester.addBeanPropertySetter(m_Prefix + "/dataType");
		digester.addBeanPropertySetter(m_Prefix + "/styleId");
	}

}
