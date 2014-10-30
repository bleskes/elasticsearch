/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2010     *
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

package com.prelert.client;

import java.util.Comparator;

/**
 * Comparator implementation which compares Strings ignoring case differences.
 * @param <X> the type of objects that may be compared by this comparator
 */
class CaseInsensitiveComparator<X extends Object> implements Comparator<X>
{

	@SuppressWarnings( { "unchecked" })
	public int compare(Object o1, Object o2)
	{
		if (o1 == null || o2 == null)
		{
			if (o1 == null && o2 == null)
			{
				return 0;
			}
			else
			{
				return (o1 == null) ? -1 : 1;
			}
		}

		if (o1 instanceof Comparable)
		{
			if (o1 instanceof String)
			{
				String str1 = (String)o1;
				String str2 = (String)o2;
				return str1.compareToIgnoreCase(str2);
			}
			else
			{
				return ((Comparable) o1).compareTo(o2);
			}
		}
		
		return o1.toString().compareToIgnoreCase(o2.toString());
	}

}
