package com.prelert.server;

import java.util.ArrayList;
import java.util.List;

import com.extjs.gxt.ui.client.Style;
import com.extjs.gxt.ui.client.data.PagingLoadConfig;
import com.extjs.gxt.ui.client.data.SortInfo;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import org.springframework.transaction.*;
import org.springframework.transaction.support.*;

import org.apache.log4j.Logger;

import com.prelert.dao.*;
import com.prelert.data.*;
import com.prelert.data.gxt.GridRowInfo;
import com.prelert.service.ListViewQueryService;


/**
 * Server-side implementation of the service for retrieving data for generic
 * List Views from the Prelert database.
 * @author Pete Harverson
 */
public class ListViewQueryServiceImpl extends RemoteServiceServlet 
	implements ListViewQueryService
{
	static Logger logger = Logger.getLogger(ListViewQueryServiceImpl.class);
	
	private ListViewDAO m_ListViewDAO;
	private TransactionTemplate	m_TxTemplate;
	
	
	/**
	 * Sets the ListViewDAO to be used by the List View query service.
	 * @param listViewDAO the data access object for list views.
	 */
	public void setListViewDAO(ListViewDAO listViewDAO)
	{
		m_ListViewDAO = listViewDAO;
	}
	
	
	/**
	 * Returns the ListViewDAO being used by the List View query service.
	 * @return the data access object for list views.
	 */
	public ListViewDAO getListViewDAO()
	{
		return m_ListViewDAO;
	}
	
	
	/**
	 * Sets the transaction manager to be used when running queries and updates
	 * to the Prelert database within transactions.
	 * @param txManager Spring PlatformTransactionManager to manage database transactions.
	 */
	public void setTransactionManager(PlatformTransactionManager txManager)
	{
		m_TxTemplate = new TransactionTemplate(txManager);
		m_TxTemplate.setReadOnly(true);
		m_TxTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
	}
	
	
	/**
	 * Returns a list of all the columns in the database table linked to the 
	 * specified List View.
	 * @param view ListView for which to obtain a list of the columns.
	 * @return list of all the columns in the specified list view.
	 */
	public List<String> getAllColumns(ListView view)
	{
		return m_ListViewDAO.getAllColumns(view);
	}
	
	
	/**
	 * Returns the records for the view specified by the supplied paging load 
	 * configuration object.
	 * @param view the List View for which to obtain the records.
	 * @param config the paging load config, which defines the page offset, size
	 * and optional sort information.
	 * @return the loading result for the List View page.
	 */
	public ListViewLoadResult<EventRecord> getRecords(ListView view, PagingLoadConfig config)
	{
		List<SortInformation> orderBy = null;
		SortInfo sortInfo = config.getSortInfo();
		
		if (sortInfo != null && sortInfo.getSortField() != null)
		{
			if (sortInfo.getSortDir() != Style.SortDir.NONE)
			{
				orderBy = new ArrayList<SortInformation>();
				SortInformation sortInformation = new SortInformation();
				sortInformation.setColumnName(sortInfo.getSortField());
				
				Style.SortDir sortDir = sortInfo.getSortDir();
				if (sortDir == Style.SortDir.ASC)
				{
					sortInformation.setSortDirection(SortInformation.SortDirection.ASC);
				}
				else
				{
					sortInformation.setSortDirection(SortInformation.SortDirection.DESC);
				}
				orderBy.add(sortInformation);
			}
		}
		else
		{
			// Supply default sort specified in the view (if any).
			orderBy = view.getDefaultOrderBy();
		}
		
		// Run the DB queries within a transaction.
		final ListView listView = view;
		final PagingLoadConfig pageConfig = config;
		final List<SortInformation> listOrderBy = orderBy;
		Object pagingLoadResult = m_TxTemplate.execute(new TransactionCallback(){

            public ListViewLoadResult<EventRecord> doInTransaction(TransactionStatus status)
            {
            	// Get the total number of rows in this view - needed for the PagingLoadResult.
        		int rowCount = m_ListViewDAO.getTotalRowCount(listView);
            					
				// Get the records for the load result.
				List<EventRecord> eventData = m_ListViewDAO.getRecords(listView, pageConfig.getOffset(), pageConfig.getLimit(), listOrderBy);
				
				return new ListViewLoadResult<EventRecord>(eventData, pageConfig.getOffset(), rowCount, -1);
            }
			
		});
		
		return (ListViewLoadResult<EventRecord>)(pagingLoadResult);
	}
	
	
	public ListViewLoadResult<EventRecord> getRecordsWithRow(ListView view, int limit, 
			String selectRowFilterAttribute, String selectRowFilterValue)
	{	
		// Run all the DB queries within a transaction.
		final ListView listView = view;
		final String selectRowFilterAttr = selectRowFilterAttribute;
		final String selectRowFilterVal = selectRowFilterValue;
		final int qryLimit = limit;

		Object pagingLoadResult = m_TxTemplate.execute(new TransactionCallback(){

            public ListViewLoadResult<EventRecord> doInTransaction(TransactionStatus status)
            {
            	// Get the total number of rows in this view - needed for the PagingLoadResult.
        		int rowCount = m_ListViewDAO.getTotalRowCount(listView);
            	
        		// Get the row number corresponding to the selected row filter.
				int index = getRowNumber(listView, selectRowFilterAttr, selectRowFilterVal);

				int loadResultRowIndex = -1;
				int offset = 0;
				if (index != -1)
				{
					// Get the index to select in the result set.
					loadResultRowIndex = index % qryLimit;
				
					// Get the offset relating to this row index.
					offset = (index/qryLimit) * qryLimit;
				}
				
				List<EventRecord> eventData = m_ListViewDAO.getRecords(listView, offset, 
					qryLimit, listView.getDefaultOrderBy());
				
				return new ListViewLoadResult<EventRecord>(eventData, offset, rowCount, loadResultRowIndex);
				
            }
			
		});
		
		return (ListViewLoadResult<EventRecord>)(pagingLoadResult);
	}
	

	public List<GridRowInfo> getRowInfo(ListView view, int id)
	{
		return m_ListViewDAO.getRowInfo(view, id);
	}
	
	
	public int getRowNumber(ListView view, String selectRowFilterAttribute, 
			String selectRowFilterValue)
	{
		return m_ListViewDAO.getRowNumber(view, selectRowFilterAttribute, 
				selectRowFilterValue, view.getDefaultOrderBy());
	}
	
}
