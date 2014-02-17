package demo.app.server;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;


import demo.app.dao.ExceptionDAO;
import demo.app.data.DatePagingLoadConfig;
import demo.app.data.DatePagingLoadResult;
import demo.app.data.ExceptionPagingLoadConfig;
import demo.app.data.TimeFrame;
import demo.app.data.gxt.EvidenceModel;
import demo.app.service.ExceptionQueryService;



public class ExceptionQueryServiceImpl extends RemoteServiceServlet
	implements ExceptionQueryService
{
	static Logger logger = Logger.getLogger(ExceptionQueryServiceImpl.class);
	
	private ExceptionDAO 		m_ExceptionDAO;
	
	
	/**
	 * Sets the EvidenceDAO to be used by the exception query service.
	 * @param exceptionDAO the data access object for exception views.
	 */
	public void setExceptionDAO(ExceptionDAO exceptionDAO)
	{
		m_ExceptionDAO = exceptionDAO;
	}
	
	
	/**
	 * Returns the ExceptionDAO being used by the exception query service.
	 * @return the data access object for exception views.
	 */
	public ExceptionDAO getExceptionDAO()
	{
		return m_ExceptionDAO;
	}
	
	
	/**
	 * Returns the first page of evidence data matching the specified load config.
	 * @param config load config specifying the range of data (i.e. noise level
	 * 		and time window) to obtain.
	 * @return load result containing the requested evidence data.
	 */
	public DatePagingLoadResult<EvidenceModel> getFirstPage(ExceptionPagingLoadConfig loadConfig)
	{
		String dataType = loadConfig.getDataType();
		Date time = loadConfig.getDate();
		int noiseLevel = loadConfig.getNoiseLevel();
		TimeFrame timeWindow = loadConfig.getTimeWindow();
		
		List<EvidenceModel> evidenceList = m_ExceptionDAO.getFirstPage(
				dataType, time, noiseLevel, timeWindow);
		
		// Note that the list is ordered in DESCENDING order, so the first page
		// corresponds to the latest date in the database.
		TimeFrame timeFrame = loadConfig.getTimeFrame();
		Date startDate = m_ExceptionDAO.getLatestDate(dataType, time, noiseLevel, timeWindow);
		Date endDate = m_ExceptionDAO.getEarliestDate(dataType, time, noiseLevel, timeWindow);
		
		return new DatePagingLoadResult<EvidenceModel>(evidenceList, timeFrame,
				getFirstRowTime(evidenceList, loadConfig), startDate, endDate);
	}
	
	
	/**
	 * Returns the last page of evidence data matching the specified load config.
	 * @param config load config specifying the range of data (i.e. noise level
	 * 		and time window) to obtain.
	 * @return load result containing the requested evidence data.
	 */
	public DatePagingLoadResult<EvidenceModel> getLastPage(ExceptionPagingLoadConfig loadConfig)
	{
		String dataType = loadConfig.getDataType();
		Date time = loadConfig.getDate();
		int noiseLevel = loadConfig.getNoiseLevel();
		TimeFrame timeWindow = loadConfig.getTimeWindow();
		
		List<EvidenceModel> evidenceList = m_ExceptionDAO.getLastPage(
				dataType, time, noiseLevel, timeWindow);
		
		// Note that the list is ordered in DESCENDING order, so the first page
		// corresponds to the latest date in the database.
		TimeFrame timeFrame = loadConfig.getTimeFrame();
		Date startDate = m_ExceptionDAO.getLatestDate(dataType, time, noiseLevel, timeWindow);
		Date endDate = m_ExceptionDAO.getEarliestDate(dataType, time, noiseLevel, timeWindow);
		
		return new DatePagingLoadResult<EvidenceModel>(evidenceList, timeFrame,
				getFirstRowTime(evidenceList, loadConfig), startDate, endDate);
	}
	
	
	/**
	 * Returns the next page of evidence data following on from the row with the
	 * specified id.
	 * @param config load config specifying the range of data to obtain. The date
	 * in this DatePagingLoadConfig must correspond to the value of the time column 
	 * for the bottom row of evidence in the current page.
	 * @param bottomRowId the value of the id column for the bottom row of evidence 
	 * in the current page.
	 * @return load result containing the requested evidence data.
	 */
	public DatePagingLoadResult<EvidenceModel> getNextPage(
			ExceptionPagingLoadConfig loadConfig, String bottomRowId)
	{		
		String dataType = loadConfig.getDataType();
		Timestamp bottomRowTimeStamp = new Timestamp(loadConfig.getDate().getTime());
		int noiseLevel = loadConfig.getNoiseLevel();
		TimeFrame timeWindow = loadConfig.getTimeWindow();
		
		List<EvidenceModel> evidenceList = m_ExceptionDAO.getNextPage(
				dataType, bottomRowTimeStamp, bottomRowId, noiseLevel, timeWindow);
		
		// Note that the list is ordered in DESCENDING order, so the first page
		// corresponds to the latest date in the database.
		TimeFrame timeFrame = loadConfig.getTimeFrame();
		Date startDate = m_ExceptionDAO.getLatestDate(
				dataType, bottomRowTimeStamp, noiseLevel, timeWindow);
		Date endDate = m_ExceptionDAO.getEarliestDate(
				dataType, bottomRowTimeStamp, noiseLevel, timeWindow);
		
		return new DatePagingLoadResult<EvidenceModel>(evidenceList, timeFrame,
				getFirstRowTime(evidenceList, loadConfig), startDate, endDate);
	}
	
	
	/**
	 * Returns the previous page of evidence data to the row with the specified id.
	 * @param config load config specifying the range of data to obtain. The date
	 * in this DatePagingLoadConfig must correspond to the value of the time column 
	 * for the top row of evidence in the current page.
	 * @param topRowId the value of the id column for the top row of evidence 
	 * in the current page.
	 * @return load result containing the requested evidence data.
	 */
	public DatePagingLoadResult<EvidenceModel> getPreviousPage(
			ExceptionPagingLoadConfig loadConfig, String topRowId)
	{
		String dataType = loadConfig.getDataType();
		Timestamp topRowTimeStamp = new Timestamp(loadConfig.getDate().getTime());
		int noiseLevel = loadConfig.getNoiseLevel();
		TimeFrame timeWindow = loadConfig.getTimeWindow();
		
		List<EvidenceModel> evidenceList = m_ExceptionDAO.getPreviousPage(
				dataType, topRowTimeStamp, topRowId, noiseLevel, timeWindow);
		
		// If empty, load the first page - the previous button is always enabled.
		if (evidenceList == null || evidenceList.size() == 0)
		{
			evidenceList = m_ExceptionDAO.getFirstPage(
					dataType, topRowTimeStamp, noiseLevel, timeWindow);
		}
		
		// Note that the list is ordered in DESCENDING order, so the first page
		// corresponds to the latest date in the database.
		TimeFrame timeFrame = loadConfig.getTimeFrame();
		Date startDate = m_ExceptionDAO.getLatestDate(
				dataType, topRowTimeStamp, noiseLevel, timeWindow);
		Date endDate = m_ExceptionDAO.getEarliestDate(
				dataType, topRowTimeStamp, noiseLevel, timeWindow);
		
		return new DatePagingLoadResult<EvidenceModel>(evidenceList, timeFrame,
				getFirstRowTime(evidenceList, loadConfig), startDate, endDate);
	}
	
	
	/**
	 * Returns a page of evidence data, whose top row will match the date in
	 * the supplied config.
	 * @param config load config specifying the range of data to obtain. The date
	 * in this DatePagingLoadConfig will correspond to the value of the time column 
	 * for the first row of evidence data that will be returned.
	 * @return load result containing the requested evidence data.
	 */
	public DatePagingLoadResult<EvidenceModel> getAtTime(ExceptionPagingLoadConfig loadConfig)
	{
		String dataType = loadConfig.getDataType();
		Timestamp time = new Timestamp(loadConfig.getDate().getTime());
		int noiseLevel = loadConfig.getNoiseLevel();
		TimeFrame timeWindow = loadConfig.getTimeWindow();
		
		List<EvidenceModel> evidenceList = m_ExceptionDAO.getAtTime(dataType, time,
				noiseLevel, timeWindow);

		
		// Note that the list is ordered in DESCENDING order, so the first page
		// corresponds to the latest date in the database.
		TimeFrame timeFrame = loadConfig.getTimeFrame();
		Date startDate = m_ExceptionDAO.getLatestDate(dataType, time, noiseLevel, timeWindow);
		Date endDate = m_ExceptionDAO.getEarliestDate(dataType, time, noiseLevel, timeWindow);
		
		return new DatePagingLoadResult<EvidenceModel>(evidenceList, timeFrame,
				getFirstRowTime(evidenceList, loadConfig), startDate, endDate);
	}
	
	
	/**
	 * Returns a list of all of the columns in an Exception List.
	 * @param dataType identifier for the type of evidence data.
	 * @return list of all of the columns for an Exception List.
	 */
	public List<String> getAllColumns(String dataType)
	{
		return m_ExceptionDAO.getAllColumns(dataType);
	}
	
	
	/**
	 * Returns the time of the first row in the specified results list.
	 * @return the time of the first row, or <code>null</code> if the
	 * supplied list is <code>null</code> or empty.
	 */
	private Date getFirstRowTime(List<EvidenceModel> evidenceList, DatePagingLoadConfig config)
	{
		Date firstRowTime = null;
		if (evidenceList == null || evidenceList.size() == 0)
		{
			return null;
		}
		
		EvidenceModel firstRow = evidenceList.get(0);
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
