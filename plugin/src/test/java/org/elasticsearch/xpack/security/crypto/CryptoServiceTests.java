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

package org.elasticsearch.xpack.security.crypto;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.elasticsearch.Version;
import org.elasticsearch.common.settings.MockSecureSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.XPackPlugin;
import org.elasticsearch.xpack.watcher.Watcher;
import org.junit.Before;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class CryptoServiceTests extends ESTestCase {
    private Settings settings;
    private Environment env;
    private Path keyFile;

    @Before
    public void init() throws Exception {
        Path home = createTempDir();
        Path xpackConf = home.resolve("config").resolve(XPackPlugin.NAME);
        Files.createDirectories(xpackConf);
        keyFile = xpackConf.resolve("system_key");
        Files.write(keyFile, CryptoService.generateKey());
        settings = Settings.builder()
                .put("resource.reload.interval.high", "2s")
                .put("xpack.security.system_key.required", randomBoolean())
                .put("path.home", home)
                .build();
        env = new Environment(settings);
    }

    public void testSigned() throws Exception {
        // randomize whether to use a system key or not
        Settings settings = randomBoolean() ? this.settings : Settings.EMPTY;
        CryptoService service = new CryptoService(settings, env);
        String text = randomAlphaOfLength(10);
        String signed = service.sign(text, Version.CURRENT);
        assertThat(service.isSigned(signed), is(true));
        assertWarnings("The system_key will be removed in 6.0 in favor of TLS for encryption and authentication.");
    }

    public void testSignAndUnsign() throws Exception {
        CryptoService service = new CryptoService(settings, env);
        String text = randomAlphaOfLength(10);
        String signed = service.sign(text, Version.CURRENT);
        assertThat(text.equals(signed), is(false));
        String text2 = service.unsignAndVerify(signed, Version.CURRENT);
        assertThat(text, equalTo(text2));

        String signedOld = service.sign(text, Version.V_5_2_0);
        assertNotEquals(text, signedOld);
        assertEquals(signed, signedOld);
        String unsigned = service.unsignAndVerify(signedOld, Version.V_5_2_0);
        assertEquals(text, unsigned);
        assertWarnings("The system_key will be removed in 6.0 in favor of TLS for encryption and authentication.");
    }

    public void testSignAndUnsignNoKeyFile() throws Exception {
        Files.delete(keyFile);
        CryptoService service = new CryptoService(Settings.EMPTY, env);
        final String text = randomAlphaOfLength(10);
        String signed = service.sign(text, Version.CURRENT);
        assertEquals(text, signed);
        String unsigned = service.unsignAndVerify(signed, Version.CURRENT);
        assertEquals(text, unsigned);

        signed = service.sign(text, Version.V_5_2_0);
        assertNotEquals(text, signed);
        unsigned = service.unsignAndVerify(signed, Version.V_5_2_0);
        assertEquals(text, unsigned);
    }

    public void testTamperedSignature() throws Exception {
        CryptoService service = new CryptoService(settings, env);
        String text = randomAlphaOfLength(10);
        String signed = service.sign(text, Version.CURRENT);
        int i = signed.indexOf("$$", 2);
        int length = Integer.parseInt(signed.substring(2, i));
        String fakeSignature = randomAlphaOfLength(length);
        String fakeSignedText = "$$" + length + "$$" + fakeSignature + signed.substring(i + 2 + length);

        try {
            service.unsignAndVerify(fakeSignedText, Version.CURRENT);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is(equalTo("tampered signed text")));
            assertThat(e.getCause(), is(nullValue()));
        }
        assertWarnings("The system_key will be removed in 6.0 in favor of TLS for encryption and authentication.");
    }

    public void testTamperedSignatureOneChar() throws Exception {
        CryptoService service = new CryptoService(settings, env);
        String text = randomAlphaOfLength(10);
        String signed = service.sign(text, Version.CURRENT);
        int i = signed.indexOf("$$", 2);
        int length = Integer.parseInt(signed.substring(2, i));
        StringBuilder fakeSignature = new StringBuilder(signed.substring(i + 2, i + 2 + length));
        fakeSignature.setCharAt(randomIntBetween(0, fakeSignature.length() - 1), randomAlphaOfLength(1).charAt(0));

        String fakeSignedText = "$$" + length + "$$" + fakeSignature.toString() + signed.substring(i + 2 + length);

        try {
            service.unsignAndVerify(fakeSignedText, Version.CURRENT);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is(equalTo("tampered signed text")));
            assertThat(e.getCause(), is(nullValue()));
        }
        assertWarnings("The system_key will be removed in 6.0 in favor of TLS for encryption and authentication.");
    }

    public void testTamperedSignatureLength() throws Exception {
        CryptoService service = new CryptoService(settings, env);
        String text = randomAlphaOfLength(10);
        String signed = service.sign(text, Version.CURRENT);
        int i = signed.indexOf("$$", 2);
        int length = Integer.parseInt(signed.substring(2, i));
        String fakeSignature = randomAlphaOfLength(length);

        // Smaller sig length
        String fakeSignedText = "$$" + randomIntBetween(0, length - 1) + "$$" + fakeSignature + signed.substring(i + 2 + length);

        try {
            service.unsignAndVerify(fakeSignedText, Version.CURRENT);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is(equalTo("tampered signed text")));
        }

        // Larger sig length
        fakeSignedText = "$$" + randomIntBetween(length + 1, Integer.MAX_VALUE) + "$$" + fakeSignature + signed.substring(i + 2 + length);
        try {
            service.unsignAndVerify(fakeSignedText, Version.CURRENT);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is(equalTo("tampered signed text")));
            assertThat(e.getCause(), is(nullValue()));
        }
        assertWarnings("The system_key will be removed in 6.0 in favor of TLS for encryption and authentication.");
    }

    public void testEncryptionAndDecryptionChars() throws Exception {
        CryptoService service = new CryptoService(settings, env);
        assertThat(service.isEncryptionEnabled(), is(true));
        final char[] chars = randomAlphaOfLengthBetween(0, 1000).toCharArray();
        final char[] encrypted = service.encrypt(chars);
        assertThat(encrypted, notNullValue());
        assertThat(Arrays.equals(encrypted, chars), is(false));

        final char[] decrypted = service.decrypt(encrypted);
        assertThat(Arrays.equals(chars, decrypted), is(true));
        assertWarnings("The system_key will be removed in 6.0 in favor of TLS for encryption and authentication.");
    }

    public void testEncryptionAndDecryptionCharsWithoutKey() throws Exception {
        Files.delete(keyFile);
        CryptoService service = new CryptoService(Settings.EMPTY, env);
        assertThat(service.isEncryptionEnabled(), is(false));
        final char[] chars = randomAlphaOfLengthBetween(0, 1000).toCharArray();
        final char[] encryptedChars = service.encrypt(chars);
        final char[] decryptedChars = service.decrypt(encryptedChars);
        assertThat(chars, equalTo(encryptedChars));
        assertThat(chars, equalTo(decryptedChars));
    }

    public void testEncryptionEnabledWithKey() throws Exception {
        CryptoService service = new CryptoService(settings, env);
        assertThat(service.isEncryptionEnabled(), is(true));
        assertWarnings("The system_key will be removed in 6.0 in favor of TLS for encryption and authentication.");
    }

    public void testEncryptionEnabledWithoutKey() throws Exception {
        Files.delete(keyFile);
        CryptoService service = new CryptoService(Settings.EMPTY, env);
        assertThat(service.isEncryptionEnabled(), is(false));
    }

    public void testEncryptedChar() throws Exception {
        CryptoService service = new CryptoService(settings, env);
        assertThat(service.isEncryptionEnabled(), is(true));

        assertThat(service.isEncrypted((char[]) null), is(false));
        assertThat(service.isEncrypted(new char[0]), is(false));
        assertThat(service.isEncrypted(new char[CryptoService.ENCRYPTED_TEXT_PREFIX.length()]), is(false));
        assertThat(service.isEncrypted(CryptoService.ENCRYPTED_TEXT_PREFIX.toCharArray()), is(true));
        assertThat(service.isEncrypted(randomAlphaOfLengthBetween(0, 100).toCharArray()), is(false));
        assertThat(service.isEncrypted(service.encrypt(randomAlphaOfLength(10).toCharArray())), is(true));
        assertWarnings("The system_key will be removed in 6.0 in favor of TLS for encryption and authentication.");
    }

    public void testSigningKeyCanBeRecomputedConsistently() {
        final SecretKey systemKey = new SecretKeySpec(CryptoService.generateKey(), CryptoService.KEY_ALGO);
        final SecretKey randomKey = CryptoService.generateSecretKey(CryptoService.RANDOM_KEY_SIZE);
        int iterations = randomInt(100);
        final SecretKey signingKey = CryptoService.createSigningKey(systemKey, randomKey);
        for (int i = 0; i < iterations; i++) {
            SecretKey regenerated = CryptoService.createSigningKey(systemKey, randomKey);
            assertThat(regenerated, equalTo(signingKey));
        }
    }

    public void testSystemKeyFileRequired() throws Exception {
        Files.delete(keyFile);
        Settings customSettings = Settings.builder().put(settings).put("xpack.security.system_key.required", true).build();
        FileNotFoundException fnfe = expectThrows(FileNotFoundException.class, () -> new CryptoService(customSettings, env));
        assertThat(fnfe.getMessage(), containsString("must be present with a valid key"));
    }

    public void testEmptySystemKeyFile() throws Exception {
        // delete and create empty file
        Files.delete(keyFile);
        Files.createFile(keyFile);
        assertTrue(Files.exists(keyFile));
        IllegalArgumentException iae = expectThrows(IllegalArgumentException.class, () -> new CryptoService(settings, env));
        assertThat(iae.getMessage(), containsString("Empty key"));
        assertWarnings("The system_key will be removed in 6.0 in favor of TLS for encryption and authentication.");
    }

    public void testSystemKeyRequiredWithKeyInKeystore() throws Exception {
        MockSecureSettings mockSecureSettings = new MockSecureSettings();
        byte[] keyBytes = Files.readAllBytes(keyFile);
        Files.delete(keyFile);
        mockSecureSettings.setFile(Watcher.ENCRYPTION_KEY_SETTING.getKey(), keyBytes);
        settings = Settings.builder()
                .put(settings)
                .put(CryptoService.SYSTEM_KEY_REQUIRED_SETTING.getKey(), true)
                .setSecureSettings(mockSecureSettings)
                .build();
        env = new Environment(settings);
        new CryptoService(settings, env);
    }
}
