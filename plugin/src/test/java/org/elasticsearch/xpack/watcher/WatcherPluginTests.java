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

package org.elasticsearch.xpack.watcher;

import org.elasticsearch.common.settings.MockSecureSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.security.crypto.CryptoService;

import static org.hamcrest.Matchers.containsString;

public class WatcherPluginTests extends ESTestCase {

    public void testValidAutoCreateIndex() {
        Watcher.validAutoCreateIndex(Settings.EMPTY);
        Watcher.validAutoCreateIndex(Settings.builder().put("action.auto_create_index", true).build());

        IllegalArgumentException exception = expectThrows(IllegalArgumentException.class,
                () -> Watcher.validAutoCreateIndex(Settings.builder().put("action.auto_create_index", false).build()));
        assertThat(exception.getMessage(), containsString("[.watches, .triggered_watches, .watcher-history-*]"));

        Watcher.validAutoCreateIndex(Settings.builder().put("action.auto_create_index",
                ".watches,.triggered_watches,.watcher-history*").build());
        Watcher.validAutoCreateIndex(Settings.builder().put("action.auto_create_index", "*w*").build());
        Watcher.validAutoCreateIndex(Settings.builder().put("action.auto_create_index", ".w*,.t*").build());

        exception = expectThrows(IllegalArgumentException.class,
                () -> Watcher.validAutoCreateIndex(Settings.builder().put("action.auto_create_index", ".watches").build()));
        assertThat(exception.getMessage(), containsString("[.watches, .triggered_watches, .watcher-history-*]"));

        exception = expectThrows(IllegalArgumentException.class,
                () -> Watcher.validAutoCreateIndex(Settings.builder().put("action.auto_create_index", ".triggered_watch").build()));
        assertThat(exception.getMessage(), containsString("[.watches, .triggered_watches, .watcher-history-*]"));

        exception = expectThrows(IllegalArgumentException.class,
                () -> Watcher.validAutoCreateIndex(Settings.builder().put("action.auto_create_index", ".watcher-history-*").build()));
        assertThat(exception.getMessage(), containsString("[.watches, .triggered_watches, .watcher-history-*]"));
    }

    public void testDeprecationLoggingEncryptionKeyMissing() {
        Settings settings = Settings.builder()
                .put("path.home", createTempDir())
                .put(Watcher.ENCRYPT_SENSITIVE_DATA_SETTING.getKey(), true)
                .build();
        new Watcher(settings);
        assertWarnings("The use of the system_key file for encrypting sensitive values is deprecated. In order to " +
                "continue using watches with encrypted data, execute the following command to store the key in the secure settings " +
                "store: 'bin/elasticsearch-keystore add-file " + Watcher.ENCRYPTION_KEY_SETTING.getKey() + " "
                + CryptoService.resolveSystemKey(new Environment(settings)) + "' on all of your nodes.");
    }

    public void testDeprecationLoggingEncryptionDisabled() {
        Settings settings = randomBoolean() ? Settings.EMPTY :
                Settings.builder().put(Watcher.ENCRYPT_SENSITIVE_DATA_SETTING.getKey(), false).build();
        new Watcher(settings);
        // test framework will assert there are no warnings
    }

    public void testDeprecationLoggingEncryptionKeyPresent() {
        MockSecureSettings secureSettings = new MockSecureSettings();
        secureSettings.setFile(Watcher.ENCRYPTION_KEY_SETTING.getKey(), new byte[] { 0x01 });
        Settings settings = randomBoolean() ? Settings.EMPTY :
                Settings.builder()
                        .put(Watcher.ENCRYPT_SENSITIVE_DATA_SETTING.getKey(), false)
                        .setSecureSettings(secureSettings)
                        .build();
        new Watcher(settings);
        // test framework will assert there are no warnings
    }
}
