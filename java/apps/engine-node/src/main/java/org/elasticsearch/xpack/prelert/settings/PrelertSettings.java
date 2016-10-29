
package org.elasticsearch.xpack.prelert.settings;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.env.Environment;
import org.elasticsearch.xpack.prelert.PrelertPlugin;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Wrapper for Prelert settings.
 * <p>
 * The majority of these can be set either via a config file or via a JVM system
 * property. In the event of both being specified the JVM system property takes
 * precedence.
 * <p>
 * A smaller number of settings are configured via either a JVM system property
 * or an environment variable.  These are settings where a change would have
 * far-reaching effects on the layout of an installation, and we want to
 * discourage post-installation changes.  These are currently:
 * <ol>
 * <li>prelert.home</li>
 * <li>prelert.logs</li>
 * </ol>
 * <p>
 * The class name is prefixed with Prelert to avoid confusion with one of the
 * many other Settings classes out there.
 */
public final class PrelertSettings {
    private static final Logger LOGGER = Loggers.getLogger(PrelertSettings.class);

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

    /**
     * Mappers for String values to be converted into the key {@code clazz}
     * Supports:
     * <ul>
     * <li>Integer
     * <li>Long
     * <li>Float
     * <li>Double
     * <li>Boolean
     * </ul>
     */
    private static final java.util.Map<Class<?>, java.util.function.Function<String, Object>> MAPPERS;

    private static volatile boolean ms_LoadedFile;
    private static java.util.Map<Object, Object> ms_FileSettings;

    private static final java.util.Map<String, String> ENVIRONMENT_SETTINGS;

    static {
        ENVIRONMENT_SETTINGS = new java.util.HashMap<>();
        ENVIRONMENT_SETTINGS.put(PRELERT_HOME_PROPERTY, PRELERT_HOME_ENV);
        ENVIRONMENT_SETTINGS.put(PRELERT_LOGS_PROPERTY, PRELERT_LOGS_ENV);

        MAPPERS = new java.util.HashMap<>();
        MAPPERS.put(Integer.class, Integer::valueOf);
        MAPPERS.put(Long.class, Long::valueOf);
        MAPPERS.put(Float.class, Float::valueOf);
        MAPPERS.put(Double.class, Double::valueOf);
        MAPPERS.put(Boolean.class, Boolean::valueOf);
    }

    private PrelertSettings() {
        // Do nothing
    }

    /**
     * Returns {@code true} if a setting with the given {@code settingName} is set
     *
     * @param settingName Name of the setting to check if it is set
     * @return {@code true} if a setting with the given {@code settingName} is set
     */
    public static boolean isSet(Environment env, String settingName) {
        return getSetting(env, settingName, s -> s) != null;
    }

    /**
     * Get a setting or return the specified {@code defaultValue}. If a setting
     * is set but its type does not match the type of the {@code defaultValue},
     * the latter will be returned.
     *
     * @param settingName  Name of the setting to get
     * @param defaultValue The default value
     * @return The setting value, or the supplied default value if no setting is
     * present or its type does not match that of the default value
     */
    public static <T> T getSettingOrDefault(Environment env, String settingName, T defaultValue) {
        java.util.Objects.requireNonNull(defaultValue);

        @SuppressWarnings("unchecked")
        Class<T> resultType = (Class<T>) defaultValue.getClass();

        Object setting = getSetting(env, settingName, MAPPERS.getOrDefault(resultType, s -> s));
        if (setting != null && resultType.isInstance(setting)) {
            return resultType.cast(setting);
        } else if (setting != null) {
            LOGGER.warn("Cannot apply value " + setting.toString() + " for setting '" + settingName
                    + "': expected value of type " + resultType.getName() + ". Default value "
                    + defaultValue + " is going to be used instead.");
        }
        return defaultValue;
    }

    /**
     * <p>
     * Looks up a setting in the following order:
     * </p>
     * <ol>
     * <li> system properties
     * <li> environment variables (only for a set of pre-specified settings)
     * <li> settings YAML file
     * </ol>
     * If a system property is found, it is converted using the {@code mapper}
     *
     * @param settingName Name of the setting to get
     * @param mapper      A mapper that is used to convert a system property String value
     * @return The retrieved setting
     */
    private static Object getSetting(Environment env, String settingName, java.util.function.Function<String, Object> mapper) {
        // System properties always take precedence
        String prop = System.getProperty(settingName);
        if (prop != null) {
            try {
                return mapper.apply(prop);
            } catch (NumberFormatException e) {
                return prop;
            }
        }

        // Is this one of the special properties where the second choice is the
        // environment?
        String correspondingEnvVar = ENVIRONMENT_SETTINGS.get(settingName);
        if (correspondingEnvVar != null) {
            return System.getenv().get(correspondingEnvVar);
        }

        // This is a setting where we'll accept config file values
        return getFileSettings(env).get(settingName);
    }

    /**
     * Get file settings.  These are lazy loaded, once per run.
     */
    private static java.util.Map<Object, Object> getFileSettings(Environment env) {
        if (ms_LoadedFile) {
            return ms_FileSettings;
        }

        synchronized (ENGINE_CONFIG_FILE) {
            // Double check in case multiple threads waited to enter the
            // synchronized block
            if (!ms_LoadedFile) {
                Path configFile = PrelertPlugin.resolveConfigFile(env, ENGINE_CONFIG_FILE);
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
     *
     * @param configFile The config file to attempt to load.
     * @return Map of settings file content; empty on error.
     */
    static java.util.Map<Object, Object> loadSettingsFile(Path configFile) {
        try (java.io.InputStream input = Files.newInputStream(configFile)) {
            java.util.Map<Object, Object> fileSettings = parseSettings(input);
            if (fileSettings != null) {
                return fileSettings;
            }
        } catch (java.io.FileNotFoundException fnfe) {
            LOGGER.warn("Config file " + configFile + " not found.");
        } catch (YAMLException ye) {
            LOGGER.error("Syntax error in config file " + configFile + ": " + ye.getMessage());
        } catch (java.io.IOException | RuntimeException e) {
            LOGGER.error("Error loading configuration settings from " + configFile + ".", e);
        }

        LOGGER.warn("Only settings supplied as system properties will be used.");

        return java.util.Collections.emptyMap();
    }

    /**
     * Parse the config file.  The caller is responsible for opening and closing
     * the supplied stream.  This method has package visibility for testing.
     *
     * @param input An input stream containing the YAML configuration to be parsed.
     * @return Map of settings file content.
     * @throws YAMLException if there is a syntax error in the YAML.
     */
    @SuppressWarnings("unchecked")
    static java.util.Map<Object, Object> parseSettings(java.io.InputStream input) {
        Yaml yaml = new Yaml(new SafeConstructor());
        return (java.util.Map<Object, Object>) yaml.load(input);
    }
}
