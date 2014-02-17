package demo.app.server;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import demo.app.client.EvidenceLog;
import demo.app.client.ListViewQueryService;
import demo.app.client.TimeFrame;
import demo.app.data.DatePagingLoadConfig;
import demo.app.data.DatePagingLoadResult;
import demo.app.data.UsageRecord;


/**
 * Server-side implementation of the service for retrieving evidence data from the
 * Prelert database for display in evidence list views.
 * @author Pete Harverson
 */
public class ListViewQueryServiceImpl extends RemoteServiceServlet
	implements ListViewQueryService
{
	static Logger logger = Logger.getLogger(ListViewQueryServiceImpl.class);
	
	private Connection m_DBConnection;
	
	
	public ListViewQueryServiceImpl()
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
	
	public DatePagingLoadResult<EvidenceLog> getFirstPage(DatePagingLoadConfig loadConfig)
	{
		TimeFrame timeFrame = loadConfig.getTimeFrame();
		
		String query = "CALL evidence_first_page()";
		logger.debug("getFirstPage() query: " + query);
		
		List<EvidenceLog> evidenceList = executeQuery(query);
		
		// Note that the list is ordered in DESCENDING order, so the first page
		// corresponds to the latest date in the database.
		Date startDate = getLatestDate();
		Date endDate = getEarliestDate();
		
		return new DatePagingLoadResult<EvidenceLog>(evidenceList, timeFrame,
				getFirstRowTime(evidenceList), startDate, endDate);
	}
	
	
	public DatePagingLoadResult<EvidenceLog> getLastPage(DatePagingLoadConfig loadConfig)
	{
		TimeFrame timeFrame = loadConfig.getTimeFrame();
		
		String query = "CALL evidence_last_page()";
		logger.debug("getLastPage() query: " + query);
		
		List<EvidenceLog> evidenceList = executeQuery(query);
		
		// Note that the list is ordered in DESCENDING order, so the first page
		// corresponds to the latest date in the database.
		Date startDate = getLatestDate();
		Date endDate = getEarliestDate();
		
		return new DatePagingLoadResult<EvidenceLog>(evidenceList, timeFrame,
				getFirstRowTime(evidenceList), startDate, endDate);
	}
	
	
	public DatePagingLoadResult<EvidenceLog> getNextPage(
			DatePagingLoadConfig loadConfig, int bottomRowId)
	{
		// CALL evidence_next_page(
	    // timeIn DATETIME, /* Time and id of bottom row */
	    // idIn BIGINT);
		
		TimeFrame timeFrame = loadConfig.getTimeFrame();
		
		String query = "CALL evidence_next_page(?, ?)";
		logger.debug("getNextPage() query: " + query);
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		ArrayList<EvidenceLog> evidenceList = new ArrayList<EvidenceLog>();
		
		try
		{
			pstmt = m_DBConnection.prepareStatement(query);
			pstmt.setTimestamp(1, new java.sql.Timestamp(loadConfig.getDate().getTime()));
			pstmt.setInt(2, bottomRowId);
			
			rs = pstmt.executeQuery();
			
			EvidenceLog record;

			while (rs.next()) 
			{
				record = new EvidenceLog();
				
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
		
		return new DatePagingLoadResult<EvidenceLog>(evidenceList, timeFrame,
				getFirstRowTime(evidenceList), startDate, endDate);
		
	}
	
	
	public DatePagingLoadResult<EvidenceLog> getPreviousPage(
			DatePagingLoadConfig loadConfig, int topRowId)
	{
		// CALL evidence_previous_page (
	    // timeIn DATETIME,
	    // idIn BIGINT) /* Time and id of top row */ 
		
		TimeFrame timeFrame = loadConfig.getTimeFrame();
		
		String query = "CALL evidence_previous_page(?, ?)";
		logger.debug("getPreviousPage() query: " + query);
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		ArrayList<EvidenceLog> evidenceList = new ArrayList<EvidenceLog>();
		
		try
		{
			pstmt = m_DBConnection.prepareStatement(query);
			pstmt.setTimestamp(1, new java.sql.Timestamp(loadConfig.getDate().getTime()));
			pstmt.setInt(2, topRowId);
			
			rs = pstmt.executeQuery();
			
			EvidenceLog record;

			while (rs.next()) 
			{
				record = new EvidenceLog();
				
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
		
		return new DatePagingLoadResult<EvidenceLog>(evidenceList, timeFrame,
				getFirstRowTime(evidenceList), startDate, endDate);

	}
	
	
	public DatePagingLoadResult<EvidenceLog> getForDescription(
			DatePagingLoadConfig loadConfig, String description)
	{
		// CALL evidence_at_description_minute(
		// timeIn DATETIME, /* The date without seconds e.g. 2009-04-16 14:47:00 */
		// descriptionIn VARCHAR(255));
		
		TimeFrame timeFrame = loadConfig.getTimeFrame();
		
		String query = "CALL evidence_at_description_minute(?, ?)";
		logger.debug("getForDescription() query: " + query);
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		ArrayList<EvidenceLog> evidenceList = new ArrayList<EvidenceLog>();
		
		try
		{
			pstmt = m_DBConnection.prepareStatement(query);
			pstmt.setDate(1, new java.sql.Date(loadConfig.getDate().getTime()));
			pstmt.setString(2, description);
			
			rs = pstmt.executeQuery();
			
			EvidenceLog record;

			while (rs.next()) 
			{
				record = new EvidenceLog();
				
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
		
		// Note that the list is ordered in DESCENDING order, so the first page
		// corresponds to the latest date in the database.
		Date startDate = getLatestDate();
		Date endDate = getEarliestDate();
		
		return new DatePagingLoadResult<EvidenceLog>(evidenceList, timeFrame,
				loadConfig.getDate(), startDate, endDate);
	}
	
	
	public DatePagingLoadResult<EvidenceLog> getAtTime(DatePagingLoadConfig loadConfig)
	{
		// CALL evidence_at_time(
	    // timeIn DATETIME);

		
		TimeFrame timeFrame = loadConfig.getTimeFrame();
		
		String query = "CALL evidence_at_time(?)";
		logger.debug("getAtTime() query: " + query);
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		ArrayList<EvidenceLog> evidenceList = new ArrayList<EvidenceLog>();
		
		try
		{
			pstmt = m_DBConnection.prepareStatement(query);
			pstmt.setTimestamp(1, new java.sql.Timestamp(loadConfig.getDate().getTime()));
			
			rs = pstmt.executeQuery();
			
			EvidenceLog record;

			while (rs.next()) 
			{
				record = new EvidenceLog();
				
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
		
		// Note that the list is ordered in DESCENDING order, so the first page
		// corresponds to the latest date in the database.
		Date startDate = getLatestDate();
		Date endDate = getEarliestDate();
		
		return new DatePagingLoadResult<EvidenceLog>(evidenceList, timeFrame,
				getFirstRowTime(evidenceList), startDate, endDate);
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
     * 
     */
	private List<EvidenceLog> executeQuery(String query)
	{
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		ArrayList<EvidenceLog> records = new ArrayList<EvidenceLog>();
		
		try
		{
			pstmt = m_DBConnection.prepareStatement(query);
			rs = pstmt.executeQuery();
			
			EvidenceLog record;

			while (rs.next()) 
			{
				record = new EvidenceLog();
				
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
	private Date getFirstRowTime(List<EvidenceLog> evidenceList)
	{
		Date firstRowTime = null;
		if (evidenceList == null || evidenceList.size() == 0)
		{
			return null;
		}
		
		EvidenceLog lastRow = evidenceList.get(0);
		
		
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
	
}
