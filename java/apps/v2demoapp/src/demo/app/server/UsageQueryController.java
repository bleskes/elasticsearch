package demo.app.server;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.servlet.http.*;

import org.apache.log4j.Logger;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;

import demo.app.dao.UsageViewDAO;
import demo.app.data.TimeSeriesConfig;
import demo.app.data.TimeSeriesDataPoint;
import demo.app.data.UsageRecord;

public class UsageQueryController extends MultiActionController
{
	static Logger logger = Logger.getLogger(UsageQueryController.class);

	private UsageViewDAO 	m_UsageViewDAO;
	
	
	/**
	 * Returns the UsageViewDAO being used by the usage query service.
	 * @return the data access object for the usage view.
	 */
	public UsageViewDAO getUsageViewDAO()
    {
    	return m_UsageViewDAO;
    }


	/**
	 * Sets the UsageViewDAO to be used by the usage query service.
	 * @param usageViewDAO the data access object for the usage view.
	 */
	public void setUsageViewDAO(UsageViewDAO usageViewDAO)
    {
    	m_UsageViewDAO = usageViewDAO;
    }
	

	public void getUsageData(HttpServletRequest request,
	        HttpServletResponse response, TimeSeriesConfig config)
	{
		logger.debug("getUsageData() called for config: " + config);
		
		// Use test date of 06-07-2009.
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.set(2009, 6, 6);
		Date testDate = calendar.getTime();
		
		// Get some sample server usage data.
		List<UsageRecord> records = m_UsageViewDAO.getDailyUsageData(
				testDate, config.getMetric(), config.getSource(), config.getUser());
		logger.debug("Number of records obtained: " + records.size());
		
		ArrayList<TimeSeriesDataPoint> usageData = new ArrayList<TimeSeriesDataPoint>();
		if (records != null && records.size() > 0)
		{
			for (UsageRecord record : records)
			{
				usageData.add(new TimeSeriesDataPoint(record.getTime(), record.getValue()));
			}
		}

		try
		{
			// Set the response code and write the response data.
			response.setStatus(HttpServletResponse.SC_OK);
			
			// Try writing Object data.
			ObjectOutputStream out = new ObjectOutputStream(response.getOutputStream());
			out.writeObject(usageData);
			
			out.flush();
			out.close();
			
		}
		catch (IOException e)
		{
			try
			{
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().print(e.getMessage());
				response.getWriter().close();
			}
			catch (IOException ioe)
			{
			}
		}

	}
}
