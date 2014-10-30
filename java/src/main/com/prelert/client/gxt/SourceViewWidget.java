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

package com.prelert.client.gxt;

import java.util.Date;
import java.util.List;

import com.prelert.data.DataSourceType;


/**
 * Interface defining methods to be implemented by GXT widgets used for displaying 
 * information from a Prelert data source relating to a specific source (server).
 * @author Pete Harverson
 */
public interface SourceViewWidget extends ViewWidget
{
	/**
	 * Returns the name of the source (server) whose data will be viewed in the
	 * widget.
	 * @return the name of the source whose data is currently being viewed,
	 * or <code>null</code> if no specific source is currently selected
	 * i.e. 'All Sources' is selected.
	 */
	public String getSource();	
	
	
	/**
	 * Sets the name of the source (server) whose data will be viewed in the widget.
	 * @param source the name of the source (server) whose data is to be
	 * displayed. If <code>null</code>, data from all sources will be displayed.
	 */
	public void setSource(String source);
	
	
	/**
     * Sets the list of data types that can be linked to from the widget.
     * @param dataSourceTypes list of data types which should be accessible from
     * 		data in the widget.
     */
	public void setLinkToDataTypes(List<DataSourceType> dataSourceTypes);
	
	
	/**
	 * Loads the data in the widget according to its current configuration.
	 */
	public void loadAtTime(Date time);
}
