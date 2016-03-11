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
import static org.junit.Assert.assertFalse;
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
    public void testGetSettingOrDefault_GivenSystemPropertyShouldMatchString()
    {
         System.setProperty("testproperty", "testvalue");

         assertTrue(PrelertSettings.isSet("testproperty"));
         assertEquals("testvalue", PrelertSettings.getSettingOrDefault("testproperty", "default"));
    }

    @Test
    public void testGetSettingOrDefault_GivenSystemPropertyShouldMatchInteger()
    {
         System.setProperty("testproperty", "42");

         assertTrue(PrelertSettings.isSet("testproperty"));
         assertEquals(new Integer(42), PrelertSettings.getSettingOrDefault("testproperty", 4));
    }

    @Test
    public void testGetSettingOrDefault_GivenSystemPropertyShouldMatchLong()
    {
         System.setProperty("testproperty", "42");

         assertTrue(PrelertSettings.isSet("testproperty"));
         assertEquals(new Long(42), PrelertSettings.getSettingOrDefault("testproperty", 4L));
    }

    @Test
    public void testGetSettingOrDefault_GivenSystemPropertyShouldMatchFloat()
    {
         System.setProperty("testproperty", "42.2");

         assertTrue(PrelertSettings.isSet("testproperty"));
         assertEquals(new Float(42.2), PrelertSettings.getSettingOrDefault("testproperty", 3.14f));
    }

    @Test
    public void testGetSettingOrDefault_GivenSystemPropertyShouldMatchDouble()
    {
         System.setProperty("testproperty", "42.2");

         assertTrue(PrelertSettings.isSet("testproperty"));
         assertEquals(new Double(42.2), PrelertSettings.getSettingOrDefault("testproperty", 3.14));
    }

    @Test
    public void testGetSettingOrDefault_GivenSystemPropertyDoesNotMatchType()
    {
         System.setProperty("testproperty", "a string");

         assertTrue(PrelertSettings.isSet("testproperty"));
         assertEquals(new Integer(3), PrelertSettings.getSettingOrDefault("testproperty", 3));
    }

    @Test
    public void testGetSetting_GivenNoSystemProperty()
    {
         System.clearProperty("testproperty");

         assertFalse(PrelertSettings.isSet("testproperty"));
         assertEquals("default", PrelertSettings.getSettingOrDefault("testproperty", "default"));
    }

    @Test
    public void testGetSetting_GivenEnvironmentSetting()
    {
         System.clearProperty("prelert.home");

         // Don't mess with the $PRELERT_HOME environment variable as this could
         // have side effects.  When tests run as part of full builds it will be
         // set.
         String prelertHomeEnv = System.getenv("PRELERT_HOME");
         if (prelertHomeEnv == null)
         {
             assertFalse(PrelertSettings.isSet("prelert.home"));
             assertEquals(".", PrelertSettings.getSettingOrDefault("prelert.home", "."));
         }
         else
         {
             assertTrue(PrelertSettings.isSet("prelert.home"));
             assertEquals(prelertHomeEnv, PrelertSettings.getSettingOrDefault("prelert.home", "."));
         }
    }
}
