/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2010     *
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

package com.prelert.client.gxt;

import com.extjs.gxt.ui.client.event.Observable;
import com.extjs.gxt.ui.client.widget.Component;

/**
 * Interface defining the methods to be implemented by a module in the Prelert UI.
 * A module is a distinct area of functionality in the UI, grouping together all 
 * the graphical components for viewing information related to that functional area
 * in a single container.
 * @author Pete Harverson
 */
public interface ModuleComponent extends Observable
{
	/**
	 * Returns a distinct id for the module.
	 * @return the module ID.
	 */
	public String getModuleId();
	
	/**
	 * Returns the top-level container which holds all the graphical components
	 * for the module.
	 * @return top-level container for the module which will be displayed in the UI.
	 */
	public Component getComponent();
}
