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

package demo.app.client;

import demo.app.client.list.EvidenceViewWindow;
import demo.app.client.list.ProbableCausesListWindow;
import demo.app.data.*;


/**
 * Factory class which creates Window objects for the Prelert desktop
 * dependent on the type of View that they will contain.
 * @author Pete Harverson
 */
public class ExtGWTWindowFactory
{
	
	/**
	 * Creates and returns a desktop Window for displaying the supplied View.
	 * @param desktop the Desktop that will contain the new window.
	 * @param view the View which will displayed in the new window.
	 * @return Window sub-class for displaying the provided View.
	 * @throws UnsupportedOperationException if display of the supplied View type
	 * is not currently supported in the Prelert desktop.
	 */
	public static ViewWindow createWindow(DesktopApp desktop, View view) throws UnsupportedOperationException
	{
		if (view.getClass() == EvidenceView.class)
		{
			return new EvidenceViewWindow(desktop, (EvidenceView)view);
		}
		else if (view.getClass() == CausalityView.class)
		{
			CausalityView causalityView = (CausalityView)view;
			if (causalityView.isDisplayAsEpisodes() == true)
			{
				return new EpisodeViewWindow(desktop, causalityView);
			}
			else
			{
				return new ProbableCausesListWindow(desktop, causalityView);
			}
		}
		else
		{
			throw new UnsupportedOperationException("Specified class " + 
					view.getClass().getName() + " of View " + 
					view.getName() + " not supported in Desktop");
		}
	}
}
