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

package com.prelert.proxy.regex;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Regex utilities.  This class must be kept generic, as it gets dragged into
 * the download product UI.  It must not link directly or indirectly to the
 * Introscope plugin.
 */
public class RegExUtilities
{
	private enum State {ESCAPED_CHAR, NON_ESCAPED_CHAR};

	public static final Pattern s_GrabSpecialChars = Pattern.compile("([\\\\*+\\[\\](){}\\$.?\\^|])");
	
	public static final Set<Character> s_SpecialCharsSet = new HashSet<Character>(
			Arrays.asList('.', '*', '+', '?', '|', '^', '$', '(', ')', '[', ']', '{', '}','\\'));

	/**
	 * @param string containing regex characters we wish to escape. Could be
	 * <code>null</code>
	 * @return string with the following characters escaped: [](){}+*^?$.\|
	 */
	public static String escapeRegex(String regExStr)
	{
		if (regExStr != null)
		{
			Matcher match = s_GrabSpecialChars.matcher(regExStr);
			return match.replaceAll("\\\\$1");
		}
		else
		{
			return null;
		}
	}


	public static boolean stringIsARegex(String test)
	{
		State state = State.NON_ESCAPED_CHAR;
		boolean result = false;
		
		char [] chars = test.toCharArray();
		
		for (char ch : chars)
		{
			if ((ch == '\\') && (state != State.ESCAPED_CHAR))
			{
				state = State.ESCAPED_CHAR;
				continue;
			}
			
			if (s_SpecialCharsSet.contains(ch) && (state != State.ESCAPED_CHAR))
			{
				result = true;
				break;
			}
			
			state = State.NON_ESCAPED_CHAR;
		}
		
		return result;
	}
	
}

