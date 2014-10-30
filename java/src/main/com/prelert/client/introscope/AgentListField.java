/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2012     *
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

package com.prelert.client.introscope;

import java.util.List;

import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.widget.Dialog;
import com.extjs.gxt.ui.client.widget.form.TriggerField;


/**
 * Extension of the GXT <code>TriggerField</code> to provide input of a list
 * of Introscope agents via the agent selection dialog.
 * @author Pete Harverson
 */
public class AgentListField extends TriggerField<String>
{
	private AgentSelectionDialog	m_AgentSelector;
	
	
	/**
	 * Creates a new Introscope Agent List trigger field, with no limit on the
	 * number of agents that can be selected for analysis.
	 */
	public AgentListField()
	{
		this(-1);
	}
	
	
	/**
	 * Creates a new Introscope Agent List trigger field.
	 * @param maxSelect the maximum number of agents that may be selected for
	 * 	analysis, or <code>-1</code> to allow unlimited selection.
	 */
	public AgentListField(int maxSelect)
	{
		setTriggerStyle("prl-list-trigger");
		setEditable(false);
		
		m_AgentSelector = new AgentSelectionDialog(maxSelect);
		m_AgentSelector.getButtonById(Dialog.OK).addSelectionListener(
				new SelectionListener<ButtonEvent>(){
			@Override
            public void componentSelected(ButtonEvent ce)
            {
				setValue(IntroscopeDiagnosticsUIBuilder.INTROSCOPE_MESSAGES.agentCountSelected(
						m_AgentSelector.getSelectedAgents().size()));
            }
		});
	}


    @Override
    protected void onTriggerClick(ComponentEvent ce)
    {
	    super.onTriggerClick(ce);
	    
	    AgentSelectionDialog agentSelector = getAgentSelector();
	    agentSelector.show();
	    agentSelector.load();
    }
    
    
    /**
     * Returns the trigger field's agent selector dialog. 
     * @return the <code>AgentSelectionDialog</code> opened by this field.
     */
    public AgentSelectionDialog getAgentSelector()
    {
    	return m_AgentSelector;
    }
    
    
    /**
	 * Sets the list of Introscope agents that have been selected for analysis.
	 * @param agents the list of selected agents.
	 */
    public void setSelectedAgents(List<String> agents)
    {
    	getAgentSelector().setSelectedAgents(agents);
    	setValue(IntroscopeDiagnosticsUIBuilder.INTROSCOPE_MESSAGES.agentCountSelected(
    			agents.size()));
    }
    
    
    /**
	 * Returns the list of Introscope agents that have been selected for analysis.
	 * @return the list of selected agents.
	 */
    public List<String> getSelectedAgents()
    {
    	return getAgentSelector().getSelectedAgents();
    }


    @Override
    protected boolean validateValue(String value)
    {
    	// Only valid if 1 or more agents are selected.
    	boolean result = super.validateValue(value);
    	if (result == true)
    	{
    		result = (getSelectedAgents().size() > 0);
    	}
    	
	    return result;
    }
}
