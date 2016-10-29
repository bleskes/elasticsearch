
package org.elasticsearch.xpack.prelert.settings;

import org.elasticsearch.common.io.PathUtils;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.test.ESTestCase;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;


public class PrelertSettingsTest extends ESTestCase {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    public void testParseSettings_GivenEmpty() throws IOException {
        String content = "";
        try (InputStream input = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
            Map<Object, Object> settings = PrelertSettings.parseSettings(input);

            assertNull(settings);
        }
    }

    public void testParseSettings_GivenSyntaxError() throws IOException {
        expectedException.expect(YAMLException.class);
        expectedException.expectMessage(
                "mapping values are not allowed here\n" +
                        " in 'reader', line 1, column 12:\n" +
                        "    name: value: unexpected\n" +
                "               ^\n");

        String content = "name: value: unexpected";
        try (InputStream input = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
            PrelertSettings.parseSettings(input);
        }
    }

    public void testParseSettings_GivenValid() throws IOException {
        String content = "name: value";
        try (InputStream input = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
            Map<Object, Object> settings = PrelertSettings.parseSettings(input);

            assertNotNull(settings);
            assertEquals(1, settings.size());
            assertEquals("value", settings.get("name"));
        }
    }

    public void testLoadFileSettings_GivenEmpty() throws Exception {
        Path configFile = PathUtils.get(getClass().getResource("/settings/empty_engine_api.yml").toURI());

        Map<Object, Object> settings = PrelertSettings.loadSettingsFile(configFile);

        assertNotNull(settings);
        assertTrue(settings.isEmpty());
    }

    public void testLoadFileSettings_GivenAllComments() throws Exception {
        Path configFile = PathUtils.get(getClass().getResource("/settings/all_comment_engine_api.yml").toURI());

        Map<Object, Object> settings = PrelertSettings.loadSettingsFile(configFile);

        assertNotNull(settings);
        assertTrue(settings.isEmpty());
    }

    public void testLoadFileSettings_GivenInvalid() throws Exception {
        Path configFile = PathUtils.get(getClass().getResource("/settings/invalid_engine_api.yml").toURI());

        Map<Object, Object> settings = PrelertSettings.loadSettingsFile(configFile);

        assertNotNull(settings);
        assertTrue(settings.isEmpty());
    }

    public void testLoadFileSettings_GivenValid() throws Exception {
        Path configFile = PathUtils.get(getClass().getResource("/settings/valid_engine_api.yml").toURI());

        Map<Object, Object> settings = PrelertSettings.loadSettingsFile(configFile);

        assertNotNull(settings);
        assertEquals(4, settings.size());
        assertEquals("somewhere/jetty", settings.get("jetty.home"));
        assertEquals(new Double(6.0), settings.get("max.jobs.factor"));
        assertEquals(new Integer(26), settings.get("max.percent.date.errors"));
        assertEquals("9300-9400", settings.get("es.transport.tcp.port"));
    }

    public void testLoadFileSettings_GivenNoSuchFile() throws Exception {
        Path configFile = PathUtils.get(getClass().getResource("/settings/valid_engine_api.yml").toURI());

        Map<Object, Object> settings = PrelertSettings.loadSettingsFile(configFile);

        assertNotNull(settings);
        assertTrue(settings.isEmpty());
    }

    public void testGetSettingOrDefault_GivenSystemPropertyShouldMatchString() {
        Environment env = new Environment(Settings.EMPTY);
        System.setProperty("testproperty", "testvalue");

        assertTrue(PrelertSettings.isSet(env, "testproperty"));
        assertEquals("testvalue", PrelertSettings.getSettingOrDefault(env, "testproperty", "default"));
    }

    public void testGetSettingOrDefault_GivenSystemPropertyShouldMatchInteger() {
        Environment env = new Environment(Settings.EMPTY);
        System.setProperty("testproperty", "42");

        assertTrue(PrelertSettings.isSet(env, "testproperty"));
        assertEquals(new Integer(42), PrelertSettings.getSettingOrDefault(env, "testproperty", 4));
    }

    public void testGetSettingOrDefault_GivenSystemPropertyShouldMatchLong() {
        Environment env = new Environment(Settings.EMPTY);
        System.setProperty("testproperty", "42");

        assertTrue(PrelertSettings.isSet(env, "testproperty"));
        assertEquals(new Long(42), PrelertSettings.getSettingOrDefault(env, "testproperty", 4L));
    }

    public void testGetSettingOrDefault_GivenSystemPropertyShouldMatchFloat() {
        Environment env = new Environment(Settings.EMPTY);
        System.setProperty("testproperty", "42.2");

        assertTrue(PrelertSettings.isSet(env, "testproperty"));
        assertEquals(new Float(42.2), PrelertSettings.getSettingOrDefault(env, "testproperty", 3.14f));
    }

    public void testGetSettingOrDefault_GivenSystemPropertyShouldMatchDouble() {
        Environment env = new Environment(Settings.EMPTY);
        System.setProperty("testproperty", "42.2");

        assertTrue(PrelertSettings.isSet(env, "testproperty"));
        assertEquals(new Double(42.2), PrelertSettings.getSettingOrDefault(env, "testproperty", 3.14));
    }

    public void testGetSettingOrDefault_GivenSystemPropertyDoesNotMatchType() {
        Environment env = new Environment(Settings.EMPTY);
        System.setProperty("testproperty", "a string");

        assertTrue(PrelertSettings.isSet(env, "testproperty"));
        assertEquals(new Integer(3), PrelertSettings.getSettingOrDefault(env, "testproperty", 3));
    }

    public void testGetSetting_GivenNoSystemProperty() {
        Environment env = new Environment(Settings.EMPTY);
        System.clearProperty("testproperty");

        assertFalse(PrelertSettings.isSet(env, "testproperty"));
        assertEquals("default", PrelertSettings.getSettingOrDefault(env, "testproperty", "default"));
    }

    public void testGetSetting_GivenEnvironmentSetting() {
        Environment env = new Environment(Settings.EMPTY);
        System.clearProperty("prelert.home");

        // Don't mess with the $PRELERT_HOME environment variable as this could
        // have side effects.  When tests run as part of full builds it will be
        // set.
        String prelertHomeEnv = System.getenv("PRELERT_HOME");
        if (prelertHomeEnv == null) {
            assertFalse(PrelertSettings.isSet(env, "prelert.home"));
            assertEquals(".", PrelertSettings.getSettingOrDefault(env, "prelert.home", "."));
        } else {
            assertTrue(PrelertSettings.isSet(env, "prelert.home"));
            assertEquals(prelertHomeEnv, PrelertSettings.getSettingOrDefault(env, "prelert.home", "."));
        }
    }
}
