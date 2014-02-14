package demo.app.client;

public enum TimeFrame
{
	WEEK, DAY, HOUR, MINUTE, SECOND;

	public long getInterval()
	{
		long interval = 0l;
		switch (this)
		{
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
