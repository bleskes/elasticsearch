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

package com.prelert.job.logging;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class DefaultJobLoggerFactoryTest
{
    @Rule
    public TemporaryFolder m_LogDir = new TemporaryFolder();

    private DefaultJobLoggerFactory m_Factory;

    @Before
    public void setUp()
    {
        m_Factory = new DefaultJobLoggerFactory(m_LogDir.getRoot().getAbsolutePath());
    }

    @Test
    public void testCreateWriteAndCloseLogger() throws IOException
    {
        Logger logger = m_Factory.newLogger("foo");
        logger.info("Job logger in action");

        Path logFilePath = Paths.get(m_LogDir.getRoot().getAbsolutePath(), "foo", "engine_api.log");
        String content = new String(Files.readAllBytes(logFilePath));
        assertTrue(content.contains("INFO  foo - Job logger in action"));

        m_Factory.close("foo", logger);
    }

    @Test
    public void testCreateMultiple() throws IOException
    {
        // Create 3 loggers for the same job and write to using all 3 of them
        Logger logger1 = m_Factory.newLogger("bar");
        Logger logger2 = m_Factory.newLogger("bar");
        Logger logger3 = m_Factory.newLogger("bar");
        logger1.info("1");
        logger2.info("2");
        logger3.info("3");

        // Close them one by one and keep writing to one of them. All statements should be
        // written except for the one written after the last close
        m_Factory.close("bar", logger1);
        logger3.info("4");
        m_Factory.close("bar", logger2);
        logger3.info("5");
        m_Factory.close("bar", logger3);
        logger3.info("6");

        Path logFilePath = Paths.get(m_LogDir.getRoot().getAbsolutePath(), "bar", "engine_api.log");
        String content = new String(Files.readAllBytes(logFilePath));
        assertTrue(content.contains("INFO  bar - 1"));
        assertTrue(content.contains("INFO  bar - 2"));
        assertTrue(content.contains("INFO  bar - 3"));
        assertTrue(content.contains("INFO  bar - 4"));
        assertTrue(content.contains("INFO  bar - 5"));
        assertFalse(content.contains("INFO  bar - 6"));
    }
}
