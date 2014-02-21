package demo.app.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import demo.app.data.TimeFrame;
import demo.app.data.gxt.EvidenceModel;

public class ExceptionDAO
{
	static Logger logger = Logger.getLogger(ExceptionDAO.class);
	static SimpleDateFormat s_LogDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	private SimpleJdbcTemplate	m_SimpleJdbcTemplate;
	
	
	/**
	 * Sets the data source to be used for connections to the Prelert database.
	 * @param dataSource
	 */
	public void setDataSource(DataSource dataSource)
	{
		m_SimpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
	}
	
	
	/**
	 * Returns a list of all of the column names for evidence data of the specified type.
	 * @param dataType identifier for the type of evidence data.
	 * @return List of column names for evidence data of the specified type.
	 */
	public List<String> getAllColumns(String dataType)
	{
		// PROC is: call display_columns(type, getCompulsory, getOptional)
		String descQry = "call display_columns(?, 1, 1);";
		logger.debug("getAllColumns() query: " + descQry);
		
		ParameterizedRowMapper<String> mapper = new ParameterizedRowMapper<String>(){

			@Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException
            {
				return rs.getString(1);
            }
		};
		
		return m_SimpleJdbcTemplate.query(descQry, mapper, dataType);
	}
	
	
	/**
	 * Returns the first page of evidence data for a view with the specified
	 * time frame.
	 * @param dataType identifier for the type of evidence data.
	 * @param timeFrame time frame for which to obtain the first page of records.
	 * @return List of Evidence records comprising the first page of data.
	 */
	public List<EvidenceModel> getFirstPage(
			String dataType, Date time, int noiseLevel, TimeFrame timeWindow)
	{
		String query = "CALL exception_first_page(?, ?, ?, ?)";
		
		Timestamp timeStamp = null;
		if (time != null)
		{
			timeStamp = new Timestamp(time.getTime());
		}
		
		String debugQuery = "CALL exception_first_page({0}, {1}, {2}, {3})";
		debugQuery = MessageFormat.format(
				debugQuery, dataType, s_LogDateFormatter.format(timeStamp), noiseLevel, timeWindow.toString());
		logger.debug("getFirstPage() query: " + debugQuery);
		
		List<EvidenceModel> evidenceList = m_SimpleJdbcTemplate.query(query, new EventRecordRowMapper(), 
				dataType, timeStamp, noiseLevel, timeWindow.toString());
		
		return evidenceList;
	}
	
	
	/**
	 * Returns the last page of evidence data for a view with the specified
	 * time frame.
	 * @param dataType identifier for the type of evidence data.
	 * @param timeFrame time frame for which to obtain the last page of records.
	 * @return List of Evidence records comprising the last page of data.
	 */
	public List<EvidenceModel> getLastPage(String dataType, Date time, int noiseLevel, TimeFrame timeWindow)
	{	
		String query = "CALL exception_last_page(?, ?, ?, ?)";
		
		Timestamp timeStamp = null;
		if (time != null)
		{
			timeStamp = new Timestamp(time.getTime());
		}
		
		String debugQuery = "CALL exception_last_page({0}, {1}, {2}, {3})";
		debugQuery = MessageFormat.format(
				debugQuery, dataType, s_LogDateFormatter.format(timeStamp), noiseLevel, timeWindow.toString());
		logger.debug("getLastPage() query: " + debugQuery);
		
		List<EvidenceModel> evidenceList = m_SimpleJdbcTemplate.query(query, new EventRecordRowMapper(), 
				dataType, timeStamp, noiseLevel, timeWindow.toString());
		
		return evidenceList;
		
	}
	
	
	/**
	 * Returns the next page of evidence data following on from the row with the
	 * specified time and id.
	 * @param dataType identifier for the type of evidence data.
	 * @param timeFrame time frame for which to obtain the next page of records.
	 * @param bottomRowTime the value of the time column for the bottom row of 
	 * evidence in the current page.
	 * @param bottomRowId the value of the id column for the bottom row of evidence 
	 * in the current page.
	 * @return List of Evidence records comprising the next page of data.
	 */
	public List<EvidenceModel> getNextPage(String dataType,
			Date bottomRowTime, String bottomRowId,
			int noiseLevel, TimeFrame timeWindow)
	{
		String query = "CALL exception_next_page(?, ?, ?, ?, ?)";
		
		Timestamp bottomRowTimeStamp = new Timestamp(bottomRowTime.getTime());
		
		String debugQuery = "CALL exception_next_page({0}, {1}, {2}, {3}, {4})";
		debugQuery = MessageFormat.format(debugQuery, 
				dataType, s_LogDateFormatter.format(bottomRowTimeStamp), 
				Integer.parseInt(bottomRowId), noiseLevel, timeWindow.toString());
		logger.debug("getNextPage() query: " + debugQuery);

		List<EvidenceModel> evidenceList = m_SimpleJdbcTemplate.query(query, new EventRecordRowMapper(), 
						dataType, bottomRowTimeStamp, Integer.parseInt(bottomRowId), 
						noiseLevel, timeWindow.toString());

		return evidenceList;
		
	}
	
	
	/**
	 * Returns the previous page of evidence data to the row with the specified 
	 * time and id.
	 * @param dataType identifier for the type of evidence data.
	 * @param timeFrame time frame for which to obtain the previous page of records.
	 * @param topRowTime the value of the time column for the top row of 
	 * evidence in the current page.
	 * @param topRowId the value of the id column for the top row of evidence 
	 * in the current page.
	 * @return List of Evidence records comprising the previous page of data.
	 */
	public List<EvidenceModel> getPreviousPage(String dataType,
			Date topRowTime, String topRowId,
			int noiseLevel, TimeFrame timeWindow)
	{
		String query = "CALL exception_previous_page(?, ?, ?, ?, ?)";
		
		Timestamp topRowTimeStamp = new Timestamp(topRowTime.getTime());
		
		String debugQuery = "CALL exception_previous_page({0}, {1}, {2}, {3}, {4})";
		debugQuery = MessageFormat.format(debugQuery, dataType, s_LogDateFormatter.format(topRowTimeStamp), 
				Integer.parseInt(topRowId), noiseLevel, timeWindow.toString());
		logger.debug("getPreviousPage() query: " + debugQuery);

		List<EvidenceModel> evidenceList = m_SimpleJdbcTemplate.query(query, new EventRecordRowMapper(), 
				dataType, topRowTimeStamp, Integer.parseInt(topRowId), noiseLevel, timeWindow.toString());

		return evidenceList;
	}
	
	
	/**
	 * Returns a page of evidence data, whose top row will match the supplied time.
	 * @param dataType identifier for the type of evidence data.
	 * @param timeFrame time frame for which to obtain evidence records.
	 * @param time value of the time column to match on for the first row of 
	 * evidence data that will be returned.
	 * @return List of Evidence records, sorted descending on time.
	 */
	public List<EvidenceModel> getAtTime(String dataType, Date time, int noiseLevel, TimeFrame timeWindow)
	{
		String query = "CALL exception_at_time(?, ?, ?, ?)";
		
		Timestamp timeStamp = new Timestamp(time.getTime());	
		
		String debugQuery = "CALL exception_at_time({0}, {1}, {2}, {3})";
		debugQuery = MessageFormat.format(debugQuery, dataType, s_LogDateFormatter.format(timeStamp), 
				noiseLevel, timeWindow.toString());
		logger.debug("getAtTime() query: " + debugQuery);
		
		List<EvidenceModel> evidenceList = m_SimpleJdbcTemplate.query(query, 
				new EventRecordRowMapper(), dataType, timeStamp, noiseLevel, timeWindow.toString());
		
		return evidenceList;
	}
	
	
	/**
	 * Returns the date of the earliest evidence record in the Prelert database
	 * for the given noise level and time window.
	 * @param dataType identifier for the type of evidence data.
	 * @return date of earliest evidence record.
	 */
	public Date getEarliestDate(String dataType, Date time, int noiseLevel, TimeFrame timeWindow)
	{		
		String query = "CALL exception_min_time(?, ?, ?, ?)";
		
		Timestamp timeStamp = null;
		if (time != null)
		{
			timeStamp = new Timestamp(time.getTime());	
		}
		
		return m_SimpleJdbcTemplate.queryForObject(query.toString(), 
				java.sql.Timestamp.class, dataType, timeStamp, noiseLevel, timeWindow.toString());
	}

	
	/**
	 * Returns the date of the latest evidence record in the Prelert database
	 * for the given noise level and time window.
	 * @param dataType identifier for the type of evidence data.
	 * @return date of latest evidence record.
	 */
	public Date getLatestDate(String dataType, Date time, int noiseLevel, TimeFrame timeWindow)
	{
		String query = "CALL exception_max_time(?, ?, ?, ?)";
		
		Timestamp timeStamp = null;
		if (time != null)
		{
			timeStamp = new Timestamp(time.getTime());	
		}	
		
		return m_SimpleJdbcTemplate.queryForObject(query.toString(), 
				java.sql.Timestamp.class, dataType, timeStamp, noiseLevel, timeWindow.toString());
	}
}