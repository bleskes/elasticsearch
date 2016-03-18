/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2014] Elasticsearch Incorporated. All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Elasticsearch Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Elasticsearch Incorporated
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Elasticsearch Incorporated.
 */

package org.elasticsearch.shield;

import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.shield.audit.index.IndexAuditTrail;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.XPackPlugin;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.not;

public class ShieldPluginSettingsTests extends ESTestCase {

    private static final String TRIBE_T1_SHIELD_ENABLED = "tribe.t1." + XPackPlugin.featureEnabledSetting(Shield.NAME);
    private static final String TRIBE_T2_SHIELD_ENABLED = "tribe.t2." + XPackPlugin.featureEnabledSetting(Shield.NAME);

    public void testShieldIsMandatoryOnTribes() {
        Settings settings = Settings.builder().put("tribe.t1.cluster.name", "non_existing")
                .put("tribe.t2.cluster.name", "non_existing").build();

        Shield shield = new Shield(settings);

        Settings additionalSettings = shield.additionalSettings();


        assertThat(additionalSettings.getAsArray("tribe.t1.plugin.mandatory", null), arrayContaining(XPackPlugin.NAME));
        assertThat(additionalSettings.getAsArray("tribe.t2.plugin.mandatory", null), arrayContaining(XPackPlugin.NAME));
    }

    public void testAdditionalMandatoryPluginsOnTribes() {
        Settings settings = Settings.builder().put("tribe.t1.cluster.name", "non_existing")
                .putArray("tribe.t1.plugin.mandatory", "test_plugin").build();

        Shield shield = new Shield(settings);

        //simulate what PluginsService#updatedSettings does to make sure we don't override existing mandatory plugins
        try {
            Settings.builder().put(settings).put(shield.additionalSettings()).build();
            fail("shield cannot change the value of a setting that is already defined, so a exception should be thrown");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString(XPackPlugin.NAME));
            assertThat(e.getMessage(), containsString("plugin.mandatory"));
        }
    }

    public void testMandatoryPluginsOnTribesShieldAlreadyMandatory() {
        Settings settings = Settings.builder().put("tribe.t1.cluster.name", "non_existing")
                .putArray("tribe.t1.plugin.mandatory", "test_plugin", XPackPlugin.NAME).build();

        Shield shield = new Shield(settings);

        //simulate what PluginsService#updatedSettings does to make sure we don't override existing mandatory plugins
        Settings finalSettings = Settings.builder().put(settings).put(shield.additionalSettings()).build();

        String[] finalMandatoryPlugins = finalSettings.getAsArray("tribe.t1.plugin.mandatory", null);
        assertThat(finalMandatoryPlugins, notNullValue());
        assertThat(finalMandatoryPlugins.length, equalTo(2));
        assertThat(finalMandatoryPlugins[0], equalTo("test_plugin"));
        assertThat(finalMandatoryPlugins[1], equalTo(XPackPlugin.NAME));
    }

    public void testShieldIsEnabledByDefaultOnTribes() {
        Settings settings = Settings.builder().put("tribe.t1.cluster.name", "non_existing")
                .put("tribe.t2.cluster.name", "non_existing").build();

        Shield shield = new Shield(settings);

        Settings additionalSettings = shield.additionalSettings();

        assertThat(additionalSettings.getAsBoolean(TRIBE_T1_SHIELD_ENABLED, null), equalTo(true));
        assertThat(additionalSettings.getAsBoolean(TRIBE_T2_SHIELD_ENABLED, null), equalTo(true));
    }

    public void testShieldDisabledOnATribe() {
        Settings settings = Settings.builder().put("tribe.t1.cluster.name", "non_existing")
                .put(TRIBE_T1_SHIELD_ENABLED, false)
                .put("tribe.t2.cluster.name", "non_existing").build();

        Shield shield = new Shield(settings);

        try {
            shield.additionalSettings();
            fail("shield cannot change the value of a setting that is already defined, so a exception should be thrown");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString(TRIBE_T1_SHIELD_ENABLED));
        }
    }

    public void testShieldDisabledOnTribesShieldAlreadyMandatory() {
        Settings settings = Settings.builder().put("tribe.t1.cluster.name", "non_existing")
                .put(TRIBE_T1_SHIELD_ENABLED, false)
                .put("tribe.t2.cluster.name", "non_existing")
                .putArray("tribe.t1.plugin.mandatory", "test_plugin", XPackPlugin.NAME).build();

        Shield shield = new Shield(settings);

        try {
            shield.additionalSettings();
            fail("shield cannot change the value of a setting that is already defined, so a exception should be thrown");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString(TRIBE_T1_SHIELD_ENABLED));
        }
    }

    public void testShieldSettingsCopiedForTribeNodes() {
        Settings settings = Settings.builder()
                .put("tribe.t1.cluster.name", "non_existing")
                .put("tribe.t2.cluster.name", "non_existing")
                .put("shield.foo", "bar")
                .put("shield.bar", "foo")
                .putArray("shield.something.else.here", new String[] { "foo", "bar" })
                .build();

        Shield shield = new Shield(settings);
        Settings additionalSettings = shield.additionalSettings();

        assertThat(additionalSettings.get("shield.foo"), nullValue());
        assertThat(additionalSettings.get("shield.bar"), nullValue());
        assertThat(additionalSettings.getAsArray("shield.something.else.here"), is(Strings.EMPTY_ARRAY));
        assertThat(additionalSettings.get("tribe.t1.shield.foo"), is("bar"));
        assertThat(additionalSettings.get("tribe.t1.shield.bar"), is("foo"));
        assertThat(additionalSettings.getAsArray("tribe.t1.shield.something.else.here"), arrayContaining("foo", "bar"));
        assertThat(additionalSettings.get("tribe.t2.shield.foo"), is("bar"));
        assertThat(additionalSettings.get("tribe.t2.shield.bar"), is("foo"));
        assertThat(additionalSettings.getAsArray("tribe.t2.shield.something.else.here"), arrayContaining("foo", "bar"));
    }

    public void testValidAutoCreateIndex() {
        Shield.validateAutoCreateIndex(Settings.EMPTY);
        Shield.validateAutoCreateIndex(Settings.builder().put("action.auto_create_index", true).build());

        try {
            Shield.validateAutoCreateIndex(Settings.builder().put("action.auto_create_index", false).build());
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString(ShieldTemplateService.SECURITY_INDEX_NAME));
            assertThat(e.getMessage(), not(containsString(IndexAuditTrail.INDEX_NAME_PREFIX)));
        }

        Shield.validateAutoCreateIndex(Settings.builder().put("action.auto_create_index", ".security").build());
        Shield.validateAutoCreateIndex(Settings.builder().put("action.auto_create_index", "*s*").build());
        Shield.validateAutoCreateIndex(Settings.builder().put("action.auto_create_index", ".s*").build());

        try {
            Shield.validateAutoCreateIndex(Settings.builder().put("action.auto_create_index", "foo").build());
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString(ShieldTemplateService.SECURITY_INDEX_NAME));
            assertThat(e.getMessage(), not(containsString(IndexAuditTrail.INDEX_NAME_PREFIX)));
        }

        try {
            Shield.validateAutoCreateIndex(Settings.builder().put("action.auto_create_index", ".shield_audit_log*").build());
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString(ShieldTemplateService.SECURITY_INDEX_NAME));
        }

        Shield.validateAutoCreateIndex(Settings.builder()
                        .put("action.auto_create_index", ".security")
                        .put("shield.audit.enabled", true)
                        .build());

        try {
            Shield.validateAutoCreateIndex(Settings.builder()
                    .put("action.auto_create_index", ".security")
                    .put("shield.audit.enabled", true)
                    .put("shield.audit.outputs", randomFrom("index", "logfile,index"))
                    .build());
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString(ShieldTemplateService.SECURITY_INDEX_NAME));
            assertThat(e.getMessage(), containsString(IndexAuditTrail.INDEX_NAME_PREFIX));
        }

        Shield.validateAutoCreateIndex(Settings.builder()
                .put("action.auto_create_index", ".shield_audit_log*,.security")
                .put("shield.audit.enabled", true)
                .put("shield.audit.outputs", randomFrom("index", "logfile,index"))
                .build());
    }
}
