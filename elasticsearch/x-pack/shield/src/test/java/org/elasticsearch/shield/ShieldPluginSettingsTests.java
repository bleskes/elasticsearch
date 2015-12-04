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

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.test.ESTestCase;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.arrayContaining;

public class ShieldPluginSettingsTests extends ESTestCase {
    private static final String TRIBE_T1_SHIELD_ENABLED = "tribe.t1." + ShieldPlugin.ENABLED_SETTING_NAME;
    private static final String TRIBE_T2_SHIELD_ENABLED = "tribe.t2." + ShieldPlugin.ENABLED_SETTING_NAME;

    public void testShieldIsMandatoryOnTribes() {
        Settings settings = Settings.builder().put("tribe.t1.cluster.name", "non_existing")
                .put("tribe.t2.cluster.name", "non_existing").build();

        ShieldPlugin shieldPlugin = new ShieldPlugin(settings);

        Settings additionalSettings = shieldPlugin.additionalSettings();


        assertThat(additionalSettings.getAsArray("tribe.t1.plugin.mandatory", null), arrayContaining(ShieldPlugin.NAME));
        assertThat(additionalSettings.getAsArray("tribe.t2.plugin.mandatory", null), arrayContaining(ShieldPlugin.NAME));
    }

    public void testAdditionalMandatoryPluginsOnTribes() {
        Settings settings = Settings.builder().put("tribe.t1.cluster.name", "non_existing")
                .putArray("tribe.t1.plugin.mandatory", "test_plugin").build();

        ShieldPlugin shieldPlugin = new ShieldPlugin(settings);

        //simulate what PluginsService#updatedSettings does to make sure we don't override existing mandatory plugins
        try {
            Settings.builder().put(settings).put(shieldPlugin.additionalSettings()).build();
            fail("shield cannot change the value of a setting that is already defined, so a exception should be thrown");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("shield"));
            assertThat(e.getMessage(), containsString("plugin.mandatory"));
        }
    }

    public void testMandatoryPluginsOnTribesShieldAlreadyMandatory() {
        Settings settings = Settings.builder().put("tribe.t1.cluster.name", "non_existing")
                .putArray("tribe.t1.plugin.mandatory", "test_plugin", ShieldPlugin.NAME).build();

        ShieldPlugin shieldPlugin = new ShieldPlugin(settings);

        //simulate what PluginsService#updatedSettings does to make sure we don't override existing mandatory plugins
        Settings finalSettings = Settings.builder().put(settings).put(shieldPlugin.additionalSettings()).build();

        String[] finalMandatoryPlugins = finalSettings.getAsArray("tribe.t1.plugin.mandatory", null);
        assertThat(finalMandatoryPlugins, notNullValue());
        assertThat(finalMandatoryPlugins.length, equalTo(2));
        assertThat(finalMandatoryPlugins[0], equalTo("test_plugin"));
        assertThat(finalMandatoryPlugins[1], equalTo(ShieldPlugin.NAME));
    }

    public void testShieldIsEnabledByDefaultOnTribes() {
        Settings settings = Settings.builder().put("tribe.t1.cluster.name", "non_existing")
                .put("tribe.t2.cluster.name", "non_existing").build();

        ShieldPlugin shieldPlugin = new ShieldPlugin(settings);

        Settings additionalSettings = shieldPlugin.additionalSettings();

        assertThat(additionalSettings.getAsBoolean(TRIBE_T1_SHIELD_ENABLED, null), equalTo(true));
        assertThat(additionalSettings.getAsBoolean(TRIBE_T2_SHIELD_ENABLED, null), equalTo(true));
    }

    public void testShieldDisabledOnATribe() {
        Settings settings = Settings.builder().put("tribe.t1.cluster.name", "non_existing")
                .put(TRIBE_T1_SHIELD_ENABLED, false)
                .put("tribe.t2.cluster.name", "non_existing").build();

        ShieldPlugin shieldPlugin = new ShieldPlugin(settings);

        try {
            shieldPlugin.additionalSettings();
            fail("shield cannot change the value of a setting that is already defined, so a exception should be thrown");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString(TRIBE_T1_SHIELD_ENABLED));
        }
    }

    public void testShieldDisabledOnTribesShieldAlreadyMandatory() {
        Settings settings = Settings.builder().put("tribe.t1.cluster.name", "non_existing")
                .put(TRIBE_T1_SHIELD_ENABLED, false)
                .put("tribe.t2.cluster.name", "non_existing")
                .putArray("tribe.t1.plugin.mandatory", "test_plugin", ShieldPlugin.NAME).build();

        ShieldPlugin shieldPlugin = new ShieldPlugin(settings);

        try {
            shieldPlugin.additionalSettings();
            fail("shield cannot change the value of a setting that is already defined, so a exception should be thrown");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString(TRIBE_T1_SHIELD_ENABLED));
        }
    }
}
