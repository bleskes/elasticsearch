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
package com.prelert.server.info;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Utility class for writing the server info state.
 *
 * @see ServerInfoFactory
 */
public class ServerInfoWriter
{
    private static Logger LOGGER = Logger.getLogger(ServerInfoWriter.class);

    private ServerInfoFactory m_ServerInfo;
    private File m_File;

    public ServerInfoWriter(ServerInfoFactory factory, File file)
    {
        m_ServerInfo = factory;
        m_File = file;
    }

    /**
     * Get the static server info and append them to <code>file</code>.
     */
    public void writeInfo()
    {
        ServerInfo info = m_ServerInfo.serverInfo();
        ObjectWriter jsonWriter = new ObjectMapper()
                    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                    .writer().withDefaultPrettyPrinter();

        // append to file
        try (FileOutputStream fos = new FileOutputStream(m_File, true))
        {
            jsonWriter.writeValue(fos, info);
        }
        catch (IOException e )
        {
            LOGGER.error("Error writing server info to file: " + m_File.getPath(), e);
        }
    }

    /**
     * Get the current server stats and append them to <code>file</code>.
     */
    public void writeStats()
    {
        // The json writer closes the outputstream after its been used
        // so it has to be reopened here
        try (FileOutputStream fos = new FileOutputStream(m_File, true))
        {
            LOGGER.info("Writing host status and stats to " + m_File.getPath());
            fos.write(m_ServerInfo.serverStats().getBytes(StandardCharsets.UTF_8));
        }
        catch (IOException e )
        {
            LOGGER.error("Error writing server stats to file: " + m_File.getPath(), e);
        }
    }


}
