/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2016     *
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

package com.prelert.server.info;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import org.apache.log4j.Logger;


/**
 * Helper class to find the current hostname.
 * Based on the observation that getting the hostname from the
 * network is not reliable, because it can:
 * a) Fail completely or
 * b) Report hostname of "localhost" or "127.0.0.1"
 * This class also falls back to reporting "localhost" as a last
 * resort, but only after trying more reliable methods.
 */
public class HostnameFinder
{
    private static final Logger LOGGER = Logger.getLogger(HostnameFinder.class);

    private static final String COMMAND = "hostname";
    private static final String COMPUTERNAME = "COMPUTERNAME";
    private static final String HOSTNAME = "HOSTNAME";
    private static final String LAST_RESORT = "localhost";

    private HostnameFinder()
    {
        // Cannot construct
    }

    public static String findHostname()
    {
        String name = findHostnameFromCommand();
        if (name.isEmpty())
        {
            name = findHostnameFromEnvironment();
            if (name.isEmpty())
            {
                return LAST_RESORT;
            }
        }
        return name;
    }

    private static String findHostnameFromCommand()
    {
        try
        {
            Process proc = Runtime.getRuntime().exec(COMMAND);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(proc.getInputStream(),
                    // Assume OS commands output the same character set as the
                    // JVM default - not necessarily true but what else can we do?
                    Charset.defaultCharset()));
            String line = reader.readLine();
            if (line != null)
            {
                return line.trim();
            }
        }
        catch (IOException e)
        {
            LOGGER.error("Problem getting output from the " + COMMAND +
                    " command", e);
        }
        return "";
    }

    private static String findHostnameFromEnvironment()
    {
        String name = getTrimmedEnvironmentVariable(COMPUTERNAME);
        if (name.isEmpty())
        {
            return getTrimmedEnvironmentVariable(HOSTNAME);
        }
        return name;
    }

    private static String getTrimmedEnvironmentVariable(String variable)
    {
        String name = System.getenv(variable);
        if (name != null)
        {
            return name.trim();
        }
        return "";
    }
}
