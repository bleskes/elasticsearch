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

package demo.app.splash.dao;

import java.util.List;
import java.util.TreeMap;

import demo.app.data.DataSourceType;


/**
 * Interface defining the methods to be implemented by a Data Access Object
 * to obtain information on Prelert data sources.
 * 
 * @author Pete Harverson
 */
public interface DataSourceDAO
{
	/**
	 * Returns a list of all the source types from which data has been retrieved
	 * by the Prelert engine e.g. p2ps logs, CPU data, p2psmon user usage data.
	 * @return the complete list of data source types.
	 */
	public List<DataSourceType> getDataSourceTypes();
	
	
	/**
	 * Returns a map of the source types against the total number of data points
	 * that have been collected for each type, ordered alphabetically by source type.
	 * @return TreeMap of data source types against the number of 
	 * 			data points collected for each, ordered by source type.
	 */
	public TreeMap<DataSourceType, Integer> getDataSourceTypeCounts();
	
	
	/**
	 * Returns a list of all sources for a specified source type, ordered by 
	 * source name e.g. a list of p2ps servers supplying service usage information.
	 * @param dataSourceType the source type for which to return the list of sources.
	 * @return the complete list of sources for the given data source type.
	 */
	public List<String> getSources(DataSourceType dataSourceType);
	
	
	/**
	 * Returns a map of the sources for the specified source type against the
	 * total number of data points that have been collected for each source,
	 * ordered by source name.
	 * @param dataSourceType the source type for which to return the sources.
	 * @return TreeMap of data sources against the number of data points 
	 * 			collected for each, ordered by source name.
	 */
	public TreeMap<String, Integer> getSourceCounts(DataSourceType dataSourceType);
	
}
