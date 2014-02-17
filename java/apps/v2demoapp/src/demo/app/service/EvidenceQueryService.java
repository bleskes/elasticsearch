package demo.app.service;

import java.util.Date;
import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;

import demo.app.data.*;
import demo.app.data.gxt.EvidenceModel;
import demo.app.data.gxt.GridRowInfo;


/**
 * Defines the methods for the interface to the Evidence View query service.
 * @author Pete Harverson
 */
public interface EvidenceQueryService extends RemoteService
{
	/**
	 * Returns the first page of evidence data matching the specified load config.
	 * @param config load config specifying the range of data (i.e. time frame)
	 * to obtain.
	 * @return load result containing the requested evidence data.
	 */
	public DatePagingLoadResult<EvidenceModel> getFirstPage(EvidencePagingLoadConfig config);
	
	
	/**
	 * Returns the last page of evidence data matching the specified load config.
	 * @param config load config specifying the range of data (i.e. time frame)
	 * to obtain.
	 * @return load result containing the requested evidence data.
	 */
	public DatePagingLoadResult<EvidenceModel> getLastPage(EvidencePagingLoadConfig config);
	
	
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
			EvidencePagingLoadConfig config, String bottomRowId);
	
	
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
			EvidencePagingLoadConfig config, String topRowId);
	
	
	/**
	 * Returns a page of evidence data, the top row of which matches the specified
	 * description, and whose time corresponds to the date in the supplied config.
	 * @param config load config specifying the range of data to obtain. The date
	 * in this DatePagingLoadConfig will correspond to the value of the time column 
	 * for the first row of evidence data that will be returned.
	 * @param description the value of the description column to match on for the
	 * top row of evidence data that will be returned.
	 * @return load result containing the requested evidence data.
	 */
	public DatePagingLoadResult<EvidenceModel> getForDescription(
			EvidencePagingLoadConfig config, String description);
	
	
	/**
	 * Returns a page of evidence data, whose top row will match the date in
	 * the supplied config.
	 * @param config load config specifying the range of data to obtain. The date
	 * in this DatePagingLoadConfig will correspond to the value of the time column 
	 * for the first row of evidence data that will be returned.
	 * @return load result containing the requested evidence data.
	 */
	public DatePagingLoadResult<EvidenceModel> getAtTime(EvidencePagingLoadConfig config);
	
	
	/**
	 * Returns a page of evidence data, the top row of which matches the specified id.
	 * @param config load config specifying the range of data to obtain 
	 * 	e.g. the time frame.
	 * @param id evidence id for the top row of evidence data that will be returned.
	 * @return load result containing the requested evidence data. The date
	 * in this DatePagingLoadResult will correspond to the value of the time column 
	 * for the first row of evidence data that will be returned.
	 */
	public DatePagingLoadResult<EvidenceModel> getIdPage(
			EvidencePagingLoadConfig config, int id);
	
	
	/**
	 * Returns the details on the row of evidence with the given id.
	 * @param id the value of the id column for the row to obtain information on.
	 * @return List of GridRowInfo objects for the row with the specified id.
	 */
	public List<GridRowInfo> getRowInfo(int rowId);
	
	
	/**
	 * Returns the data model for the single item of evidence with the given id.
	 * @param id id of the item of evidence to return.
	 * @return the complete data model for the item of evidence with the given id.
	 */
	public EvidenceModel getEvidenceSingle(int id);
	
	
	/**
	 * Returns a list of all of the columns in an Evidence View with the specified
	 * time frame.
	 * @param dataType identifier for the type of evidence data.
	 * @param timeFrame time frame for which to return the list of columns.
	 * @return list of all of the columns for an Evidence View with the specified
	 * time frame.
	 */
	public List<String> getAllColumns(String dataType, TimeFrame timeFrame);
	
	
	/**
	 * Returns a list of the names of the columns that support filtering.
	 * @param dataType identifier for the type of evidence data.
	 * @param getCompulsory <code>true</code> to return compulsory columns, 
	 * 		<code>false</code> otherwise.
	 * @param getOptional <code>true</code> to return optional columns, 
	 * 		<code>false</code> otherwise.
	 * @return a list of the filterable columns.
	 */
	public List<String> getFilterableColumns(String dataType, 
			boolean getCompulsory, boolean getOptional);
	
	
	/**
	 * Returns the list of values in the evidence table for the specified column.
	 * @param dataType identifier for the type of evidence data.
	 * @param columnName name of the column for which to return the values.
	 * @return list of all the distinct values in the evidence table for the
	 * 			given column.
	 */
	public List<String> getColumnValues(String dataType, String columnName);
	
	
	/**
	 * Returns the date of the earliest evidence record in the Prelert database.
	 * @param dataType identifier for the type of evidence data.
	 * @param source name of the source (server) for which to obtain evidence data
	 * 	or <code>null</code> to return data from all sources.
	 * @return date of earliest evidence record, or <code>null</code> if the
	 * database contains no evidence records.
	 */
	public Date getEarliestDate(String dataType, String source);
	
	
	/**
	 * Returns the date of the latest evidence record in the Prelert database.
	 * @param dataType identifier for the type of evidence data.
	 * @param source name of the source (server) for which to obtain evidence data
	 * 	or <code>null</code> to return data from all sources.
	 * @return date of latest evidence record, or <code>null</code> if the
	 * database contains no evidence records.
	 */
	public Date getLatestDate(String dataType, String source);

}
