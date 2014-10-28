/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2012     *
 *                                                          *
 *----------------------------------------------------------*
 *----------------------------------------------------------*
 * WARNING:                                                 *
 * THIS FILE CONTAINS UNPUBLISHED PROPRIETARY               *
 * SOURCE CODE WHICH IS THE PROPERTY OF PRELERT LTD AND     *
 * PARENT OR SUBSIDIARY COMPANIES.                          *
 * PLEASE READ THE FOLLOWING AND TAKE CAREFUL NOTE:         *
 *                                                          *
 * This source code is confidential and any person who      *
 * receives a copy of it, or believes that they are viewing *
 * it without permission is asked to notify Prelert Ltd     *
 * on +44 (0)20 3567 1249 or email to legal@prelert.com.    *
 * All intellectual property rights in this source code     *
 * are owned by Prelert Ltd.  No part of this source code   *
 * may be reproduced, adapted or transmitted in any form or *
 * by any means, electronic, mechanical, photocopying,      *
 * recording or otherwise.                                  *
 *                                                          *
 *----------------------------------------------------------*
 *                                                          *
 *                                                          *
 ***********************************************************/

package com.prelert.data;

import java.io.Serializable;
import java.util.Date;


/**
 * Encapsulates a time marker, i.e. a time sent through the data
 * stream that indicates all processing is complete up to that time.
 *
 * This class can convert a time marker to XML format to be sent
 * between processes.
 *
 * The C++ CTimeMarker class also contains knowledge of the same
 * XML format, so changes to this class are likely to necessitate
 * similar changes to that C++ class.
 *
 * Time markers must be passed along the data stream along with
 * standard data handled by each application.  It is an error
 * to send a time marker containing a particular time to the
 * next process in the chain, and subsequently send data with an
 * earlier time.
 */
public class TimeMarker implements Serializable, Comparable<TimeMarker>
{
	private static final long serialVersionUID = 102223823819747253L;

	public enum SpecialTime { EPOCH, END_OF_TIME };

	private Date m_Time;


	/**
	 * Creates a time marker containing the current time.
	 */
	public TimeMarker()
	{
		m_Time = new Date();
	}


	/**
	 * Creates a time marker containing a special time.
	 * @param specialTime An enumerated type indicating the special time to be
	 *                    marked.
	 */
	public TimeMarker(SpecialTime specialTime)
	{
		switch (specialTime)
		{
			case EPOCH:
			{
				m_Time = new Date(0L);
				break;
			}
			case END_OF_TIME:
			{
				// This is based on the maximum value that 32 bit C++ processes
				// can store in their time_t type.  Once we get to the stage of
				// only ever building 64 bit C++ processes, we should change
				// the argument here to be Long.MAX_VALUE.
				m_Time = new Date(Integer.MAX_VALUE * 1000L);
				break;
			}
		}
	}


	/**
	 * Creates a time marker containing a specific time.
	 * @param time The time to be marked.
	 */
	public TimeMarker(Date time)
	{
		m_Time = time;
	}


	/**
	 * Returns the marker time.
	 * @return The marker time.
	 */
	public Date getTime()
	{
		return m_Time;
	}


	/**
	 * Tests this <code>TimeMarker</code> for equality with another object.
	 * @return true if the comparison object is an TimeMarker object with
	 *              an identical time, otherwise false.
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}

		if (!(obj instanceof TimeMarker))
		{
			return false;
		}

		TimeMarker other = (TimeMarker)obj;

		return m_Time.equals(other.getTime());
	}


	/**
	 * Returns a <code>String</code> representation of this time marker.
	 * @return A <code>String</code> representation of this time marker.
	 */
	@Override
	public String toString()
	{
		StringBuilder strRep = new StringBuilder("{ marked time = ");
		strRep.append(m_Time);
		strRep.append(" = C epoch time ");
		strRep.append(m_Time.getTime() / 1000L);
		strRep.append(" }");

		return strRep.toString();
	}


	/**
	 * Returns an XML representation of this time marker.  This must
	 * be in the same format that the C++ <code>CTimeMarker</code>
	 * class uses.
	 * @return An XML representation of this time marker.
	 */
	public String toXml()
	{
		StringBuilder strRep = new StringBuilder("<prelert_time_marker>");
		strRep.append("<time>");
		strRep.append(m_Time.getTime() / 1000L);
		strRep.append("</time>");
		strRep.append("</prelert_time_marker>");

		return strRep.toString();
	}


	/**
	 * Ordering on <code>TimeMarker</code>s is just done on the time.
	 */
	@Override
	public int compareTo(TimeMarker other)
	{
		return m_Time.compareTo(other.getTime());
	}

}
