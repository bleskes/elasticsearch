/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2009     *
 *                                                          *
 *----------------------------------------------------------*
 *----------------------------------------------------------*
 * WARNING:                                                 *
 * THIS FILE CONTAINS UNPUBLISHED PROPRIETARY               *
 * SOURCE CODE WHICH IS THE PROPERTY OF PRELERT LTD AND     *
 * PARENT OR SUBSIDIARY COMPANIES.                          *
 * PLEASE READ THE FOLLOWING AND TAKE CAREFUL NOTE:         *
 *                                                          *
 * This source code is confidential and any person who      *
 * receives a copy of it, or believes that they are viewing *
 * it without permission is asked to notify Prelert Ltd     *
 * on +44 (0)20 3567 1249 or email to legal@prelert.com.    *
 * All intellectual property rights in this source code     *
 * are owned by Prelert Ltd.  No part of this source code   *
 * may be reproduced, adapted or transmitted in any form or *
 * by any means, electronic, mechanical, photocopying,      *
 * recording or otherwise.                                  *
 *                                                          *
 *----------------------------------------------------------*
 *                                                          *
 *                                                          *
 ***********************************************************/

package demo.app.server;

import org.apache.commons.digester.Digester;


/**
 * Apache Commons Digester RuleSet, defining rules for parsing the
 * XML configuration of Exception Views.
 * @author Pete Harverson
 */
public class ExceptionViewDigesterRuleSet extends ListViewDigesterRuleSet
{
	/**
	 * Creates a new Rule Set for Exception Views, with 'exceptionView' to be
	 * used as the prefix of the matching pattern.
	 */
	public ExceptionViewDigesterRuleSet()
	{
		this("exceptionView");
	}


	/**
	 * Creates a new Rule Set for Exception Views, with the specified String to be
	 * used as the leading portion of the matching pattern.
	 * @param prefix leading portion for the matching pattern.
	 */
	public ExceptionViewDigesterRuleSet(String prefix)
	{
		super(prefix);
	}
	
	
	/**
	 * Add the set of Rule instances defined in this RuleSet for Exception Views to the 
	 * specified Digester instance.
	 * This method should only be called by a Digester instance. 
	 * @param digester Digester instance to which the new Rule instances should be added.
	 */
	public void addRuleInstances(Digester digester)
	{
		super.addRuleInstances(digester, "demo.app.data.ExceptionView");
		
		digester.addCallMethod(getPrefix() + "/timeWindow", "setTimeWindow", 0);
		digester.addBeanPropertySetter(getPrefix() + "/noiseLevel");
	}
}
