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

package com.prelert.server;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.prelert.dao.EvidenceDAO;
import com.prelert.data.Attribute;
import com.prelert.data.Evidence;
import com.prelert.data.TimeFrame;
import com.prelert.data.gxt.AttributeModel;
import com.prelert.data.gxt.DatePagingLoadResult;
import com.prelert.data.gxt.EvidenceModel;
import com.prelert.data.gxt.EvidencePagingLoadConfig;
import com.prelert.service.EvidenceQueryService;


/**
 * Server-side implementation of the service for retrieving evidence data from the
 * Prelert database for display in evidence list views.
 * @author Pete Harverson
 */
@SuppressWarnings("serial")
public class EvidenceQueryServiceImpl extends RemoteServiceServlet
	implements EvidenceQueryService
{
	static Logger s_Logger = Logger.getLogger(EvidenceQueryServiceImpl.class);
	
	private EvidenceDAO 		m_EvidenceDAO;
	
	
	/**
	 * Sets the EvidenceDAO to be used by the evidence query service.
	 * @param evidenceDAO the data access object for evidence views.
	 */
	public void setEvidenceDAO(EvidenceDAO evidenceDAO)
	{
		m_EvidenceDAO = evidenceDAO;
	}
	
	
	/**
	 * Returns the EvidenceViewDAO being used by the evidence query service.
	 * @return the data access object for evidence views.
	 */
	public EvidenceDAO getEvidenceDAO()
	{
		return m_EvidenceDAO;
	}
	
	
	@Override
	public DatePagingLoadResult<EvidenceModel> getFirstPage(EvidencePagingLoadConfig loadConfig)
	{
		String dataType = loadConfig.getDataType();
		String source = loadConfig.getSource();
		List<String> filterAttributes = loadConfig.getFilterAttributes();
		List<String> filterValues = loadConfig.getFilterValues();

		List<Evidence> evidenceList = m_EvidenceDAO.getFirstPage(
				dataType, source, filterAttributes, filterValues, loadConfig.getPageSize());

		return createDatePagingLoadResult(evidenceList, dataType, 
								source, filterAttributes, filterValues);
	}
	
	
	@Override
	public DatePagingLoadResult<EvidenceModel> getLastPage(EvidencePagingLoadConfig loadConfig)
	{	
		String dataType = loadConfig.getDataType();
		String source = loadConfig.getSource();
		List<String> filterAttributes = loadConfig.getFilterAttributes();
		List<String> filterValues = loadConfig.getFilterValues();
		
		List<Evidence> evidenceList = m_EvidenceDAO.getLastPage(
				dataType, source, filterAttributes, filterValues, loadConfig.getPageSize());
		
		return createDatePagingLoadResult(evidenceList, dataType, 
								source, filterAttributes, filterValues);
	}
	
	
	@Override
	public DatePagingLoadResult<EvidenceModel> getNextPage(
			EvidencePagingLoadConfig loadConfig)
	{
		String dataType = loadConfig.getDataType();
		String source = loadConfig.getSource();
		Timestamp bottomRowTimeStamp = new Timestamp(loadConfig.getTime().getTime());
		List<String> filterAttributes = loadConfig.getFilterAttributes();
		List<String> filterValues = loadConfig.getFilterValues();

		List<Evidence> evidenceList = m_EvidenceDAO.getNextPage(
				dataType, source, bottomRowTimeStamp, loadConfig.getRowId(), 
				filterAttributes, filterValues, loadConfig.getPageSize());
		
		return createDatePagingLoadResult(evidenceList, dataType, 
								source, filterAttributes, filterValues);
	}
	
	
	@Override
	public DatePagingLoadResult<EvidenceModel> getPreviousPage(
			EvidencePagingLoadConfig loadConfig)
	{
		// Check if there is a date in the loadConfig (if not, the calling
		// page would have been empty). 
		if (loadConfig.getTime() == null)
		{
			return getFirstPage(loadConfig);
		}
		
		String dataType = loadConfig.getDataType();
		String source = loadConfig.getSource();
		Timestamp topRowTime = new Timestamp(loadConfig.getTime().getTime());
		List<String> filterAttributes = loadConfig.getFilterAttributes();
		List<String> filterValues = loadConfig.getFilterValues();

		List<Evidence> evidenceList = m_EvidenceDAO.getPreviousPage(
				dataType, source, topRowTime, loadConfig.getRowId(), 
				filterAttributes, filterValues, loadConfig.getPageSize());
		
		// If empty, load the first page - the previous button is always enabled.
		if (evidenceList == null || evidenceList.size() == 0)
		{
			evidenceList = m_EvidenceDAO.getFirstPage(dataType, source, 
					filterAttributes, filterValues, loadConfig.getPageSize());
		}
		
		return createDatePagingLoadResult(evidenceList, dataType, 
								source, filterAttributes, filterValues);
	}
	

	@Override
	public DatePagingLoadResult<EvidenceModel> getAtTime(EvidencePagingLoadConfig loadConfig)
	{
		// Check if there is a date in the loadConfig. 
		if (loadConfig.getTime() == null)
		{
			return getFirstPage(loadConfig);
		}
		
		String dataType = loadConfig.getDataType();
		String source = loadConfig.getSource();
		Timestamp time = new Timestamp(loadConfig.getTime().getTime());
		List<String> filterAttributes = loadConfig.getFilterAttributes();
		List<String> filterValues = loadConfig.getFilterValues();
		
		List<Evidence> evidenceList = m_EvidenceDAO.getAtTime(
				dataType, source, time, filterAttributes, filterValues, loadConfig.getPageSize());
		
		return createDatePagingLoadResult(evidenceList, dataType, source, 
									filterAttributes, filterValues);
	}
	
	
	@Override
    public DatePagingLoadResult<EvidenceModel> getIdPage(EvidencePagingLoadConfig loadConfig)
    {
    	String dataType = loadConfig.getDataType();
    	String source = loadConfig.getSource();
    	int id = loadConfig.getRowId();
    	
    	List<Evidence> evidenceList = m_EvidenceDAO.getIdPage(
    			dataType, source, id, loadConfig.getPageSize());
    	
    	return createDatePagingLoadResult(evidenceList, dataType, source, 
    									null, null);
    }
    
    
	@Override
	public DatePagingLoadResult<EvidenceModel> searchFirstPage(EvidencePagingLoadConfig config)
	{
		String dataType = config.getDataType();
		String source = config.getSource();
		String containsText = config.getContainsText();
		
		List<Evidence> evidenceList = m_EvidenceDAO.searchFirstPage(
				dataType, source, containsText, config.getPageSize());
		
		return createDatePagingLoadResult(
				evidenceList, dataType, source, containsText);
	}
	
	
	@Override
	public DatePagingLoadResult<EvidenceModel> searchLastPage(EvidencePagingLoadConfig config)
	{
		String dataType = config.getDataType();
		String source = config.getSource();
		String containsText = config.getContainsText();
		
		List<Evidence> evidenceList = 
			m_EvidenceDAO.searchLastPage(dataType, source, containsText, config.getPageSize());
		
		return createDatePagingLoadResult(
				evidenceList, dataType, source, containsText);
	}
	
	
	@Override
	public DatePagingLoadResult<EvidenceModel> searchNextPage(
			EvidencePagingLoadConfig config)
	{
		String dataType = config.getDataType();
		String source = config.getSource();
		Timestamp bottomRowTime = new Timestamp(config.getTime().getTime());
		String containsText = config.getContainsText();
		
		List<Evidence> evidenceList = m_EvidenceDAO.searchNextPage(dataType, source, 
				bottomRowTime, config.getRowId(), containsText, config.getPageSize());
		
		return createDatePagingLoadResult(
				evidenceList, dataType, source, containsText);
	}
	
	
	@Override
	public DatePagingLoadResult<EvidenceModel> searchPreviousPage(
			EvidencePagingLoadConfig config)
	{
		// Check if there is a date in the loadConfig (if not, the calling
		// page would have been empty). 
		if (config.getTime() == null)
		{
			return searchFirstPage(config);
		}
		
		String dataType = config.getDataType();
		String source = config.getSource();
		Timestamp topRowTime = new Timestamp(config.getTime().getTime());
		String containsText = config.getContainsText();

		List<Evidence> evidenceList = m_EvidenceDAO.searchPreviousPage(dataType, source, 
				topRowTime, config.getRowId(), containsText, config.getPageSize());
		
		// If empty, load the first page - the previous button is always enabled.
		if (evidenceList == null || evidenceList.size() == 0)
		{
			evidenceList = m_EvidenceDAO.searchFirstPage(
					dataType, source, containsText, config.getPageSize());
		}
		
		return createDatePagingLoadResult(
				evidenceList, dataType, source, containsText);
	}
	
	
	@Override
	public DatePagingLoadResult<EvidenceModel> searchAtTime(
			EvidencePagingLoadConfig config)
	{
		// Check if there is a date in the loadConfig. 
		if (config.getTime() == null)
		{
			return searchFirstPage(config);
		}
		
		String dataType = config.getDataType();
		String source = config.getSource();
		Timestamp time = new Timestamp(config.getTime().getTime());
		String containsText = config.getContainsText();
		
		List<Evidence> evidenceList = m_EvidenceDAO.searchAtTime(
				dataType, source, time, containsText, config.getPageSize());
		
		return createDatePagingLoadResult(
				evidenceList, dataType, source, containsText);
	}


	@Override
	public List<AttributeModel> getEvidenceAttributes(int id)
	{
		List<AttributeModel> models = new Vector<AttributeModel>();
		
		List<Attribute> attributes = m_EvidenceDAO.getEvidenceAttributes(id);
		for (Attribute attr : attributes)
		{
			AttributeModel model = new AttributeModel(attr.getAttributeName(), attr.getAttributeValue());
			models.add(model);
		}
		
		return models;		
	}
	
	
	@Override
	public EvidenceModel getEvidenceSingle(int id)
	{
		Evidence evidence = m_EvidenceDAO.getEvidenceSingle(id);
		
		EvidenceModel evidenceForId = null;
		if (evidence != null)
		{
			evidenceForId = new EvidenceModel(evidence.getProperties());
		}
		
		return evidenceForId;
	}
	
	
	@Override
    public List<String> getAllColumns(String dataType)
    {
    	return m_EvidenceDAO.getAllColumns(dataType);
    }
    
    
	@Override
	public List<String> getFilterableColumns(String dataType, 
			boolean getCompulsory, boolean getOptional)
	{
		return m_EvidenceDAO.getFilterableColumns(dataType, getCompulsory, getOptional);
	}


	@Override
	public List<String> getColumnValues(String dataType, String columnName, int maxRows)
	{
		List<String> columnValues = null;
		if (columnName.equalsIgnoreCase("severity") == false)
		{
			columnValues = m_EvidenceDAO.getColumnValues(dataType, columnName, maxRows);
		}
		else
		{
			// For severity just return a hardcoded list of 
			columnValues = Arrays.asList("clear", "unknown", "warning", 
						"minor", "major", "critical");
		}
		
		s_Logger.debug("getColumnValues(" + dataType + "," + columnName + 
				") returning: " + columnValues.size());
		
		return columnValues;
	}
	
	
	/**
	 * Creates a DatePagingLoadResult object for the supplied list of evidence data
	 * and load criteria.
	 * @return the DatePagingLoadResult.
	 */
	protected DatePagingLoadResult<EvidenceModel> createDatePagingLoadResult(
			List<Evidence> evidenceList, String dataType, String source, 
			List<String> filterAttributes, List<String> filterValues)
	{
		// Note that the list is ordered in DESCENDING order, so the first page
		// corresponds to the latest date in the database.
		Evidence startEvidence = m_EvidenceDAO.getLatestEvidence(
				dataType, source, filterAttributes, filterValues);
		Date startDate = null;
		if (startEvidence != null)
		{
			startDate = startEvidence.getTime();
		}
		
		Evidence endEvidence = m_EvidenceDAO.getEarliestEvidence(
				dataType, source, filterAttributes, filterValues);
		Date endDate = null;
		if (endEvidence != null)
		{
			endDate = endEvidence.getTime();
		}
		
		Date firstRowTime = null;
		if (evidenceList != null && evidenceList.size() > 0)
		{
			Evidence firstRow = evidenceList.get(0);
			firstRowTime = firstRow.getTime();
		}
		
		// Set the flag indicating if there is an earlier load result for this 
		// paging config. i.e. false if the result set is null/empty or the last
		// row in the result set is the record that is furthest back in time for this config.
		boolean isEarlierResults = false;
		if (evidenceList != null && evidenceList.size() > 0)
		{
			int lastIdInResults = evidenceList.get(evidenceList.size()-1).getId();
			int earliestId = endEvidence.getId();

			if (lastIdInResults != earliestId)
			{
				isEarlierResults = true;
			}
		}
		
		// Convert Evidence to EvidenceModels
		Vector<EvidenceModel> modelList = new Vector<EvidenceModel>();
		
		for (Evidence evidence : evidenceList)
		{
			EvidenceModel model = new EvidenceModel(evidence.getProperties());
			modelList.add(model);
		}
		
		return new DatePagingLoadResult<EvidenceModel>(modelList, TimeFrame.SECOND,
				firstRowTime, startDate, endDate, isEarlierResults);
	}
	
	
	/**
	 * Creates a DatePagingLoadResult object for the supplied list of evidence data
	 * and search criteria.
	 * @return the DatePagingLoadResult.
	 */
	protected DatePagingLoadResult<EvidenceModel> createDatePagingLoadResult(
			List<Evidence> evidenceList, String dataType, 
			String source, String containsText)
	{
		// Note that the list is ordered in DESCENDING order, so the first page
		// corresponds to the latest date in the database.
		Evidence startEvidence = m_EvidenceDAO.searchLatestEvidence(
				dataType, source, containsText);
		Date startDate = null;
		if (startEvidence != null)
		{
			startDate = startEvidence.getTime();
		}
		
		Evidence endEvidence = m_EvidenceDAO.searchEarliestEvidence(
				dataType, source, containsText);
		Date endDate = null;
		if (endEvidence != null)
		{
			endDate = endEvidence.getTime();
		}
		
		Date firstRowTime = null;
		if (evidenceList != null && evidenceList.size() > 0)
		{
			Evidence firstRow = evidenceList.get(0);
			firstRowTime = firstRow.getTime();
		}
		
		// Set the flag indicating if this is the last result for this paging config.
		// i.e. true if the result set is null/empty or the last row in the result set
		// is the record that is furthest back in time for this config.
		boolean isEarlierResults = false;
		if (evidenceList != null && evidenceList.size() > 0)
		{
			int lastIdInResults = evidenceList.get(evidenceList.size()-1).getId();
			int earliestId = endEvidence.getId();

			if (lastIdInResults != earliestId)
			{
				isEarlierResults = true;
			}
		}
		
		// Convert Evidence to EvidenceModels
		Vector<EvidenceModel> modelList = new Vector<EvidenceModel>();
		
		for (Evidence evidence : evidenceList)
		{
			EvidenceModel model = new EvidenceModel(evidence.getProperties());
			modelList.add(model);
		}
		
		return new DatePagingLoadResult<EvidenceModel>(modelList, TimeFrame.SECOND,
				firstRowTime, startDate, endDate, isEarlierResults);
	}
}
