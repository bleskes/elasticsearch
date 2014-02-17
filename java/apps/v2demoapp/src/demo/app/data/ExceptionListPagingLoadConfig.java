package demo.app.data;


/**
 * Paging load configuration for the Exception List window.
 * @author Pete Harverson
 */
public class ExceptionListPagingLoadConfig extends DatePagingLoadConfig
{


	/**
	 * Returns the level of noise to act as the filter for the exception list.
	 * @return the noise level, a value from 0 to 100.
	 */
	public int getNoiseLevel()
	{
		return (Integer) get("noiseLevel");
	}


	/**
	 * Sets the level of noise to act as the filter for the exception list.
	 * @param noiseLevel the noise level, a value from 0 to 100.
	 */
	public void setNoiseLevel(int noiseLevel)
	{
		set("noiseLevel", noiseLevel);
	}


	/**
	 * Returns the time window for the Exception List in which an item of
	 * evidence is examined to see if it is an 'exception'.
	 * @return the time window e.g. WEEK, DAY or HOUR.
	 */
	public TimeFrame getTimeWindow()
	{
		return (TimeFrame) get("timeWindow");
	}


	/**
	 * Sets the time window for the Exception List in which an item of
	 * evidence is examined to see if it is an 'exception'.
	 * @param timeWindow the time window e.g. WEEK, DAY or HOUR.
	 */
	public void setTimeWindow(TimeFrame timeWindow)
	{
		set("timeWindow", timeWindow);
	}

}
