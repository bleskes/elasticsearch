package demo.app.server;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import demo.app.data.TimeFrame;
import demo.app.data.gxt.EvidenceModel;

import static demo.app.data.DateTimeFormatPatterns.*;

/**
 * Class contains a number of utility functions for use in server-side code.
 * @author Pete Harverson
 */
public class ServerUtil
{
	/**
	 * Retrieves the value of the time field as a Date object for the 
	 * specified EventRecord in the context of the specified time frame.
	 * @param record EventRecord for which to obtain the value of the time field.
	 * @param timeFrame	time frame context of record e.g. WEEK, DAY, 
	 * 			HOUR, MINUTE or SECOND.
	 * @return recorded time of event record.
	 * @throws ParseException if the value of the time field in the supplied
	 *  record cannot be parsed to a Date object.
	 * 
	 */
	public static Date parseTimeField(EvidenceModel record, TimeFrame timeFrame) 
		throws ParseException
	{
		Date date = null;
		
		String timeVal = record.getTime(timeFrame);
		
		// Need to use GWT DateTimeFormat class as java.text.SimpleDateFormat
		// not supported. Note that GWT DateTimeFormat cannot be used on the
		// server-side too due to its call to GWT.create().
		SimpleDateFormat dateFormatter = null;
		switch (timeFrame)
		{
			case WEEK:
				dateFormatter = new SimpleDateFormat(WEEK_PATTERN);
				break;
			case DAY:
				dateFormatter = new SimpleDateFormat(DAY_PATTERN);
				break;
			case HOUR:
				dateFormatter = new SimpleDateFormat(HOUR_PATTERN);
				break;
			case MINUTE:
				dateFormatter = new SimpleDateFormat(MINUTE_PATTERN);
				break;
			case SECOND:
				dateFormatter = new SimpleDateFormat(SECOND_PATTERN);
				break;
		}
		
		if (dateFormatter != null && timeVal != null)
		{
			date = dateFormatter.parse(timeVal);
		}
		
		return date;
	}
}
