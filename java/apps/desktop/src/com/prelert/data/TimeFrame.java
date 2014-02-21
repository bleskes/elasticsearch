package com.prelert.data;

/**
 * Enum representing a time frame e.g. a week, day or hour.
 * @author Pete Harverson
 */
public enum TimeFrame
{
	ALL, WEEK, DAY, HOUR, MINUTE, SECOND;

	/**
	 * Returns the number of milliseconds in the interval corresponding to this
	 * time frame i.e. number of milliseconds in a week / day / hour.
	 * @return the number of milliseconds in the time frame interval.
	 */
	public long getInterval()
	{
		long interval = 0l;
		switch (this)
		{
			case ALL:
				return Long.MAX_VALUE;
				
			case WEEK:
				return 604800000l;
			
			case DAY:
				return 86400000l;
				
			case HOUR:
				return 3600000l;
				
			case MINUTE:
				return 60000l;
				
			case SECOND:
				return 1000l;
		}
		
		return interval;
	}
}
