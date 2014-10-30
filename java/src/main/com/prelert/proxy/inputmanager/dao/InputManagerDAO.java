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

package com.prelert.proxy.inputmanager.dao;

import java.util.List;

import com.prelert.data.Attribute;
import com.prelert.data.ExternalTimeSeriesDetails;
import com.prelert.data.TimeSeriesInterpretation;

/**
 * DAO interface for the database routines responsible for added and querying
 * details of external time series and evidence types.
 */
public interface InputManagerDAO 
{
	/**
	 * Register an external time series with the Prelert Database. 
	 * @param datatype
	 * @param metric
	 * @param interp
	 * @param graphName
	 * @param graphTitle
	 * @param graphYAxisLabel
	 * @param usualInterval
	 * @param pluginName
	 * @return The id of the created Time Series Type or -1 on failure
	 */
	public int addExternalTimeSeriesType(String datatype, String metric, 
											TimeSeriesInterpretation interp, 
											String graphName, String graphTitle, 
											String graphYAxisLabel, 
											int usualInterval, String plugin);

	/**
	 * For an existing Time Series (as determined by <code>datatype</code> and 
	 * <code>metric</code>) returns unique id for the newly created external Time
	 * Series and associates it with the <code>key</code>.
	 * The <code>key</code> value only has to be meaningful to the Plugin for this 
	 * external type and is passed to the Plugin to aid data retrieval.
	 * @param datatype
	 * @param metric
	 * @param externalKey for the Plugin
	 * @return The unique index for the newly created external time series or the 
	 * index of the existing time series with this <code>externalKey</code>.
	 * This function will always return a -ve number.
	 */
	public int addExternalTimeSeries(String datatype, String metric, String externalKey);
	
	
	/**
	 * For the given datatype and metric returns a list of Ids for external time 
	 * series of this type.
	 * @param datatype
	 * @param metric
	 * @return List of time series Ids
	 */
	public List<Integer> externalTimeSeriesIdsForType(String datatype, String metric);
	

	/**
	 * For those time series that are going to be "external points only", i.e.
	 * have all their metadata stored in the Prelert database, set the metadata.
	 * A metric path is created for the time series using the provided attributes.
	 * This method will only operate the first time it is called for a given
	 * time series ID.  It is not possible to change the metadata for a time
	 * series after it has been set.
	 * 
	 * @param timeSeriesId - As returned by a call to {@linkplain #addExternalTimeSeries}
	 * @param source - The source for this time series.
	 * @param attributes - List of attributes that will be used to build the 
	 * 						metric path. The order of these attributes determines
	 * 						the order of elements in the metric path.
	 * @return
	 */
	public boolean setExternalTimeSeriesDetails(int timeSeriesId, String source, 
										List<Attribute> attributes);
	
	
	/**
	 * Overloaded method allows specific values to be used as the source
	 * and metric prefixes and the metric path separator.
	 * 
	 * For those time series that are going to be "external points only", i.e.
	 * have all their metadata stored in the Prelert database, set the metadata.
	 * A metric path is created for the time series using the provided attributes.
	 * This method will only operate the first time it is called for a given
	 * time series ID.  It is not possible to change the metadata for a time
	 * series after it has been set.
	 * 
	 * @param timeSeriesId - As returned by a call to {@linkplain #addExternalTimeSeries}
	 * @param source - The source for this time series.
	 * @param attributes - List of attributes that will be used to build the 
	 * 						metric path. The order of these attributes determines
	 * 						the order of elements in the metric path.
	 * @param sourcePrefix - The string used to prefix the source in the metric path
	 * @param sourcePosition - The position of the source in the metric path. 
	 * 							If 0 then source is the first element in the path before
	 * 							all the attributes else it is inserted at this position.
	 * @param metricPrefix - The string used to prefix the metric in the metric path
	 * @param metricPathDelimiter - The string used to delimit elements of the 
	 * 								metric path
	 * @return
	 */
	public boolean setExternalTimeSeriesDetails(int timeSeriesId, String source, 
										List<Attribute> attributes,
										String sourcePrefix, int sourcePosition, 
										String metricPrefix,
										String metricPathDelimiter);
	
	
	/**
	 * Return the list of details for the external time series
	 * in the current database. 
	 *  
	 * @param activeOnly If true only return the external time series that have 
	 * been set active else return all.
	 *  
	 * @return
	 */
	public List<ExternalTimeSeriesDetails> getExternalTimeSeriesDetails(boolean activeOnly);
	
	
	/**
	 * For each Id in the list parameter set active to true for each
	 * external time series with that Id
	 * 
	 * @param timeSeriesIds List of external time series ids
	 * @return
	 */
	public boolean setExternalTimeSeriesActive(List<Integer> timeSeriesIds);
	
 }
