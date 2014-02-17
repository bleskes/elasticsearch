package com.prelert.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.extjs.gxt.ui.client.Style;
import com.extjs.gxt.ui.client.data.BasePagingLoadResult;
import com.extjs.gxt.ui.client.data.PagingLoadConfig;
import com.extjs.gxt.ui.client.data.PagingLoadResult;
import com.extjs.gxt.ui.client.data.SortInfo;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import com.prelert.data.*;
import com.prelert.service.EvidenceQueryService;


public class HostedQueryServiceImpl extends RemoteServiceServlet 
	implements EvidenceQueryService
{
	
	private Connection conn;
	
	static Logger logger = Logger.getLogger(HostedQueryServiceImpl.class);
	
	private Connection m_DBConnection;
	
	
	public HostedQueryServiceImpl()
	{
		try
		{
			// Load the MySQL JDBC driver directly for this test.
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			logger.debug("Loaded MySQL JDBC driver");
		}
		catch (Exception ex)
		{
			logger.error("Error loading MySQL JDBC driver", null);
		}

		try
		{
			m_DBConnection = DriverManager.getConnection("jdbc:mysql://localhost/prelert?" +
			       	"user=root&password=root123");
		}
		catch (SQLException ex)
		{
			// handle any errors
			logger.error("Error obtaining database connection", ex);
		}
	}
	
	public DatePagingLoadResult<EventRecord> getFirstPage(EvidencePagingLoadConfig loadConfig)
	{
		TimeFrame timeFrame = loadConfig.getTimeFrame();
		
		String query = "CALL evidence_first_page()";
		logger.debug("getFirstPage() query: " + query);
		
		List<EventRecord> evidenceList = executeQuery(query);
		
		// Note that the list is ordered in DESCENDING order, so the first page
		// corresponds to the latest date in the database.
		Date startDate = getLatestDate();
		Date endDate = getEarliestDate();
		
		return new DatePagingLoadResult<EventRecord>(evidenceList, timeFrame,
				getFirstRowTime(evidenceList), startDate, endDate);
	}
	
	
	public DatePagingLoadResult<EventRecord> getLastPage(EvidencePagingLoadConfig loadConfig)
	{
		TimeFrame timeFrame = loadConfig.getTimeFrame();
		
		String query = "CALL evidence_last_page()";
		logger.debug("getLastPage() query: " + query);
		
		List<EventRecord> evidenceList = executeQuery(query);
		
		// Note that the list is ordered in DESCENDING order, so the first page
		// corresponds to the latest date in the database.
		Date startDate = getLatestDate();
		Date endDate = getEarliestDate();
		
		return new DatePagingLoadResult<EventRecord>(evidenceList, timeFrame,
				getFirstRowTime(evidenceList), startDate, endDate);
	}
	
	
	public DatePagingLoadResult<EventRecord> getNextPage(
			EvidencePagingLoadConfig loadConfig, String bottomRowId)
	{
		// CALL evidence_next_page(
	    // timeIn DATETIME, /* Time and id of bottom row */
	    // idIn BIGINT);
		
		TimeFrame timeFrame = loadConfig.getTimeFrame();
		
		String query = "CALL evidence_next_page(?, ?)";
		logger.debug("getNextPage() query: " + query);
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		ArrayList<EventRecord> evidenceList = new ArrayList<EventRecord>();
		
		try
		{
			pstmt = m_DBConnection.prepareStatement(query);
			pstmt.setTimestamp(1, new java.sql.Timestamp(loadConfig.getDate().getTime()));
			pstmt.setInt(2, Integer.parseInt(bottomRowId));
			
			rs = pstmt.executeQuery();
			
			EventRecord record;

			while (rs.next()) 
			{
				record = new EventRecord();
				
				ResultSetMetaData metaData = rs.getMetaData();

				String columnName;
				Object columnValue;
				for (int i = 1; i <= metaData.getColumnCount(); i++)
				{
					columnName = metaData.getColumnName(i);
					columnValue = rs.getObject(i);
					
					record.set(columnName, columnValue);
				}
				
				evidenceList.add(record);
			}

		}

		catch (SQLException ex)
		{
			// handle any errors
			logger.error("Error running getNextPage(PagingLoadConfig) query", ex);
		}
		finally 
		{
			// it is a good idea to release
			// resources in a finally{} block
			// in reverse-order of their creation
			// if they are no-longer needed
			if (rs != null)
			{
				try
				{
					rs.close();
				}
				catch (SQLException sqlEx)
				{
				} // ignore
				rs = null;
			}
			if (pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (SQLException sqlEx)
				{
				} // ignore
			}
		}
		
		// Note that the list is ordered in DESCENDING order, so the first page
		// corresponds to the latest date in the database.
		Date startDate = getLatestDate();
		Date endDate = getEarliestDate();
		
		return new DatePagingLoadResult<EventRecord>(evidenceList, timeFrame,
				getFirstRowTime(evidenceList), startDate, endDate);
		
	}
	
	
	public DatePagingLoadResult<EventRecord> getPreviousPage(
			EvidencePagingLoadConfig loadConfig, String topRowId)
	{
		// CALL evidence_previous_page (
	    // timeIn DATETIME,
	    // idIn BIGINT) /* Time and id of top row */ 
		
		TimeFrame timeFrame = loadConfig.getTimeFrame();
		
		String query = "CALL evidence_previous_page(?, ?)";
		logger.debug("getPreviousPage() query: " + query);
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		ArrayList<EventRecord> evidenceList = new ArrayList<EventRecord>();
		
		try
		{
			pstmt = m_DBConnection.prepareStatement(query);
			pstmt.setTimestamp(1, new java.sql.Timestamp(loadConfig.getDate().getTime()));
			pstmt.setInt(2, Integer.parseInt(topRowId));
			
			rs = pstmt.executeQuery();
			
			EventRecord record;

			while (rs.next()) 
			{
				record = new EventRecord();
				
				ResultSetMetaData metaData = rs.getMetaData();

				String columnName;
				Object columnValue;
				for (int i = 1; i <= metaData.getColumnCount(); i++)
				{
					columnName = metaData.getColumnName(i);
					columnValue = rs.getObject(i);
					
					record.set(columnName, columnValue);
				}
				
				evidenceList.add(record);
			}

		}

		catch (SQLException ex)
		{
			// handle any errors
			logger.error("Error running getPreviousPage(PagingLoadConfig) query", ex);
		}
		finally 
		{
			// it is a good idea to release
			// resources in a finally{} block
			// in reverse-order of their creation
			// if they are no-longer needed
			if (rs != null)
			{
				try
				{
					rs.close();
				}
				catch (SQLException sqlEx)
				{
				} // ignore
				rs = null;
			}
			if (pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (SQLException sqlEx)
				{
				} // ignore
			}
		}
		
		// Note that the list is ordered in DESCENDING order, so the first page
		// corresponds to the latest date in the database.
		Date startDate = getLatestDate();
		Date endDate = getEarliestDate();
		
		return new DatePagingLoadResult<EventRecord>(evidenceList, timeFrame,
				getFirstRowTime(evidenceList), startDate, endDate);

	}
	
	
	public DatePagingLoadResult<EventRecord> getForDescription(
			EvidencePagingLoadConfig loadConfig, String description)
	{
		// CALL evidence_at_description_minute(
		// timeIn DATETIME, /* The date without seconds e.g. 2009-04-16 14:47:00 */
		// descriptionIn VARCHAR(255));
		
		TimeFrame timeFrame = loadConfig.getTimeFrame();
		
		String query = "CALL evidence_at_description_minute(?, ?)";
		logger.debug("getForDescription() query: " + query);
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		ArrayList<EventRecord> evidenceList = new ArrayList<EventRecord>();
		
		try
		{
			pstmt = m_DBConnection.prepareStatement(query);
			pstmt.setDate(1, new java.sql.Date(loadConfig.getDate().getTime()));
			pstmt.setString(2, description);
			
			rs = pstmt.executeQuery();
			
			EventRecord record;

			while (rs.next()) 
			{
				record = new EventRecord();
				
				ResultSetMetaData metaData = rs.getMetaData();

				String columnName;
				Object columnValue;
				for (int i = 1; i <= metaData.getColumnCount(); i++)
				{
					columnName = metaData.getColumnName(i);
					columnValue = rs.getObject(i);
					
					record.set(columnName, columnValue);
				}
				
				evidenceList.add(record);
			}

		}

		catch (SQLException ex)
		{
			// handle any errors
			logger.error("Error running getForDescription(PagingLoadConfig) query", ex);
		}
		finally 
		{
			// it is a good idea to release
			// resources in a finally{} block
			// in reverse-order of their creation
			// if they are no-longer needed
			if (rs != null)
			{
				try
				{
					rs.close();
				}
				catch (SQLException sqlEx)
				{
				} // ignore
				rs = null;
			}
			if (pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (SQLException sqlEx)
				{
				} // ignore
			}
		}
		
		Date startDate = getEarliestDate();
		Date endDate = getLatestDate();
		
		return new DatePagingLoadResult<EventRecord>(evidenceList, timeFrame,
				loadConfig.getDate(), startDate, endDate);
	}
	
	
	public DatePagingLoadResult<EventRecord> getAtTime(EvidencePagingLoadConfig loadConfig)
	{
		// CALL evidence_at_time(
	    // timeIn DATETIME);

		
		TimeFrame timeFrame = loadConfig.getTimeFrame();
		
		String query = "CALL evidence_at_time(?)";
		logger.debug("getAtTime() query: " + query);
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		ArrayList<EventRecord> evidenceList = new ArrayList<EventRecord>();
		
		try
		{
			pstmt = m_DBConnection.prepareStatement(query);
			pstmt.setTimestamp(1, new java.sql.Timestamp(loadConfig.getDate().getTime()));
			
			rs = pstmt.executeQuery();
			
			EventRecord record;

			while (rs.next()) 
			{
				record = new EventRecord();
				
				ResultSetMetaData metaData = rs.getMetaData();

				String columnName;
				Object columnValue;
				for (int i = 1; i <= metaData.getColumnCount(); i++)
				{
					columnName = metaData.getColumnName(i);
					columnValue = rs.getObject(i);
					
					record.set(columnName, columnValue);
				}
				
				evidenceList.add(record);
			}

		}

		catch (SQLException ex)
		{
			// handle any errors
			logger.error("Error running getAtTime(PagingLoadConfig) query", ex);
		}
		finally 
		{
			// it is a good idea to release
			// resources in a finally{} block
			// in reverse-order of their creation
			// if they are no-longer needed
			if (rs != null)
			{
				try
				{
					rs.close();
				}
				catch (SQLException sqlEx)
				{
				} // ignore
				rs = null;
			}
			if (pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (SQLException sqlEx)
				{
				} // ignore
			}
		}
		
		Date startDate = getEarliestDate();
		Date endDate = getLatestDate();
		
		return new DatePagingLoadResult<EventRecord>(evidenceList, timeFrame,
				loadConfig.getDate(), startDate, endDate);
	}
	
	
	public DatePagingLoadResult<EventRecord> getIdPage(EvidencePagingLoadConfig loadConfig, int id)
	{
		// CALL evidence_at_time(
	    // timeIn DATETIME);

		
		TimeFrame timeFrame = loadConfig.getTimeFrame();
		
		String query = "CALL evidence_id_page(?);";
		logger.debug("getIdPage() query: " + query);
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		ArrayList<EventRecord> evidenceList = new ArrayList<EventRecord>();
		
		try
		{
			pstmt = m_DBConnection.prepareStatement(query);
			pstmt.setInt(1, id);
			
			rs = pstmt.executeQuery();
			
			EventRecord record;

			while (rs.next()) 
			{
				record = new EventRecord();
				
				ResultSetMetaData metaData = rs.getMetaData();

				String columnName;
				Object columnValue;
				for (int i = 1; i <= metaData.getColumnCount(); i++)
				{
					columnName = metaData.getColumnName(i);
					columnValue = rs.getObject(i);
					
					record.set(columnName, columnValue);
				}
				
				evidenceList.add(record);
			}

		}

		catch (SQLException ex)
		{
			// handle any errors
			logger.error("Error running getIdPage(PagingLoadConfig) query", ex);
		}
		finally 
		{
			// it is a good idea to release
			// resources in a finally{} block
			// in reverse-order of their creation
			// if they are no-longer needed
			if (rs != null)
			{
				try
				{
					rs.close();
				}
				catch (SQLException sqlEx)
				{
				} // ignore
				rs = null;
			}
			if (pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (SQLException sqlEx)
				{
				} // ignore
			}
		}
		
		Date startDate = getEarliestDate();
		Date endDate = getLatestDate();
		
		return new DatePagingLoadResult<EventRecord>(evidenceList, timeFrame,
				loadConfig.getDate(), startDate, endDate);
	}
	
	
	public Date getEarliestDate()
	{
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		Date minTime = new Date();
		
		try
		{
			String query = "select MIN(time) from evidence;";
			
			pstmt = m_DBConnection.prepareStatement(query);
			rs = pstmt.executeQuery();
			
			while (rs.next()) 
			{				
				minTime = rs.getTimestamp(1);
			}
		}
		catch (SQLException ex)
		{
			// handle any errors
			logger.error("Error running getEarliestDate() query", ex);
		}
		finally 
		{
			// it is a good idea to release
			// resources in a finally{} block
			// in reverse-order of their creation
			// if they are no-longer needed
			if (rs != null)
			{
				try
				{
					rs.close();
				}
				catch (SQLException sqlEx)
				{
				} // ignore
				rs = null;
			}
			if (pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (SQLException sqlEx)
				{
				} // ignore
			}
		}

	    return minTime;
	}
	
	
	public Date getLatestDate()
	{
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		Date maxTime = new Date();
		
		try
		{
			String query = "select MAX(time) from evidence;";
			
			pstmt = m_DBConnection.prepareStatement(query);
			rs = pstmt.executeQuery();
			
			while (rs.next()) 
			{				
				maxTime = rs.getTimestamp(1);
			}
		}
		catch (SQLException ex)
		{
			// handle any errors
			logger.error("Error running getEarliestDate() query", ex);
		}
		finally 
		{
			// it is a good idea to release
			// resources in a finally{} block
			// in reverse-order of their creation
			// if they are no-longer needed
			if (rs != null)
			{
				try
				{
					rs.close();
				}
				catch (SQLException sqlEx)
				{
				} // ignore
				rs = null;
			}
			if (pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (SQLException sqlEx)
				{
				} // ignore
			}
		}

	    return maxTime;
	}
	
	
    /**
     * Executes the specified query.
     */
	private List<EventRecord> executeQuery(String query)
	{
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		ArrayList<EventRecord> records = new ArrayList<EventRecord>();
		
		try
		{
			pstmt = m_DBConnection.prepareStatement(query);
			rs = pstmt.executeQuery();
			
			EventRecord record;

			while (rs.next()) 
			{
				record = new EventRecord();
				
				ResultSetMetaData metaData = rs.getMetaData();

				String columnName;
				Object columnValue;
				for (int i = 1; i <= metaData.getColumnCount(); i++)
				{
					columnName = metaData.getColumnName(i);
					columnValue = rs.getObject(i);
					
					record.set(columnName, columnValue);
				}
				
				records.add(record);
			}

		}

		catch (SQLException ex)
		{
			// handle any errors
			logger.error("Error running getStatesData(PagingLoadConfig) query", ex);
		}
		finally 
		{
			// it is a good idea to release
			// resources in a finally{} block
			// in reverse-order of their creation
			// if they are no-longer needed
			if (rs != null)
			{
				try
				{
					rs.close();
				}
				catch (SQLException sqlEx)
				{
				} // ignore
				rs = null;
			}
			if (pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (SQLException sqlEx)
				{
				} // ignore
			}
		}

	    return records;
	}
	
	
	/**
	 * Returns the time of the first row in the specified results list.
	 * @return
	 */
	private Date getFirstRowTime(List<EventRecord> evidenceList)
	{
		Date firstRowTime = null;
		if (evidenceList == null || evidenceList.size() == 0)
		{
			return null;
		}
		
		EventRecord lastRow = evidenceList.get(0);
		
		
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try
        {
			firstRowTime = dateFormatter.parse((String)lastRow.get("time"));
        }
        catch (ParseException e)
        {
	        logger.debug("getFirstRowTime() - unable to parse time value to Date: " + lastRow.get("time"));
        }
        
        return firstRowTime;
	}



	public List<GridRowInfo> getRowInfo(String databaseView, int rowId)
	{
		ArrayList<GridRowInfo> rowData = new ArrayList<GridRowInfo>();
		
		Statement stmt = null;
		ResultSet rs = null;
		try 
		{
			stmt = conn.createStatement();
			
			GWT.log("getRowInfo() running query: SELECT * FROM " + databaseView + 
					" WHERE id="+ rowId, null);
			
			rs = stmt.executeQuery("SELECT * FROM " + databaseView + " WHERE id="+ rowId);
			
			String columnName;
			String columnValue;
			
			ResultSetMetaData metaData = rs.getMetaData();
			
			while (rs.next()) 
			{
				for (int i = 1; i <= metaData.getColumnCount(); i++)
				{
					columnName = metaData.getColumnName(i);
					columnValue = rs.getString(i);
					rowData.add(new GridRowInfo(columnName, columnValue));
				}
			}

		}

		catch (SQLException ex)
		{
			// handle any errors
			GWT.log("Error running getRowInfo() query", ex);
		}
		finally 
		{
			// it is a good idea to release
			// resources in a finally{} block
			// in reverse-order of their creation
			// if they are no-longer needed
			if (rs != null)
			{
				try
				{
					rs.close();
				}
				catch (SQLException sqlEx)
				{
				} // ignore
				rs = null;
			}
			if (stmt != null)
			{
				try
				{
					stmt.close();
				}
				catch (SQLException sqlEx)
				{
				} // ignore
			}
		}	

		return rowData;
	}


	public List<String> getAllColumns(String databaseView)
	{
		ArrayList<String> columns = new ArrayList<String>();
		
		Statement stmt = null;
		ResultSet rs = null;
		try 
		{
			stmt = conn.createStatement();
			
			GWT.log("getTableColumns() running query: desc evidence", null);
			
			rs = stmt.executeQuery("desc " + databaseView);
			
			while (rs.next()) 
			{
				columns.add(rs.getString("Field"));
			}

		}

		catch (SQLException ex)
		{
			// handle any errors
			GWT.log("Error running getTableColumns() query", ex);
		}
		finally 
		{
			// it is a good idea to release
			// resources in a finally{} block
			// in reverse-order of their creation
			// if they are no-longer needed
			if (rs != null)
			{
				try
				{
					rs.close();
				}
				catch (SQLException sqlEx)
				{
				} // ignore
				rs = null;
			}
			if (stmt != null)
			{
				try
				{
					stmt.close();
				}
				catch (SQLException sqlEx)
				{
				} // ignore
			}
		}	

		return columns;
	}
	
	
	public List<String> getColumnValues(String columnName)
	{
		ArrayList<String> columns = new ArrayList<String>();
		
		Statement stmt = null;
		ResultSet rs = null;
		try 
		{
			stmt = conn.createStatement();
			
			StringBuilder query = new StringBuilder("select distinct ");
			query.append(columnName);
			query.append(" from evidence_second_view ORDER BY ");
			query.append(columnName);
			query.append(" ASC");
			
			logger.debug("getColumnValues() running query: desc evidence");
			
			rs = stmt.executeQuery(query.toString());
			
			while (rs.next()) 
			{
				columns.add(rs.getString(columnName));
			}

		}

		catch (SQLException ex)
		{
			// handle any errors
			GWT.log("Error running getTableColumns() query", ex);
		}
		finally 
		{
			// it is a good idea to release
			// resources in a finally{} block
			// in reverse-order of their creation
			// if they are no-longer needed
			if (rs != null)
			{
				try
				{
					rs.close();
				}
				catch (SQLException sqlEx)
				{
				} // ignore
				rs = null;
			}
			if (stmt != null)
			{
				try
				{
					stmt.close();
				}
				catch (SQLException sqlEx)
				{
				} // ignore
			}
		}	

		return columns;
	}


    public ListView getDrillDownView(ViewTool menuItem, EventRecord record)
    {
	    // TODO Auto-generated method stub
	    return null;
    }


    public ListView getProbableCauseView(EventRecord record)
    {
	    // TODO Auto-generated method stub
	    return null;
    }



	
}
