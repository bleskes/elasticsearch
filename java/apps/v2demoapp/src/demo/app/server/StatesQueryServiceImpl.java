package demo.app.server;

import java.sql.*;
import java.util.*;

import com.extjs.gxt.ui.client.Style;
import com.extjs.gxt.ui.client.data.*;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import demo.app.dao.EvidenceDAO;
import demo.app.data.gxt.EvidenceModel;
import demo.app.data.gxt.GridRowInfo;
import demo.app.service.StatesQueryService;


import org.apache.log4j.Logger;

public class StatesQueryServiceImpl extends RemoteServiceServlet 
		implements StatesQueryService
{
	static Logger logger = Logger.getLogger(StatesQueryServiceImpl.class);
	
	private Connection conn;
	private EvidenceDAO m_EvidenceDAO;


	public StatesQueryServiceImpl()
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
			conn = DriverManager.getConnection("jdbc:mysql://localhost/prelert?" +
			       	"user=root&password=root123");
		}
		catch (SQLException ex)
		{
			// handle any errors
			logger.error("Error obtaining database connection", ex);
		}
	}
	
	
	public PagingLoadResult<EvidenceModel> getEvidenceData(PagingLoadConfig config)
	{
		ArrayList<EvidenceModel> evidenceData = new ArrayList<EvidenceModel>();
		
		// Build the query, adding in an ORDER BY clause if required.
		// SELECT description, entity, severity, cond_prob FROM evidence LIMIT offset, limit;
		StringBuffer evidenceQryBuff = new StringBuffer("SELECT id, description, source, severity FROM evidence ");
		
		SortInfo sortInfo = config.getSortInfo();
		if (sortInfo != null && sortInfo.getSortDir() != Style.SortDir.NONE)
		{
			evidenceQryBuff.append("ORDER BY ");
			evidenceQryBuff.append(sortInfo.getSortField());
			evidenceQryBuff.append(' ');
			evidenceQryBuff.append(sortInfo.getSortDir());
			evidenceQryBuff.append(' ');
		}
		
		evidenceQryBuff.append("LIMIT ");
		evidenceQryBuff.append(config.getOffset());
		evidenceQryBuff.append(',');
		evidenceQryBuff.append(config.getLimit());
		
		String evidenceQry = evidenceQryBuff.toString();
		
		int rowCount = 0;
		
		Statement stmt = null;
		ResultSet rs = null;
		try 
		{
			logger.debug("getEvidenceData() running query: " + evidenceQry);
			
			stmt = conn.createStatement();
			
			// Get total number of rows.
			rs = stmt.executeQuery("SELECT count(*) as numrows from evidence");
			while (rs.next()) 
			{
				rowCount = rs.getInt("numrows");
			}
			
			rs = stmt.executeQuery(evidenceQry);
			
			int id;
			String source;
			String description;
			String severity;
			EvidenceModel evidence;
			
			while (rs.next()) 
			{
				id = rs.getInt("id");
				source = rs.getString("source");
				description = rs.getString("description");
				severity = rs.getString("severity");
				evidence = new EvidenceModel();
				evidence.set("id", id);
				evidence.set("source", source);
				evidence.set("description", description);
				evidence.set("severity", severity);
				
				evidenceData.add(evidence);
			}
		}

		catch (SQLException ex)
		{
			// handle any errors
			logger.error("Error running getEvidenceData(PagingLoadConfig) query", ex);
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
		
		return new BasePagingLoadResult(evidenceData, config.getOffset(), rowCount);
		
	}
	
	
	public PagingLoadResult<EvidenceModel> getProbableCauseData(PagingLoadConfig config)
	{
		ArrayList<EvidenceModel> statesData = new ArrayList<EvidenceModel>();
		
		// Build the query, adding in an ORDER BY clause if required.
		// SELECT description, entity, severity, cond_prob FROM states LIMIT offset, limit;
		//StringBuffer statesQryBuff = new StringBuffer("SELECT description, source, severity FROM probable_cause_view ");
		StringBuffer statesQryBuff = new StringBuffer("SELECT * FROM probable_cause_view ");
		
		SortInfo sortInfo = config.getSortInfo();
		if (sortInfo != null && sortInfo.getSortDir() != Style.SortDir.NONE)
		{
			statesQryBuff.append("ORDER BY ");
			statesQryBuff.append(sortInfo.getSortField());
			statesQryBuff.append(' ');
			statesQryBuff.append(sortInfo.getSortDir());
			statesQryBuff.append(' ');
		}
		
		statesQryBuff.append("LIMIT ");
		statesQryBuff.append(config.getOffset());
		statesQryBuff.append(',');
		statesQryBuff.append(config.getLimit());
		
		String statesQry = statesQryBuff.toString();
		
		int rowCount = 0;
		
		Statement stmt = null;
		ResultSet rs = null;
		try 
		{
			logger.debug("getProbableCauseData() running query: " + statesQry);
			
			stmt = conn.createStatement();
			
			// Get total number of rows.
			rs = stmt.executeQuery("SELECT count(*) as numrows from probable_cause_view");
			while (rs.next()) 
			{
				rowCount = rs.getInt("numrows");
			}
			
			rs = stmt.executeQuery(statesQry);
			
			EvidenceModel evidence;
			String source;
			String description;
			String severity;
			
			while (rs.next()) 
			{
				evidence = new EvidenceModel();
				
				ResultSetMetaData metaData = rs.getMetaData();

				String columnName;
				Object columnValue;
				for (int i = 1; i <= metaData.getColumnCount(); i++)
				{
					columnName = metaData.getColumnName(i);
					columnValue = rs.getObject(i);
					
					evidence.set(columnName, columnValue);
				}
				
				statesData.add(evidence);
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
		
		return new BasePagingLoadResult(statesData, config.getOffset(), rowCount);
		
	}
	
	
	public List<GridRowInfo> getRowInfo(int rowId)
	{
		ArrayList<GridRowInfo> rowData = new ArrayList<GridRowInfo>();
		
		Statement stmt = null;
		ResultSet rs = null;
		try 
		{
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT * FROM evidence WHERE id="+ rowId);
			
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
			logger.error("Error running getRowInfo() query", ex);
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
	
	
	public List<EvidenceModel> getWeeklyAggregate()
	{
		ArrayList<EvidenceModel> weeklyData = new ArrayList<EvidenceModel>();
		
		Statement stmt = null;
		ResultSet rs = null;
		try 
		{
			stmt = conn.createStatement();
			rs = stmt.executeQuery("select description, sum(count) as sum from evidence_week_view group by description");
			
			String columnName;
			String columnValue;
			EvidenceModel record;
			
			ResultSetMetaData metaData = rs.getMetaData();
			
			while (rs.next()) 
			{
				record = new EvidenceModel();
				for (int i = 1; i <= metaData.getColumnCount(); i++)
				{
					columnName = metaData.getColumnName(i);
					columnValue = rs.getString(i);
					record.set(columnName, columnValue);
				}
				
				weeklyData.add(record);
			}

		}

		catch (SQLException ex)
		{
			// handle any errors
			logger.error("Error running getWeeklyAggregate() query", ex);
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

		return weeklyData;
	}
	
	
	public List<String> getTableColumns(String tableName)
	{
		//return m_EvidenceDAO.getTableColumns();

		ArrayList<String> columns = new ArrayList<String>();
		
		Statement stmt = null;
		ResultSet rs = null;
		try 
		{
			stmt = conn.createStatement();
			rs = stmt.executeQuery("desc " + tableName);
			
			while (rs.next()) 
			{
				columns.add(rs.getString("Field"));
			}

		}

		catch (SQLException ex)
		{
			// handle any errors
			logger.error("Error running getTableColumns() query", ex);
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
	
	
	public void setEvidenceDAO(EvidenceDAO evidenceDAO)
	{
		m_EvidenceDAO = evidenceDAO;
	}
	
	
	public EvidenceDAO getEvidenceDAO()
	{
		return m_EvidenceDAO;
	}
	
}
