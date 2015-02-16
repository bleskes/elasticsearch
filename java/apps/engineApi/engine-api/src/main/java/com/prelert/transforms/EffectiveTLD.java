/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2015     *
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
 ************************************************************/

package com.prelert.transforms;

import java.util.List;

import com.google.common.net.InternetDomainName;


/**
 * Split a hostname into effective top level domain and sub domain
 * using Google guava InternetDomainName<br/>
 *
 * The logic is a little different as we wish to accept domains that
 * aren't recognised top level domains such as those ending '.local'.
 * See the unit tests for clear examples
 */
public class EffectiveTLD
{
	/**
	 * Immutable class for the domain split results
	 */
	static public class DomainSplit
	{
		private String m_SubDomain;
		private String m_EffectiveTLD;

		private DomainSplit(String subDomain, String effectiveTLD)
		{
			m_SubDomain = subDomain;
			m_EffectiveTLD = effectiveTLD;
		}

		public String getSubDomain()
		{
			return m_SubDomain;
		}

		public String getEffectiveTLD()
		{
			return m_EffectiveTLD;
		}
	}

	/**
	 * Split host into effective top level domain and sub domain
	 * following this logic:<br/>
	 * <ol>
	 * <li>An empty host returns empty domain and sub domain</li>
	 * <li>If the host does not have a recognised public suffix such as .local
	 * the effective TLD is everything after the final '.' and the subdomain
	 * is everything before the the final '.'</li>
	 * <li>The host has a public suffix so split according to the rules
	 * of the Guava InternetDomainname class </li>
	 * </ol>
	 *
	 * @param host
	 * @return
	 */
	static public DomainSplit lookup(String host)
	{
		if (host.isEmpty())
		{
			return new DomainSplit("", "");
		}

		InternetDomainName idn = InternetDomainName.from(host);

		StringBuilder subDomain = new StringBuilder();
		String effectiveTLD = "";

		// for the case where the host is internal like .local
		// so the not a recognised public suffix
		if (idn.hasPublicSuffix() == false)
		{
			List<String> parts = idn.parts();
			if (idn.parts().size() > 0)
			{
				effectiveTLD = parts.get(parts.size() -1);
				for (int i=0; i<parts.size() -1; i++)
				{
					subDomain.append(parts.get(i)).append('.');
				}

				if (subDomain.length() > 0)
				{
					// trim final '.'
					subDomain.deleteCharAt(subDomain.length() -1);
				}
			}

			return new DomainSplit(subDomain.toString(), effectiveTLD);
		}

		while (idn.isTopPrivateDomain() == false)
		{
			subDomain.append(idn.parts().get(0)).append('.');
			idn = idn.parent();
		}

		if (subDomain.length() > 0)
		{
			// trim final '.'
			subDomain.deleteCharAt(subDomain.length() -1);
		}

		effectiveTLD = idn.topPrivateDomain().toString();

		return new DomainSplit(subDomain.toString(), effectiveTLD);
	}
}
