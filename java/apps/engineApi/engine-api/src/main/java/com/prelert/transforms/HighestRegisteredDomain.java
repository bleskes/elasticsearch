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

import org.apache.log4j.Logger;

import com.google.common.net.InetAddresses;
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
    public static final String INVALID_HRD_MSG = "The highest registered domain contains " +
                            "invalid characters";
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

    public HighestRegisteredDomain(int[] inputIndicies, int[] outputIndicies,
             Logger logger)
    {
        super(inputIndicies, outputIndicies, logger);
    }


    /**
     * The InternetDomainName class isn't very lenient and won't tolerate
     * parts starting with a '_' or '-'. It may be against the RFC in
     * many domains in the wild have subdomain parts starting with '_'
     * the Highest Registered Domain will never start with a '_'.
     *
     * This functions splits the domain into parts by '.' and prepends
     * an extra character if the part starts with '_' or '-' so it can
     * be passed into InternetDomainName without an exception being
     * thrown.
     *
     *
     * @param domain
     * @return If not modified the domain argument is returned
     * otherwise a new string with 'p' preceeding the invalid
     * '_' or '-'
     */
    public static String sanitiseDomainName(String domain)
    {
        String dotDomain = HighestRegisteredDomain.replaceDots(domain);

        String dotCharRegex = "\\.";

        String [] split = dotDomain.split(dotCharRegex);

        boolean modifed = false;
        for (int i=0; i<split.length; ++i)
        {
            String part = split[i];
            if (part.startsWith("-") || part.startsWith("_"))
            {
                split[i] = 'p' + part;
                modifed = true;
            }
        }

        if (!modifed)
        {
            return domain;
        }

        StringBuilder sb = new StringBuilder();
        for (String part : split)
        {
            sb.append(part).append('.');
        }
        sb.deleteCharAt(sb.length() -1);  // delete last dot

        return sb.toString();
    }

    /**
     * Replace the various unicode dot characters u3002, uFF0E and uFF61
     * with the ascii '.'
     *
     * @param input
     * @return
     */
    public static String replaceDots(String input)
    {
        return input.replaceAll("\u3002|\uFF0E|\uFF61", ".");
    }

    /**
     * Split host into effective top level domain and sub domain.
     * First the host argument is sanitised with {@link #sanitiseDomainName(String)}
     * then split according to the following logic:<br/>
     *
     * <ol>
     * <li>An empty host returns empty domain and sub domain</li>
     * <li>If the host does not have a recognised public suffix such as .local
     * the effective TLD is everything after the final '.' and the subdomain
     * is everything before the the final '.'</li>
     * <li>The host <em>is</em>  a public suffix return '' as the subdomain and the
     * host as the highest registered domain</li>
     * <li>The host has a public suffix so split according to the rules
     * of the Guava InternetDomainname class </li>
     * </ol>
     *
     * @param host
     * @return
     * @throws IllegalArgumentException if the HRD part of the host contains
     * invalid characters i.e. it starts with '_' or '-'
     */
    static public DomainSplit lookup(String host)
    {
        if (host.isEmpty())
        {
            return new DomainSplit("", "");
        }

        host = host.trim();

        // Put IP addresses into the domain portion of the result in their
        // entirety
        if (InetAddresses.isInetAddress(host))
        {
            return new DomainSplit("", host);
        }

        String sanitisedDomain = HighestRegisteredDomain.sanitiseDomainName(host);
        boolean sanitised = sanitisedDomain != host;

        InternetDomainName idn = InternetDomainName.from(sanitisedDomain);

        if (idn.isPublicSuffix())
        {
            return new DomainSplit("", host);
        }

        String highestRegistered = "";

        // for the case where the host is internal like .local
        // so is not a recognised public suffix
        if (idn.hasPublicSuffix() == false)
        {
            String subDomain = "";

            List<String> parts = idn.parts();
            if (!idn.parts().isEmpty())
            {
                highestRegistered = parts.get(parts.size() -1);

                // does this have a public suffix thats been sanitised?
                if (sanitised)
                {
                    // the input argument should end with the HRD
                    if (host.endsWith(highestRegistered) == false)
                    {
                        throw new IllegalArgumentException(INVALID_HRD_MSG);
                    }
                }

                subDomain = host.substring(0, host.length() - (highestRegistered.length()));
                if (subDomain.endsWith("."))
                {
                    subDomain = subDomain.substring(0, subDomain.length() -1);
                }
            }

            return new DomainSplit(subDomain, highestRegistered);
        }

        highestRegistered = idn.topPrivateDomain().toString();
        String subDomain = host.substring(0, host.length() - (highestRegistered.length()));
        if (subDomain.endsWith("."))
        {
            subDomain = subDomain.substring(0, subDomain.length() -1);
        }

        // check that the highest registered domain has
        // not been sanitised - this part should never
        // contain invalid characters
        if (sanitised)
        {
            // the input argument should end with the HRD
            if (host.endsWith(highestRegistered) == false)
            {
                throw new IllegalArgumentException(INVALID_HRD_MSG);
            }
        }

        return new DomainSplit(subDomain, highestRegistered);
    }

    @Override
    public boolean transform(String[] inputRecord,
                            String[] outputRecord)
    {
        try
        {
            DomainSplit split = HighestRegisteredDomain.lookup(inputRecord[m_InputIndicies[0]]);

            outputRecord[m_OutputIndicies[0]] = split.m_SubDomain;
            if (m_OutputIndicies.length == 2)
            {
                outputRecord[m_OutputIndicies[1]] = split.m_HighestRegisteredDomain;
            }

            return true;
        }
        catch (IllegalArgumentException e)
        {
            m_Logger.error("Cannot extract domain. " + e.getMessage());
            return false;
        }
    }
}
