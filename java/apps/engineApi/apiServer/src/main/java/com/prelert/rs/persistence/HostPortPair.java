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
 ************************************************************/

package com.prelert.rs.persistence;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class HostPortPair
{
    private final InetAddress m_Host;
    private final int m_Port;

    HostPortPair(InetAddress host, int port)
    {
        m_Host = host;
        m_Port = port;
    }

    public InetAddress getHost()
    {
        return m_Host;
    }

    public int getPort()
    {
        return m_Port;
    }

    public static List<HostPortPair> ofList(String hostPortPairsList)
    {
        List<HostPortPair> hosts = new ArrayList<>();
        String[] hostsAndPorts = hostPortPairsList.split(",");
        for (String hostPortPair : hostsAndPorts)
        {
            String[] hostAndPortParts = hostPortPair.split(":");
            if (hostAndPortParts.length != 2)
            {
                throw new IllegalArgumentException("Failed to parse host/port pair: '"
                        + hostPortPair + "' is not in the form <address:port>.");
            }
            hosts.add(new HostPortPair(parseHost(hostAndPortParts[0]), parsePort(hostAndPortParts[1])));
        }
        return hosts;
    }

    private static InetAddress parseHost(String host)
    {
        try
        {
            return InetAddress.getByName(host);
        }
        catch (UnknownHostException e)
        {
            String msg = "Failed to parse host: '" + host + "' cannot be resolved.";
            throw new IllegalArgumentException(msg, e);
        }
    }

    private static int parsePort(String port)
    {
        try
        {
            return Integer.parseInt(port);
        }
        catch (NumberFormatException e)
        {
            String msg = "Failed to parse port: '" + port + "' is not a valid port number.";
            throw new IllegalArgumentException(msg, e);
        }
    }
}
