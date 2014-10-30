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

package com.prelert.client.event;

import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.widget.Component;


/**
 * Show Module event type, fired from a component to trigger the UI switching to 
 * show a different module.
 * @author Pete Harverson
 */
public class ShowModuleEvent extends ComponentEvent
{
	private String 			m_ModuleId;
	
	
	/**
	 * Creates a new NavigationBarEvent.
	 * @param component the component that is the source of the event.
	 * @param moduleId the ID of the target module e.g. the ID of the Explorer
	 * module if the user has clicked on the Explorer link in the Navigation bar.
	 */
	public ShowModuleEvent(Component component, String moduleId)
	{
		super(component);
		m_ModuleId = moduleId;
	}
	
	
	/**
	 * Returns the id of the target module of the event. For example, if the user 
	 * has clicked on the link to the Explorer module, the ID of the Explorer
	 * module will be returned. 
	 * @return the ID of the target module.
	 */
	public String getModuleId()
	{
		return m_ModuleId;
	}

}
