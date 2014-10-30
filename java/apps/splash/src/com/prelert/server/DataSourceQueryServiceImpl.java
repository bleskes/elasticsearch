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

package com.prelert.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;

import com.extjs.gxt.ui.client.data.BaseListLoadResult;
import com.extjs.gxt.ui.client.data.BaseModelData;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.prelert.dao.DataSourceDAO;
import com.prelert.data.DataSource;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.DataSourceType;
import com.prelert.data.gxt.DataSourceModel;
import com.prelert.data.gxt.DataSourceTreeModel;
import com.prelert.service.DataSourceQueryService;



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
	 * @param includeCounts <code>true</code> to include data point count in the returned data,
	 * 			<code>false</code> otherwise.
	 * @return the complete list of data source types, as tree model objects.
	 */
	public List<DataSourceTreeModel> getDataSourceTypeTreeModels(boolean includeCounts)
	{
		List<DataSourceType> dataSourceTypes = m_DataSourceDAO.getDataSourceTypes();
		
		// Convert to list of DataSourceTreeModel objects.
		List<DataSourceTreeModel> dataSourceModels =  new ArrayList<DataSourceTreeModel>();
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
				if (includeCounts == true)
				{
					model.setCount(sourceType.getCount());
				}
				dataSourceModels.add(model);
			}
		}
		
		return dataSourceModels;
	}
	
	
	
	/**
	 * Returns a list of all sources for a specified source type, ordered by 
	 * source name e.g. a list of p2ps servers supplying service usage information.
	 * @param dataSourceType the source type for which to return the list of sources.
	 * @param includeCounts <code>true</code> to include data point count in the returned data,
	 * 			<code>false</code> otherwise.
	 * @return the complete list of sources for the given data source type.
	 */
	protected List<DataSourceTreeModel> getDataSourcesForType(
			DataSourceTreeModel sourceType, boolean includeCounts)
	{		
		List<DataSource> dataSources = 
			m_DataSourceDAO.getDataSources(sourceType.getDataSourceType());
		
		// Convert to list of DataSourceTreeModel objects.
		List<DataSourceTreeModel> dataSourceModels = 
			new ArrayList<DataSourceTreeModel>(dataSources.size());
		DataSourceTreeModel model;
		for (DataSource dataSource : dataSources)
		{
			model = new DataSourceTreeModel();
			model.setText(dataSource.getSource());
			model.setSource(dataSource.getSource());
			model.setDataSourceType(dataSource.getDataSourceType());
			
			if (includeCounts = true)
			{
				model.setCount(dataSource.getCount());
			}
			
			dataSourceModels.add(model);
		}
		
		return dataSourceModels;
	}
	
	
	/**
	 * Returns a list of all sources for a specified source type, in descending 
	 * order of data point count e.g. a list of p2ps servers supplying service 
	 * usage information.
	 * @param dataSourceType the source type for which to return the list of sources.
	 * @return the list of sources for the given data source type, complete 
	 * with data point count, as tree model objects.
	 */
	public List<DataSourceTreeModel> getDataSourceTreeModelsByCount(
			DataSourceTreeModel dataSourceType)
	{
		List<DataSource> dataSources = m_DataSourceDAO.getDataSources(dataSourceType.getDataSourceType());
		
		// Convert to list of DataSourceTreeModel objects.
		List<DataSourceTreeModel> dataSourceModels = 
			new ArrayList<DataSourceTreeModel>(dataSources.size());
		DataSourceTreeModel model;
		for (DataSource dataSource : dataSources)
		{
			model = new DataSourceTreeModel();
			model.setText(dataSource.getSource());
			model.setSource(dataSource.getSource());
			model.setDataSourceType(dataSource.getDataSourceType());
			model.setCount(dataSource.getCount());
			
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
	 * 			If <code>null</code>, then an 'Analysed Data' root node will be
	 * 			returned. If called with 'Analysed Data' root node (i.e. where the
	 * 			data source category is null), then the the list of source types 
	 * 			from which data has been retrieved will be returned.
	 * @param includeCounts <code>true</code> to include data point count in the returned data,
	 * 			<code>false</code> otherwise.
	 * @return list of data sources for the given 'parent' data source as tree model objects.
	 */
	public List<DataSourceTreeModel> getDataSourceTreeModels(
			DataSourceTreeModel dataSource, boolean includeCounts)
    { 	
    	if (dataSource == null)
    	{
    		// Create the 'Analysed Data' root node.
    		DataSourceTreeModel analysedDataRoot = new DataSourceTreeModel();
    		analysedDataRoot.setText("Analysed Data");	// Overwritten client-side with I18N value.
    		
    		List<DataSourceTreeModel> analysedDataModel = new ArrayList<DataSourceTreeModel>(1);
    		analysedDataModel.add(analysedDataRoot);
    		return analysedDataModel;
    	}
    	else
    	{
    		List<DataSourceTreeModel> dataSourceModels;
    		
    		if (dataSource.getDataSourceCategory() == null)
    		{
    			// Obtain the list of data source types. 
    			dataSourceModels = getDataSourceTypeTreeModels(includeCounts);
        		
        		// Place notification types before time series.
        		Collections.sort(dataSourceModels, new ModelTypeComparator());
    		}
    		else
    		{
    			// Return the list of sources for this data source type.
    			dataSourceModels = getDataSourcesForType(dataSource, includeCounts);
    		}
    		
    		return dataSourceModels;
    	}
    	
    }
	

	/**
	 * Returns a list of all the source types from which data has been retrieved
	 * by the Prelert engine e.g. p2ps logs, CPU data, p2psmon user usage data.
	 * @param includeCounts <code>true</code> to include data point count in the returned data,
	 * 			<code>false</code> otherwise.
	 * @return the complete list of data source types, in ascending alphabetical 
	 * 			order of data source type name.
	 */
    public List<DataSourceModel> getDataSourceTypes()
    {
    	List<DataSourceType> dataSourceTypes = m_DataSourceDAO.getDataSourceTypes();
    	
    	// Place in ascending alphabetical order of data source type name.
		Collections.sort(dataSourceTypes, new DataSourceTypeNameComparator());
		
		// Convert to list of DataSourceModel objects.
		List<DataSourceModel> dataSourceModels =  new ArrayList<DataSourceModel>();
		DataSourceModel model;
		for (DataSourceType sourceType : dataSourceTypes)
		{
			// Only add in Notifications and Time Series.
			if (sourceType.getDataCategory() == DataSourceCategory.NOTIFICATION ||
					sourceType.getDataCategory() == DataSourceCategory.TIME_SERIES)
			{
				model = new DataSourceModel();
				model.setDataSourceType(sourceType);
				model.setCount(sourceType.getCount());
				dataSourceModels.add(model);
			}
		}
		
		return dataSourceModels;
    }


    /**
	 * Returns a list of 'child' data sources for the given 'parent' data source
	 * e.g. the list of p2ps servers supplying p2ps log file data.
	 * @param dataSource the source for which to obtain the 'child' data sources.
	 * 			If <code>null</code>, then an 'Analysed Data' root node will be
	 * 			returned. If called with 'Analysed Data' root node (i.e. where the
	 * 			data source category is null), then the the list of source types 
	 * 			from which data has been retrieved will be returned.
	 * @param includeCounts <code>true</code> to include data point count in the returned data,
	 * 			<code>false</code> otherwise.
	 * @return list of data sources for the given 'parent' data source.
	 */
	public List<DataSourceModel> getDataSources(DataSourceType dataSourceType)
    {
    	List<DataSource> dataSources = m_DataSourceDAO.getDataSources(dataSourceType);
		
		// Convert to list of DataSourceModel objects.
		List<DataSourceModel> dataSourceModels = 
			new ArrayList<DataSourceModel>(dataSources.size());
		DataSourceModel model;
		for (DataSource dataSource : dataSources)
		{
			model = new DataSourceModel();
			model.setDataSourceType(dataSourceType);
			model.setSource(dataSource.getSource());
			model.setCount(dataSource.getCount());
			
			dataSourceModels.add(model);
		}
		
		return dataSourceModels;
    }


	/**
	 * Returns the complete list of data sources for list based load results.
	 * @return load result containing the requested data.
	 */
	public BaseListLoadResult<BaseModelData> getAllDataSourcesListLoadResult()
	{
		List<DataSource> dataSources = m_DataSourceDAO.getAllDataSources();
		
		List<BaseModelData> models = new ArrayList<BaseModelData>();
		
		BaseModelData dataSourceModel;
		for (DataSource dataSource : dataSources)
		{
			dataSourceModel = new BaseModelData();
			dataSourceModel.set("dataSourceName", dataSource.getDataSourceType().getName());
			dataSourceModel.set("dataSourceCategory", dataSource.getDataSourceType().getDataCategory().toString());
			dataSourceModel.set("source", dataSource.getSource());
			dataSourceModel.set("count", dataSource.getCount());
			
			models.add(dataSourceModel);
		}
		
		return new BaseListLoadResult<BaseModelData>(models);
	}
	
	
	/**
	 * Returns the data sources for the specified source type for list based load results.
	 * @return load result containing the requested data.
	 */
	public BaseListLoadResult<BaseModelData> getDataSourcesListLoadResult(DataSourceType dataSourceType)
    {
    	List<DataSource> dataSources = m_DataSourceDAO.getDataSources(dataSourceType);
    	
    	List<BaseModelData> models = new ArrayList<BaseModelData>();
		
		BaseModelData dataSourceModel;
		for (DataSource dataSource : dataSources)
		{
			dataSourceModel = new BaseModelData();
			dataSourceModel.set("dataSourceName", dataSource.getDataSourceType().getName());
			dataSourceModel.set("dataSourceCategory", dataSource.getDataSourceType().getDataCategory().toString());
			dataSourceModel.set("source", dataSource.getSource());
			dataSourceModel.set("count", dataSource.getCount());
			
			models.add(dataSourceModel);
		}
		
		logger.debug("getDataSourcesList() returning " + models.size());
		
		return new BaseListLoadResult<BaseModelData>(models);
    }
	
	
	/**
     * Comparator which sorts DataSourceType objects in ascending alphabetical
     * order of data source type name.
     */
    class DataSourceTypeNameComparator implements Comparator<DataSourceType>
    {
    	
        public int compare(DataSourceType type1, DataSourceType type2)
        {
	        String name1 = type1.getName();
	        String name2 = type2.getName();
	        
	        int comp;
	        
	        if (name1.equals(name2) == false)
	        {
	        	comp = name1.compareTo(name2);
	        }
	        else
	        {
	        	// e.g. names are the same for TIME_SERIES and TIME_SERIES_FEATURE.
	        	DataSourceCategory cat1 = type1.getDataCategory();
	        	DataSourceCategory cat2 = type2.getDataCategory();
	        	
	        	comp = cat1.compareTo(cat2);
	        }
	        
	        return comp;
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
