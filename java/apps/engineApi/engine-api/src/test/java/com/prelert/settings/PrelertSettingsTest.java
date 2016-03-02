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

package com.prelert.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.yaml.snakeyaml.error.YAMLException;


public class PrelertSettingsTest
{

    @Rule
    public ExpectedException m_ExpectedException = ExpectedException.none();

    @Test
    public void testParseSettings_GivenEmpty() throws IOException
    {
        String content = "";
        try (InputStream input = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)))
        {
            Map<Object, Object> settings = PrelertSettings.parseSettings(input);

            assertNull(settings);
        }
    }

    @Test
    public void testParseSettings_GivenSyntaxError() throws IOException
    {
        m_ExpectedException.expect(YAMLException.class);
        m_ExpectedException.expectMessage(
                "mapping values are not allowed here\n" +
                " in 'reader', line 1, column 12:\n" +
                "    name: value: unexpected\n" +
                "               ^\n");

        String content = "name: value: unexpected";
        try (InputStream input = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)))
        {
            PrelertSettings.parseSettings(input);
        }
    }

    @Test
    public void testParseSettings_GivenValid() throws IOException
    {
        String content = "name: value";
        try (InputStream input = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)))
        {
            Map<Object, Object> settings = PrelertSettings.parseSettings(input);

            assertNotNull(settings);
            assertEquals(1, settings.size());
            assertEquals("value", settings.get("name"));
        }
    }

    @Test
    public void testLoadFileSettings_GivenEmpty()
    {
        File configFile = new File(getClass().getResource("/empty_engine_api.yml").getFile());

        Map<Object, Object> settings = PrelertSettings.loadSettingsFile(configFile);

        assertNotNull(settings);
        assertTrue(settings.isEmpty());
    }

    @Test
    public void testLoadFileSettings_GivenAllComments()
    {
        File configFile = new File(getClass().getResource("/all_comment_engine_api.yml").getFile());

        Map<Object, Object> settings = PrelertSettings.loadSettingsFile(configFile);

        assertNotNull(settings);
        assertTrue(settings.isEmpty());
    }

    @Test
    public void testLoadFileSettings_GivenInvalid()
    {
        File configFile = new File(getClass().getResource("/invalid_engine_api.yml").getFile());

        Map<Object, Object> settings = PrelertSettings.loadSettingsFile(configFile);

        assertNotNull(settings);
        assertTrue(settings.isEmpty());
    }

    @Test
    public void testLoadFileSettings_GivenValid()
    {
        File configFile = new File(getClass().getResource("/valid_engine_api.yml").getFile());

        Map<Object, Object> settings = PrelertSettings.loadSettingsFile(configFile);

        assertNotNull(settings);
        assertEquals(4, settings.size());
        assertEquals("somewhere/jetty", settings.get("jetty.home"));
        assertEquals(new Double(6.0), settings.get("max.jobs.factor"));
        assertEquals(new Integer(26), settings.get("max.percent.date.errors"));
        assertEquals("9300-9400", settings.get("es.transport.tcp.port"));
    }

    @Test
    public void testLoadFileSettings_GivenNoSuchFile()
    {
        File configFile = new File("missing_engine_api.yml");

        Map<Object, Object> settings = PrelertSettings.loadSettingsFile(configFile);

        assertNotNull(settings);
        assertTrue(settings.isEmpty());
    }

    @Test
    public void testGetSetting_GivenSystemProperty()
    {
         System.setProperty("testproperty", "testvalue");

         Object valueNoDefault = PrelertSettings.getSetting("testproperty");
         Object valueGivenDefault = PrelertSettings.getSetting("testproperty", "default");
         String strValueNoDefault = PrelertSettings.getSettingText("testproperty");
         String strValueGivenDefault = PrelertSettings.getSettingText("testproperty", "default");

         assertEquals("testvalue", valueNoDefault);
         assertEquals("testvalue", valueGivenDefault);
         assertEquals("testvalue", strValueNoDefault);
         assertEquals("testvalue", strValueGivenDefault);
    }

    @Test
    public void testGetSetting_GivenNoSystemProperty()
    {
         System.clearProperty("testproperty");

         Object valueNoDefault = PrelertSettings.getSetting("testproperty");
         Object valueGivenDefault = PrelertSettings.getSetting("testproperty", "default");
         String strValueNoDefault = PrelertSettings.getSettingText("testproperty");
         String strValueGivenDefault = PrelertSettings.getSettingText("testproperty", "default");

         assertNull(valueNoDefault);
         assertEquals("default", valueGivenDefault);
         assertEquals("null", strValueNoDefault);
         assertEquals("default", strValueGivenDefault);
    }

    @Test
    public void testGetSetting_GivenEnvironmentSetting()
    {
         System.clearProperty("prelert.home");

         Object valueNoDefault = PrelertSettings.getSetting("prelert.home");
         Object valueGivenDefault = PrelertSettings.getSetting("prelert.home", ".");
         String strValueNoDefault = PrelertSettings.getSettingText("prelert.home");
         String strValueGivenDefault = PrelertSettings.getSettingText("prelert.home", ".");

         // Don't mess with the $PRELERT_HOME environment variable as this could
         // have side effects.  When tests run as part of full builds it will be
         // set.
         String prelertHomeEnv = System.getenv("PRELERT_HOME");
         if (prelertHomeEnv == null)
         {
             assertNull(valueNoDefault);
             assertEquals(".", valueGivenDefault);
             assertEquals("null", strValueNoDefault);
             assertEquals(".", strValueGivenDefault);
         }
         else
         {
             assertEquals(prelertHomeEnv, valueNoDefault);
             assertEquals(prelertHomeEnv, valueGivenDefault);
             assertEquals(prelertHomeEnv, strValueNoDefault);
             assertEquals(prelertHomeEnv, strValueGivenDefault);
         }
    }
}
