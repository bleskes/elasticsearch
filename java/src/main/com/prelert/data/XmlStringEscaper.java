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


/**
 * Escapes XML strings.  Not using Apache Commons for this, as it needs
 * to work for the "data" package, which needs to be GWT compatible.
 * @author David Roberts
 */
public class XmlStringEscaper
{
	/**
	 * Escapes an XML string.
	 * @param str The string to escape.
	 * @return The escaped string.
	 */
	public static String escapeXmlString(String str)
	{
		// This is implemented on the basis that most strings WON'T need
		// changing at all - if it's the case that the majority of strings DO
		// need changing, it would have been faster to skip this pre-processing
		// and just call changeXmlString() straight away
		for (int index = 0; index < str.length(); ++index)
		{
			switch (str.charAt(index))
			{
				case '\"':
				case '&':
				case '\'':
				case '<':
				case '>':
				{
					return changeXmlString(index, str);
				}
			}
		}

		// Return the string unaltered - this should be a reference to the same
		// object that was passed as input, so no copying of characters should
		// have occurred in this case
		return str;
	}


	/**
	 * Escapes an XML string.  This method assumes that the string it's passed
	 * needs changing, but won't do any harm if no escaping is necessary.
	 * @param firstPos The position of the first character in the string that
	 *                 might need escaping.  (Other than efficiency, it doesn't
	 *                 matter if it doesn't.)
	 * @param str The string to escape.
	 * @return The escaped string.
	 */
	private static String changeXmlString(int firstPos, String str)
	{
		StringBuilder escapedStr = new StringBuilder();
		if (firstPos > 0)
		{
			escapedStr.append(str, 0, firstPos);
		}

		for (int index = firstPos; index < str.length(); ++index)
		{
			char current = str.charAt(index);
			switch (current)
			{
				case '\"':
				{
					escapedStr.append("&quot;");
					break;
				}
				case '&':
				{
					escapedStr.append("&amp;");
					break;
				}
				case '\'':
				{
					escapedStr.append("&apos;");
					break;
				}
				case '<':
				{
					escapedStr.append("&lt;");
					break;
				}
				case '>':
				{
					escapedStr.append("&gt;");
					break;
				}
				default:
				{
					escapedStr.append(current);
					break;
				}
			}
		}

		return escapedStr.toString();
	}

}
