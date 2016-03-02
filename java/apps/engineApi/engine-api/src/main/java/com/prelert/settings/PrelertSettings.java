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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
// NB: Using SnakeYAML because that's what Elasticsearch uses and it makes our
// installer bundle smaller if we use the same as them.  If Elasticsearch
// changes then we should change too.
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;


/**
 * Wrapper for Prelert settings.
 *
 * The majority of these can be set either via a config file or via a JVM system
 * property.  In the event of both being specified the JVM system property takes
 * precedence.
 *
 * A smaller number of settings are configured via either a JVM system property
 * or an environment variable.  These are settings where a change would have
 * far-reaching effects on the layout of an installation, and we want to
 * discourage post-installation changes.  These are currently:
 * <ol>
 *   <li>prelert.home</li>
 *   <li>prelert.logs</li>
 * </ol>
 *
 * The class name is prefixed with Prelert to avoid confusion with one of the
 * many other Settings classes out there.
 */
public final class PrelertSettings
{
    private static final Logger LOGGER = Logger.getLogger(PrelertSettings.class);

    /**
     * Name of the System property containing the value of Prelert Home
     */
    public static final String PRELERT_HOME_PROPERTY = "prelert.home";

    /**
     * Name of the environment variable PRELERT_HOME
     */
    public static final String PRELERT_HOME_ENV = "PRELERT_HOME";

    /**
     * Default Prelert Home if not specified
     */
    public static final String DEFAULT_PRELERT_HOME = ".";

    /**
     * Name of the System property path to the logs directory
     */
    public static final String PRELERT_LOGS_PROPERTY = "prelert.logs";

    /**
     * Name of the environment variable PRELERT_LOGS
     */
    public static final String PRELERT_LOGS_ENV = "PRELERT_LOGS";

    /**
     * Default Prelert logs directory if not specified
     */
    public static final String DEFAULT_PRELERT_LOGS = "logs";

    /**
     * Name of Engine API config directory, relative to Prelert Home.
     */
    public static final String ENGINE_CONFIG_DIRECTORY = "config";

    /**
     * Name of Engine API config file.
     */
    public static final String ENGINE_CONFIG_FILE = "engine_api.yml";

    private static volatile boolean ms_LoadedFile;
    private static Map<Object, Object> ms_FileSettings;

    private static final Map<String, String> ENVIRONMENT_SETTINGS;

    static
    {
        ENVIRONMENT_SETTINGS = new HashMap<>();
        ENVIRONMENT_SETTINGS.put(PRELERT_HOME_PROPERTY, PRELERT_HOME_ENV);
        ENVIRONMENT_SETTINGS.put(PRELERT_LOGS_PROPERTY, PRELERT_LOGS_ENV);
    }

    private PrelertSettings()
    {
        // Do nothing
    }

    /**
     * Get a setting in its natural form.  The caller should know the expected
     * form, and is responsible for validating that the actual form is as
     * expected.
     * @param settingName Name of the setting to get.
     * @return The setting value, or null if not set.
     */
    public static Object getSetting(String settingName)
    {
        return getSetting(settingName, null);
    }

    /**
     * Get a setting in its natural form.  The caller should know the expected
     * form, and is responsible for validating that the actual form is as
     * expected.
     * @param settingName Name of the setting to get.
     * @return The setting value, or the supplied default value if not set.
     */
    public static Object getSetting(String settingName, Object defaultValue)
    {
        // System properties always take precedence
        Object prop = System.getProperty(settingName);
        if (prop != null)
        {
            return prop;
        }

        // Is this one of the special properties where the second choice is the
        // environment?
        String correspondingEnvVar = ENVIRONMENT_SETTINGS.get(settingName);
        if (correspondingEnvVar != null)
        {
            if (System.getenv().containsKey(correspondingEnvVar))
            {
                return System.getenv().get(correspondingEnvVar);
            }
            return defaultValue;
        }

        // This is a setting where we'll accept config file values
        return getFileSettings().getOrDefault(settingName, defaultValue);
    }

    /**
     * Get a setting in String form.  This will convert a setting to a string
     * even if it wasn't a string in the underlying config file.
     * @param settingName Name of the setting to get.
     * @return The setting value converted to a string, or null if not set.
     */
    public static String getSettingText(String settingName)
    {
        return getSettingText(settingName, null);
    }

    /**
     * Get a setting in String form.  This will convert a setting to a string
     * even if it wasn't a string in the underlying config file.
     * @param settingName Name of the setting to get.
     * @return The setting value converted to a string, or the supplied default
     * value if not set.
     */
    public static String getSettingText(String settingName, String defaultValue)
    {
        Object setting = getSetting(settingName, defaultValue);
        if (setting instanceof String)
        {
            return (String) setting;
        }
        return "" + setting;
    }

    /**
     * Get file settings.  These are lazy loaded, once per run.
     */
    private static Map<Object, Object> getFileSettings()
    {
        if (ms_LoadedFile)
        {
            return ms_FileSettings;
        }

        synchronized (ENGINE_CONFIG_FILE)
        {
            // Double check in case multiple threads waited to enter the
            // synchronized block
            if (!ms_LoadedFile)
            {
                File configFile = new File(
                        new File(getSettingText(PRELERT_HOME_PROPERTY, DEFAULT_PRELERT_HOME),
                                ENGINE_CONFIG_DIRECTORY),
                        ENGINE_CONFIG_FILE);
                ms_FileSettings = loadSettingsFile(configFile);
                ms_LoadedFile = true;
            }
        }

        return ms_FileSettings;
    }

    /**
     * Load the config file.  The intention is that this should only be called
     * once per run of the Engine API, however, only inefficiency will result
     * from calling it more than once.   This method has package visibility for
     * testing.
     * @param configFile The config file to attempt to load.
     * @return Map of settings file content; empty on error.
     */
    static Map<Object, Object> loadSettingsFile(File configFile)
    {
        try (InputStream input = new FileInputStream(configFile))
        {
            Map<Object, Object> fileSettings = parseSettings(input);
            if (fileSettings != null)
            {
                return fileSettings;
            }
        }
        catch (FileNotFoundException fnfe)
        {
            LOGGER.warn("Config file " + configFile + " not found.");
        }
        catch (YAMLException ye)
        {
            LOGGER.error("Syntax error in config file " + configFile + ": " + ye.getMessage());
        }
        catch (IOException | RuntimeException e)
        {
            LOGGER.error("Error loading configuration settings from " + configFile + ".", e);
        }

        LOGGER.warn("Only settings supplied as system properties will be used.");

        return Collections.emptyMap();
    }

    /**
     * Parse the config file.  The caller is responsible for opening and closing
     * the supplied stream.  This method has package visibility for testing.
     * @param input An input stream containing the YAML configuration to be parsed.
     * @return Map of settings file content.
     * @throws YAMLException if there is a syntax error in the YAML.
     */
    @SuppressWarnings("unchecked")
    static Map<Object, Object> parseSettings(InputStream input)
    {
        Yaml yaml = new Yaml(new SafeConstructor());
        return (Map<Object, Object>)yaml.load(input);
    }

}
