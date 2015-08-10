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
 * using Google guava InternetDomainName<br>
 *
 * The logic is a little different as we wish to accept domains that
 * aren't recognised top level domains such as those ending '.local'.
 * See the unit tests for clear examples
 */
public class HighestRegisteredDomain extends Transform
{
    public static final String INVALID_HRD_MSG = "The highest registered domain contains '%s'" +
                            "invalid characters";

    private static final String DOT_CHAR_REGEX = "\\.";

    private static final int MAX_DNS_NAME_LENGTH = 253;

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

    public HighestRegisteredDomain(List<TransformIndex> readIndicies, List<TransformIndex> writeIndicies, Logger logger)
    {
        super(readIndicies, writeIndicies, logger);
    }

    /**
     * The InternetDomainName class isn't very lenient and won't tolerate
     * parts starting with a '_' or '-'. It may be against the RFC in
     * many domains in the wild have subdomain parts starting with '_'
     * the Highest Registered Domain will never start with a '_'.
     *
     * This functions splits the domain into parts by '.' and prepends
     * 'p' if the part starts with '_' or '-' so it can
     * be passed into InternetDomainName without an exception being
     * thrown.
     *
     * Parts starting with 'p' are also escaped with a 'p' so the
     * resulting host can be desanitised by removing 'p' from the
     * start of each part
     *
     * @param domain
     * @return If not modified the domain argument is returned
     * otherwise a new string with 'p' preceding the invalid
     * '_' or '-'
     */
    public static String sanitiseDomainName(String domain)
    {
        String [] split = HighestRegisteredDomain.splitDomain(domain);

        boolean mustModify = false;
        for (int i=0; i<split.length; ++i)
        {
            String part = split[i];
            if (part.startsWith("-") || part.startsWith("_"))
            {
                mustModify = true;
            }
        }

        if (!mustModify)
        {
            return domain;
        }

        StringBuilder sb = new StringBuilder();
        for (String part : split)
        {
            if (part.startsWith("-") || part.startsWith("_") || part.startsWith("p"))
            {
                sb.append('p');
            }
            sb.append(part).append('.');
        }
        sb.deleteCharAt(sb.length() -1);  // delete last dot

        return sb.toString();
    }


    /**
     * A sanitised string has each part prepended with 'p' if the part
     * starts with '-', '_' or 'p' therefore if the first character of any
     * part is 'p' removing that character returns the string to it's
     * state before it was sanitised.
     *
     * @param sanitisedDomain
     * @return
     */
    public static String desanitise(String sanitisedDomain)
    {
        String [] split = HighestRegisteredDomain.splitDomain(sanitisedDomain);

        StringBuilder sb = new StringBuilder();
        for (String part : split)
        {
            if (part.startsWith("p"))
            {
                sb.append(part.substring(1)).append('.');
            }
            else
            {
                sb.append(part).append('.');
            }
        }
        sb.deleteCharAt(sb.length() -1);  // delete last dot

        return sb.toString();
    }


    private static String [] splitDomain(String domain)
    {
        String dotDomain = HighestRegisteredDomain.replaceDots(domain);
        return dotDomain.split(DOT_CHAR_REGEX);
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
        // This is structured to be fast in the case of no replacements
        // being necessary, which is expected 99.9-100% of the time.
        // In this case no new string is created.
        String output = input;
        if (output.indexOf('\u3002') >= 0)
        {
            output = output.replace('\u3002', '.');
        }
        if (output.indexOf('\uFF0E') >= 0)
        {
            output = output.replace('\uFF0E', '.');
        }
        if (output.indexOf('\uFF61') >= 0)
        {
            output = output.replace('\uFF61', '.');
        }
        return output;
    }

    /**
     * Split host into Highest Registered Domain and sub domain.
     * First the host argument is sanitised with {@link #sanitiseDomainName(String)}
     * then split according to the following logic:<br>
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
     * <li>The host is completly invalid e.g '192.168.62.9\143\127' or
     * '_kerberos._udp.192.168.62.226'. Return the 2 right most parts as the HRD
     * and everythin else as the sub domain</li>
     * </ol>
     *
     * A result is always returned even if host is not a valid DNS name
     * in which case the sub domain is equal to the host argument and the highest
     * registered domain is ""
     *
     * @param host
     * @return Sub domain, HRD pair
     */
    public static DomainSplit lookup(String host)
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

        if (sanitisedDomain.length() > MAX_DNS_NAME_LENGTH)
        {
            sanitisedDomain = sanitisedDomain.substring(sanitisedDomain.length() - MAX_DNS_NAME_LENGTH, sanitisedDomain.length());
        }

        InternetDomainName idn;
        try
        {
            // if this fails after sanitisation return
            // host as the subdomain
            idn = InternetDomainName.from(sanitisedDomain);
        }
        catch (IllegalArgumentException e)
        {
            String [] split = HighestRegisteredDomain.splitDomain(host);
            if (split.length == 0)
            {
                return new DomainSplit("", "");
            }
            if (split.length == 1)
            {
                return new DomainSplit(split[0], "");
            }
            else if (split.length == 2)
            {
                return new DomainSplit(split[0], split[1]);
            }
            else
            {
                String hrd = split[split.length -2] + "." + split[split.length -1];
                String sub = host.substring(0, host.length() - (hrd.length() + 1));
                return new DomainSplit(sub, hrd);
            }
        }

        if (idn.isPublicSuffix())
        {
            return new DomainSplit("", host);
        }

        String highestRegistered = "";

        // for the case where the host is internal like .local
        // so is not a recognised public suffix
        if (idn.hasPublicSuffix() == false)
        {
            List<String> parts = idn.parts();
            if (!parts.isEmpty())
            {
                if (parts.size() == 1)
                {
                    return new DomainSplit("", host);
                }

                highestRegistered = parts.get(parts.size() -1);
            }
        }
        else
        {
            // HRD is the top private domain
            highestRegistered = idn.topPrivateDomain().toString();
        }


        if (sanitised)
        {
            highestRegistered = HighestRegisteredDomain.desanitise(highestRegistered);
        }

        String subDomain = host.substring(0, host.length() - highestRegistered.length());
        if (subDomain.endsWith("."))
        {
            subDomain = subDomain.substring(0, subDomain.length() -1);
        }

        return new DomainSplit(subDomain, highestRegistered);
    }

    @Override
    public TransformResult transform(String[][] readWriteArea)
    {
        if (m_WriteIndicies.isEmpty())
        {
            return TransformResult.FAIL;
        }

        if (m_ReadIndicies.isEmpty())
        {
            return TransformResult.FAIL;
        }

        TransformIndex i = m_ReadIndicies.get(0);
        String field = readWriteArea[i.array][i.index];

        try
        {

            DomainSplit split = HighestRegisteredDomain.lookup(field);

            i = m_WriteIndicies.get(0);
            readWriteArea[i.array][i.index] = split.m_SubDomain;

            if (m_WriteIndicies.size() > 1)
            {
                i = m_WriteIndicies.get(1);
                readWriteArea[i.array][i.index] = split.m_HighestRegisteredDomain;
            }

            return TransformResult.OK;
        }
        catch (IllegalArgumentException e)
        {
            m_Logger.error("Cannot extract domain. " + e.getMessage());
            return TransformResult.FAIL;
        }
    }
}
