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

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.prelert.data.DatePagingLoadConfig;
import com.prelert.data.DatePagingLoadResult;
import com.prelert.data.EventRecord;
import com.prelert.data.ExceptionPagingLoadConfig;
import com.prelert.data.TimeFrame;
import com.prelert.service.ExceptionQueryService;

public class HostedExceptionQueryServiceImpl extends RemoteServiceServlet 
	implements ExceptionQueryService
{
	
	static Logger logger = Logger.getLogger(HostedExceptionQueryServiceImpl.class);
	
	private Connection m_DBConnection;
	
	
	public HostedExceptionQueryServiceImpl()
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
	

	@Override
	public List<String> getAllColumns(String databaseView)
	{
		ArrayList<String> columns = new ArrayList<String>();
		
		Statement stmt = null;
		ResultSet rs = null;
		try 
		{
			stmt = m_DBConnection.createStatement();
			
			logger.debug("getAllColumns() running query: desc evidence_second_view");
			
			rs = stmt.executeQuery("desc " + databaseView);
			
			while (rs.next()) 
			{
				columns.add(rs.getString("Field"));
			}

		}

		catch (SQLException ex)
		{
			// handle any errors
			logger.debug("Error running getTableColumns() query", ex);
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





	@Override
	public DatePagingLoadResult<EventRecord> getFirstPage(
	        ExceptionPagingLoadConfig loadConfig)
	{
		int noiseLevel = loadConfig.getNoiseLevel();
		TimeFrame timeWindow = loadConfig.getTimeWindow();
		
	
		String query = "CALL exception_first_page(?, ?)";
		logger.debug("getFirstPage() query: " + query);
		
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		ArrayList<EventRecord> evidenceList = new ArrayList<EventRecord>();
		
		try
		{
			pstmt = m_DBConnection.prepareStatement(query);
			pstmt.setInt(1, noiseLevel);
			pstmt.setString(2, timeWindow.toString());
			
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
			logger.error("Error running query " + query, ex);
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
		TimeFrame timeFrame = loadConfig.getTimeFrame();
		Date startDate = getLatestDate(noiseLevel, timeWindow);
		Date endDate = getEarliestDate(noiseLevel, timeWindow);
		
		return new DatePagingLoadResult<EventRecord>(evidenceList, timeFrame,
				getFirstRowTime(evidenceList), startDate, endDate);
	}


	@Override
	public DatePagingLoadResult<EventRecord> getLastPage(
	        ExceptionPagingLoadConfig config)
	{
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public DatePagingLoadResult<EventRecord> getNextPage(
	        ExceptionPagingLoadConfig config, String bottomRowId)
	{
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public DatePagingLoadResult<EventRecord> getPreviousPage(
	        ExceptionPagingLoadConfig config, String topRowId)
	{
		// TODO Auto-generated method stub
		return null;
	}

	
	
	@Override
	public DatePagingLoadResult<EventRecord> getAtTime(
	        ExceptionPagingLoadConfig config)
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	

	
	public Date getEarliestDate(int noiseLevel, TimeFrame timeWindow)
	{
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		Date minTime = new Date();
		
		try
		{
			String query = "CALL exception_min_time(?, ?);";
			
			pstmt = m_DBConnection.prepareStatement(query);
			pstmt.setInt(1, noiseLevel);
			pstmt.setString(2, timeWindow.toString());
			
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
	
	
	public Date getLatestDate(int noiseLevel, TimeFrame timeWindow)
	{
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		Date maxTime = new Date();
		
		try
		{
			String query = "CALL exception_max_time(?, ?)";
			
			pstmt = m_DBConnection.prepareStatement(query);
			pstmt.setInt(1, noiseLevel);
			pstmt.setString(2, timeWindow.toString());
			
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
	 * Returns the time of the first row in the specified results list.
	 * @return the time of the first row, or <code>null</code> if the
	 * supplied list is <code>null</code> or empty.
	 */
	private Date getFirstRowTime(List<EventRecord> evidenceList)
	{
		Date firstRowTime = null;
		if (evidenceList == null || evidenceList.size() == 0)
		{
			return null;
		}
		
		EventRecord firstRow = evidenceList.get(0);
		String timeStr = firstRow.getTime(TimeFrame.SECOND);
			
		try
        {
			SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			firstRowTime = dateFormatter.parse(timeStr);
        }
        catch (ParseException e)
        {
	        logger.debug("getFirstRowTime() - unable to parse time value to Date: " + timeStr);
        }
        
        return firstRowTime;
	}
	
}
