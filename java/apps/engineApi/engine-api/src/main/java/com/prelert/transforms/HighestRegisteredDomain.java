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
 * Split a hostname into Highest Registered Domain and sub domain
 * using Google guava InternetDomainName<br/>
 *
 * The logic is a little different as we wish to accept domains that
 * aren't recognised top level domains such as those ending '.local'.
 * See the unit tests for clear examples
 */
public class HighestRegisteredDomain extends Transform
{
    /**
     * Immutable class for the domain split results
     */
    public static class DomainSplit
    {
        private String m_SubDomain;
        private String m_HighestRegisteredDomain;

        private DomainSplit(String subDomain, String highestRegisteredDomain)
        {
            m_SubDomain = subDomain;
            m_HighestRegisteredDomain = highestRegisteredDomain;
        }

        public String getSubDomain()
        {
            return m_SubDomain;
        }

        public String getHighestRegisteredDomain()
        {
            return m_HighestRegisteredDomain;
        }
    }

    public HighestRegisteredDomain(int[] inputIndicies, int[] outputIndicies)
    {
        super(inputIndicies, outputIndicies);
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
        String highestRegistered = "";

        // for the case where the host is internal like .local
        // so the not a recognised public suffix
        if (idn.hasPublicSuffix() == false)
        {
            List<String> parts = idn.parts();
            if (!idn.parts().isEmpty())
            {
                highestRegistered = parts.get(parts.size() -1);
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

            return new DomainSplit(subDomain.toString(), highestRegistered);
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

        highestRegistered = idn.topPrivateDomain().toString();

        return new DomainSplit(subDomain.toString(), highestRegistered);
    }

    @Override
    public boolean transform(String[] inputRecord,
                            String[] outputRecord)
    throws TransformException
    {
        DomainSplit split = lookup(inputRecord[m_InputIndicies[0]]);

        outputRecord[m_OutputIndicies[0]] = split.m_SubDomain;
        if (m_OutputIndicies.length == 2)
        {
            outputRecord[m_OutputIndicies[1]] = split.m_HighestRegisteredDomain;
        }

        return false;
    }
}
