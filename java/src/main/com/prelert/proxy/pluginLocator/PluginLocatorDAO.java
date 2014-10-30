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

package com.prelert.proxy.pluginLocator;

import java.util.List;

import com.prelert.data.DataSourceCategory;
import com.prelert.proxy.data.ExternalDataTypeConfig;
import com.prelert.proxy.data.ExternalTimeSeriesConfig;

/**
 * This class queries the Prelert database about the source of known evidence and
 * time series types. Specifically whether the type is internal or external and if
 * external then how to locate it.
 */
public interface PluginLocatorDAO
{

	/**
	 * Determines whether the data for <code>dataType</code> is stored in the
	 * the internal Prelert database or externally and accessed through a
	 * plugin.  Searches Time Series and Evidence types for
	 * <code>dataType</code>.
	 * @param dataType can be either a Time Series or Evidence type.
	 * @return True if dataType is served by an external plugin.
	 */
	boolean isExternal(String dataType);


	/**
	 * Queries the database to determine the type of external plugin that needs
	 * to be used to access data externally.  If the data type is internal (or
	 * unknown) then null is returned.
	 * @param dataType Can be either a Time Series or Evidence type.
	 * @return The name of the plugin for external types; null otherwise.
	 */
	String getPluginName(String dataType);


	/**
	 * Returns details of each external datatype and its <code>Plugin</code>
	 * registered with the Prelert database. 
	 * @return List of configurations for each external data type.
	 */
	public List<ExternalDataTypeConfig> getExternalPluginsDescriptions();


	/**
	 * For the given Time Series Id which is an external Time Series return
	 * details of the Plugin used to access that Time Series's data.
	 * <code>timeSeriesId</code> must be < 0.
	 * @param timeSeriesId must be < 0.
	 * @return null if no plugins were found else the description of how to
	 *         access the external time series, i.e. plugin and key.
	 */
	public ExternalTimeSeriesConfig getPluginDescriptionForTimeSeriesId(int timeSeriesId);
	
	
	/**
	 * Adds the type to the evidence_type table if it is not already present.  
	 * It will return the id allocated to the type (regardless of whether 
	 * the type was already present).
	 * 
	 * @param type - The data source type name.
	 * @param category - One of Notification/Time Series Feature/etc
	 * @return - The type id
	 */
	public int addEvidenceType(String type, DataSourceCategory category);

}
