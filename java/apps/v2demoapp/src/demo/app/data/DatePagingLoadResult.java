package demo.app.data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.extjs.gxt.ui.client.data.BaseListLoadResult;
import com.extjs.gxt.ui.client.data.BaseModelData;


/**
 * An extension of ListLoadResult for a loader for paging through a date range.
 * @author Pete Harverson
 *
 * @param <BaseModelData>
 */
public class DatePagingLoadResult<BaseModelData> extends BaseListLoadResult<BaseModelData>
	implements Serializable
{
	private TimeFrame 	m_TimeFrame;
	private Date		m_Date;
	private Date		m_StartDate;
	private Date 		m_EndDate;

	
	/**
	 * Creates a new, empty date paging load result.
	 */
	public DatePagingLoadResult()
	{
		this(null);
	}
	
	
	/**
	 * Creates a new date paging load result containing the supplied list of data.
	 * @param data list of the model data contained within the load result.
	 */
	public DatePagingLoadResult(List<BaseModelData> list)
    {
	    super(list);
    }
	

	/**
	 * Creates a new date paging load result containing the supplied list of data.
	 * @param data list of the model data contained within the load result.
	 * @param timeFrame the time frame for this load result e.g. week, day or hour.
	 * @param date the date for this load result. This should correspond to the 
	 * 		start date/time for the result's time frame i.e. the start of the
	 * 		week, day or hour.
	 * @param startDate the start (i.e. earliest) date of the full range of 
	 * 		results which are available.
	 * @param endDate the end (i.e. latest) date of the full range of results
	 * 		which are available.
	 */
	public DatePagingLoadResult(List<BaseModelData> list, TimeFrame timeFrame,
            Date date, Date startDate, Date endDate)
    {
	    super(list);
	    m_TimeFrame = timeFrame;
	    m_Date = date;
	    m_StartDate = startDate;
	    m_EndDate = endDate;
    }


	/**
	 * Returns the time frame for this load result.
	 * @return time frame of this load result e.g. week, day or hour.
	 */
	public TimeFrame getTimeFrame()
    {
    	return m_TimeFrame;
    }


	/**
	 * Sets the time frame for this load result.
	 * @param timeFrame time frame of this load result e.g. week, day or hour.
	 */
	public void setTimeFrame(TimeFrame timeFrame)
    {
    	m_TimeFrame = timeFrame;
    }


	/**
	 * Returns the date for this load result. This should correspond to the 
	 * start date/time for the result's time frame i.e. the start of the
	 * week, day or hour.
	 * @return the date for this load result, which may or may not correspond to
	 * the date of the first result in this load result depending on the request
	 * that was made by the client.
	 */
	public Date getDate()
    {
    	return m_Date;
    }


	/**
	 * Sets the date for this load result. This should correspond to the 
	 * start date/time for the result's time frame i.e. the start of the
	 * week, day or hour.
	 * @param date the date for this load result, which may or may not correspond to
	 * the date of the first result in this load result depending on the request
	 * that was made by the client.
	 */
	public void setDate(Date date)
    {
    	m_Date = date;
    }


	/**
	 * Returns the start (i.e. earliest) date of the full range of results 
	 * which are available.
	 * @return the start date of the results which are available.
	 */
	public Date getStartDate()
    {
    	return m_StartDate;
    }


	/**
	 * Sets the start (i.e. earliest) date of the full range of results which 
	 * are available.
	 * @param startDate the start date of the results which are available.
	 */
	public void setStartDate(Date startDate)
    {
    	m_StartDate = startDate;
    }


	/**
	 * Returns the end (i.e. latest) date of the full range of results 
	 * which are available.
	 * @return the end date of the results which are available.
	 */
	public Date getEndDate()
    {
    	return m_EndDate;
    }


	/**
	 * Sets the end (i.e. latest) date of the full range of results which 
	 * are available.
	 * @param endDate the end date of the results which are available.
	 */
	public void setEndDate(Date endDate)
    {
    	m_EndDate = endDate;
    }
	
}
