package demo.app.server;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.RuleSetBase;


/**
 * Apache Commons Digester RuleSet, defining rules for parsing the
 * XML configuration of Evidence Views.
 * @author Pete Harverson
 */
public class EvidenceViewDigesterRuleSet extends ListViewDigesterRuleSet
{
	
	/**
	 * Creates a new Rule Set for Evidence Views, with 'evidenceView' to be
	 * used as the prefix of the matching pattern.
	 */
	public EvidenceViewDigesterRuleSet()
	{
		this("evidenceView");
	}


	/**
	 * Creates a new Rule Set for Evidence Views, with the specified String to be
	 * used as the leading portion of the matching pattern.
	 * @param prefix leading portion for the matching pattern.
	 */
	public EvidenceViewDigesterRuleSet(String prefix)
	{
		super(prefix);
	}
	
	
	/**
	 * Add the set of Rule instances defined in this RuleSet for Evidence Views to the 
	 * specified Digester instance.
	 * This method should only be called by a Digester instance. 
	 * @param digester Digester instance to which the new Rule instances should be added.
	 */
	public void addRuleInstances(Digester digester)
	{
		super.addRuleInstances(digester, "demo.app.data.EvidenceView");
		
		digester.addCallMethod(getPrefix() + "/timeFrame", "setTimeFrame", 0);
	}

}
