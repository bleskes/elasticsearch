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

package demo.app.splash.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import demo.app.data.DataSourceCategory;
import demo.app.data.DataSourceType;
import demo.app.data.gxt.DataSourceTreeModel;
import demo.app.splash.dao.DataSourceDAO;
import demo.app.splash.service.DataSourceQueryService;


/**
 * Server-side implementation of the service for retrieving information on 
 * Prelert data sources.
 * @author Pete Harverson
 */
public class DataSourceQueryServiceImpl extends RemoteServiceServlet 
	implements DataSourceQueryService
{
	static Logger logger = Logger.getLogger(DataSourceQueryServiceImpl.class);
	
	private DataSourceDAO	m_DataSourceDAO;
	
	
	/**
	 * Sets the DataSourceDAO to be used to make queries on data sources.
	 * @param dataSourceDAO the data access object for Prelert data source information.
	 */
	public void setDataSourceDAO(DataSourceDAO dataSourceDAO)
	{
		m_DataSourceDAO = dataSourceDAO;
	}
	
	
	/**
	 * Returns the DataSourceDAO being used to make queries on data sources.
	 * @param dataSourceDAO the data access object for Prelert data source information.
	 */
	public DataSourceDAO getDataSourceDAO()
	{
		return m_DataSourceDAO;
	}
	
	
	/**
	 * Returns a list of all the source types from which data has been retrieved
	 * by the Prelert engine e.g. p2ps logs, CPU data, p2psmon user usage data.
	 * <p>
	 * <b>N.B.</b> the returned list of DataSourceModel objects will contain no count data.
	 * @return the complete list of data source types, with no count data.
	 */
	public List<DataSourceTreeModel> getDataSourceTypes()
	{
		List<DataSourceType> dataSourceTypes = m_DataSourceDAO.getDataSourceTypes();
		
		// Convert to list of DataSourceModel objects.
		List<DataSourceTreeModel> dataSourceModels = 
			new ArrayList<DataSourceTreeModel>();
		DataSourceTreeModel model;
		for (DataSourceType sourceType : dataSourceTypes)
		{
			// Only add in Notifications and Time Series.
			if (sourceType.getDataCategory() == DataSourceCategory.NOTIFICATION ||
					sourceType.getDataCategory() == DataSourceCategory.TIME_SERIES)
			{
				model = new DataSourceTreeModel();
				model.setText(sourceType.getName());
				model.setDataSourceType(sourceType);
				dataSourceModels.add(model);
			}
		}
		
		return dataSourceModels;
	}
	
	
	/**
	 * Returns a list of all the source types from which data has been retrieved
	 * by the Prelert engine e.g. p2ps logs, CPU data, p2psmon user usage data.
	 * @return the complete list of data source types, complete with data point count.
	 */
	public List<DataSourceTreeModel> getDataSourceTypeCounts()
	{	
		TreeMap<DataSourceType, Integer> dataSourceTypes = m_DataSourceDAO.getDataSourceTypeCounts();
		
		// Convert to list of DataSourceModel objects.
		Iterator<DataSourceType> sourceTypesIter = dataSourceTypes.keySet().iterator();
		
		List<DataSourceTreeModel> dataSourceModels = 
			new ArrayList<DataSourceTreeModel>(dataSourceTypes.size());
		
		DataSourceTreeModel model;
		DataSourceType sourceType;
		while (sourceTypesIter.hasNext())
		{
			sourceType = sourceTypesIter.next();
			
			// Only add in Notifications and Time Series.
			if (sourceType.getDataCategory() == DataSourceCategory.NOTIFICATION ||
					sourceType.getDataCategory() == DataSourceCategory.TIME_SERIES)
			{
				model = new DataSourceTreeModel();
				model.setDataSourceType(sourceType);
				model.setText(sourceType.getName());
				model.setCount(dataSourceTypes.get(sourceType));
				
				dataSourceModels.add(model);
			}
		}
		
		return dataSourceModels;
	}
	
	
	/**
	 * Returns a list of all sources for a specified source type, ordered by 
	 * source name e.g. a list of p2ps servers supplying service usage information.
	 * <p>
	 * <b>N.B.</b> the returned list of DataSourceModel objects will contain no count data.
	 * @param dataSourceType the source type for which to return the list of sources.
	 * @return the complete list of sources for the given data source type, with no
	 * 			count data.
	 */
	public List<DataSourceTreeModel> getSources(DataSourceTreeModel sourceType)
	{
		List<String> dataSources = m_DataSourceDAO.getSources(sourceType.getDataSourceType());
		
		// Convert to list of DataSourceModel objects.
		List<DataSourceTreeModel> dataSourceModels = 
			new ArrayList<DataSourceTreeModel>(dataSources.size());
		DataSourceTreeModel model;
		for (String source : dataSources)
		{
			model = new DataSourceTreeModel();
			model.setText(source);
			model.setSource(source);
			model.setDataSourceName(sourceType.getDataSourceName());
			model.setDataSourceCategory(sourceType.getDataSourceCategory());
			dataSourceModels.add(model);
		}
		
		return dataSourceModels;
	}
	
	
	/**
	 * Returns a list of all sources for a specified source type, ordered by 
	 * source name e.g. a list of p2ps servers supplying service usage information.
	 * @param dataSourceType the source type for which to return the list of sources.
	 * @return the complete list of sources for the given data source type, complete 
	 * with data point count.
	 */
	public List<DataSourceTreeModel> getSourceCounts(DataSourceTreeModel sourceType)
	{	
		TreeMap<String, Integer> dataSources =  
			m_DataSourceDAO.getSourceCounts(sourceType.getDataSourceType());
		
		// Convert to list of DataSourceModel objects.
		Iterator<String> sourcesIter = dataSources.keySet().iterator();
		
		List<DataSourceTreeModel> dataSourceModels = 
			new ArrayList<DataSourceTreeModel>(dataSources.size());
		String source;
		DataSourceTreeModel model;
		while (sourcesIter.hasNext())
		{	
			source = sourcesIter.next();
			
			model = new DataSourceTreeModel();
			model.setText(source);
			model.setSource(source);
			model.setDataSourceName(sourceType.getDataSourceName());
			model.setDataSourceCategory(sourceType.getDataSourceCategory());
			model.setCount(dataSources.get(source));
			dataSourceModels.add(model);
		}
		
		return dataSourceModels;
	}
	
	
	/**
	 * Returns a list of all sources for a specified source type, in descending 
	 * order of data point count e.g. a list of p2ps servers supplying service 
	 * usage information.
	 * @param dataSourceType the source type for which to return the list of sources.
	 * @return the complete list of sources for the given data source type, complete 
	 * with data point count.
	 */
	public List<DataSourceTreeModel> getSourcesOrderByCount(DataSourceTreeModel sourceType)
	{
		TreeMap<String, Integer> dataSources =  
			m_DataSourceDAO.getSourceCounts(sourceType.getDataSourceType());
		
		// Convert to list of DataSourceModel objects.
		Iterator<String> sourcesIter = dataSources.keySet().iterator();
		
		List<DataSourceTreeModel> dataSourceModels = 
			new ArrayList<DataSourceTreeModel>(dataSources.size());
		String source;
		DataSourceTreeModel model;
		while (sourcesIter.hasNext())
		{
			source = sourcesIter.next();
			
			model = new DataSourceTreeModel();
			model.setText(source);
			model.setSource(source);
			model.setDataSourceName(sourceType.getDataSourceName());
			model.setDataSourceCategory(sourceType.getDataSourceCategory());
			dataSourceModels.add(model);
		}
		
		// Place in descending order of data point count.
		Collections.sort(dataSourceModels, new ModelCountComparator());
		
		return dataSourceModels;
	}
	
	
	/**
	 * Returns a list of 'child' data sources for the given 'parent' data source
	 * e.g. the list of p2ps servers supplying p2ps log file data.
	 * @param dataSource the source for which to obtain the 'child' data sources.
	 * 			If <code>null</code>, then the list of source types from which data
	 * 			has been retrieved will be returned.
	 * @return list of data sources for the given 'parent' data source.
	 */
    public List<DataSourceTreeModel> getDataSources(DataSourceTreeModel dataSource)
    { 	
    	if (dataSource == null)
	    {
    		// Place notification types before time series.
    		List<DataSourceTreeModel> dataSourceTypes = getDataSourceTypeCounts();
    		Collections.sort(dataSourceTypes, new ModelTypeComparator());
    		
    		return dataSourceTypes;
	    }
	    else
	    {	
	    	return getSources(dataSource);
	    }
    }
    
    
    /**
     * Comparator which sorts DataSourceModel objects in descending 
     * data point count order.
     */
    class ModelCountComparator implements Comparator<DataSourceTreeModel>
    {
        public int compare(DataSourceTreeModel model1, DataSourceTreeModel model2)
        {
	        Integer count1 = model1.getCount();
	        Integer count2 = model2.getCount();
	        
	        return count2.compareTo(count1);
        }	
    }
	

    /**
     * Comparator which sorts DataSourceModel objects by placing NOTIFICATION types
     * first, and then in ascending alphabetical order of data source type name.
     */
    class ModelTypeComparator implements Comparator<DataSourceTreeModel>
    {
        public int compare(DataSourceTreeModel model1, DataSourceTreeModel model2)
        {
        	int comparison = 0;
        	
        	DataSourceCategory cat1 = model1.getDataSourceCategory();
        	DataSourceCategory cat2 = model2.getDataSourceCategory();
        	
        	if (cat1.equals(cat2) == false)
        	{
        		if (cat1 == DataSourceCategory.NOTIFICATION)
        		{
        			comparison = -1;
        		}
        		else
        		{
        			comparison = 1;
        		}
        	}
        	else
        	{
        		String name1 = model1.getDataSourceName();
    	        String name2 = model2.getDataSourceName();
    	        
    	        comparison = name1.compareTo(name2);
        	} 
	        
	        return comparison;
        }
    }

}
