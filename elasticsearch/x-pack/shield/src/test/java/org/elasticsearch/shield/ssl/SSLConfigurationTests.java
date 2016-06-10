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

package org.elasticsearch.shield.ssl;

import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JcePEMEncryptorBuilder;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.env.Environment;
import org.elasticsearch.shield.ssl.SSLConfiguration.Custom;
import org.elasticsearch.shield.ssl.SSLConfiguration.Global;
import org.elasticsearch.shield.ssl.TrustConfig.Reloadable.Listener;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.threadpool.TestThreadPool;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.watcher.ResourceWatcherService;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509ExtendedTrustManager;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;

public class SSLConfigurationTests extends ESTestCase {

    public void testThatSSLConfigurationHasCorrectDefaults() {
        SSLConfiguration globalConfig = new Global(Settings.EMPTY);
        assertThat(globalConfig.keyConfig(), sameInstance(KeyConfig.NONE));
        assertThat(globalConfig.trustConfig(), is(not((globalConfig.keyConfig()))));
        assertThat(globalConfig.trustConfig(), instanceOf(StoreTrustConfig.class));
        assertThat(globalConfig.sessionCacheSize(), is(equalTo(Global.DEFAULT_SESSION_CACHE_SIZE)));
        assertThat(globalConfig.sessionCacheTimeout(), is(equalTo(Global.DEFAULT_SESSION_CACHE_TIMEOUT)));
        assertThat(globalConfig.protocol(), is(equalTo(Global.DEFAULT_PROTOCOL)));

        SSLConfiguration scopedConfig = new Custom(Settings.EMPTY, globalConfig);
        assertThat(scopedConfig.keyConfig(), sameInstance(globalConfig.keyConfig()));
        assertThat(scopedConfig.trustConfig(), sameInstance(globalConfig.trustConfig()));
        assertThat(globalConfig.sessionCacheSize(), is(equalTo(Global.DEFAULT_SESSION_CACHE_SIZE)));
        assertThat(globalConfig.sessionCacheTimeout(), is(equalTo(Global.DEFAULT_SESSION_CACHE_TIMEOUT)));
        assertThat(globalConfig.protocol(), is(equalTo(Global.DEFAULT_PROTOCOL)));
    }

    public void testThatSSLConfigurationWithoutAutoGenHasCorrectDefaults() {
        SSLConfiguration globalSettings = new Global(Settings.EMPTY);
        SSLConfiguration scopedSettings = new Custom(Settings.EMPTY, globalSettings);
        for (SSLConfiguration sslConfiguration : Arrays.asList(globalSettings, scopedSettings)) {
            assertThat(sslConfiguration.keyConfig(), sameInstance(KeyConfig.NONE));
            assertThat(sslConfiguration.sessionCacheSize(), is(equalTo(Global.DEFAULT_SESSION_CACHE_SIZE)));
            assertThat(sslConfiguration.sessionCacheTimeout(), is(equalTo(Global.DEFAULT_SESSION_CACHE_TIMEOUT)));
            assertThat(sslConfiguration.protocol(), is(equalTo(Global.DEFAULT_PROTOCOL)));
            assertThat(sslConfiguration.trustConfig(), notNullValue());
            assertThat(sslConfiguration.trustConfig(), is(instanceOf(StoreTrustConfig.class)));

            StoreTrustConfig ksTrustInfo = (StoreTrustConfig) sslConfiguration.trustConfig();
            assertThat(ksTrustInfo.trustStorePath, is(nullValue()));
            assertThat(ksTrustInfo.trustStorePassword, is(nullValue()));
            assertThat(ksTrustInfo.trustStoreAlgorithm, is(nullValue()));
        }
    }

    public void testThatOnlyKeystoreInSettingsSetsTruststoreSettings() {
        Settings settings = Settings.builder()
                .put("xpack.security.ssl.keystore.path", "path")
                .put("xpack.security.ssl.keystore.password", "password")
                .build();
        Settings profileSettings = settings.getByPrefix("xpack.security.ssl.");
        // Pass settings in as component settings
        SSLConfiguration globalSettings = new Global(settings);
        SSLConfiguration scopedSettings = new Custom(profileSettings, globalSettings);
        SSLConfiguration scopedEmptyGlobalSettings =
                new Custom(profileSettings, new Global(Settings.EMPTY));
        for (SSLConfiguration sslConfiguration : Arrays.asList(globalSettings, scopedSettings, scopedEmptyGlobalSettings)) {
            assertThat(sslConfiguration.keyConfig(), instanceOf(StoreKeyConfig.class));
            StoreKeyConfig ksKeyInfo = (StoreKeyConfig) sslConfiguration.keyConfig();

            assertThat(ksKeyInfo.keyStorePath, is(equalTo("path")));
            assertThat(ksKeyInfo.keyStorePassword, is(equalTo("password")));
            assertThat(ksKeyInfo.keyPassword, is(equalTo(ksKeyInfo.keyStorePassword)));
            assertThat(ksKeyInfo.keyStoreAlgorithm, is(KeyManagerFactory.getDefaultAlgorithm()));
            assertThat(sslConfiguration.trustConfig(), is(sameInstance(ksKeyInfo)));
        }
    }

    public void testThatKeyPasswordCanBeSet() {
        Settings settings = Settings.builder()
                .put("xpack.security.ssl.keystore.path", "path")
                .put("xpack.security.ssl.keystore.password", "password")
                .put("xpack.security.ssl.keystore.key_password", "key")
                .build();
        SSLConfiguration sslConfiguration = new Global(settings);
        assertThat(sslConfiguration.keyConfig(), instanceOf(StoreKeyConfig.class));
        StoreKeyConfig ksKeyInfo = (StoreKeyConfig) sslConfiguration.keyConfig();
        assertThat(ksKeyInfo.keyStorePassword, is(equalTo("password")));
        assertThat(ksKeyInfo.keyPassword, is(equalTo("key")));

        // Pass settings in as profile settings
        Settings profileSettings = settings.getByPrefix("xpack.security.ssl.");
        SSLConfiguration sslConfiguration1 = new Custom(profileSettings,
                randomBoolean() ? sslConfiguration : new Global(Settings.EMPTY));
        assertThat(sslConfiguration1.keyConfig(), instanceOf(StoreKeyConfig.class));
        ksKeyInfo = (StoreKeyConfig) sslConfiguration1.keyConfig();
        assertThat(ksKeyInfo.keyStorePassword, is(equalTo("password")));
        assertThat(ksKeyInfo.keyPassword, is(equalTo("key")));
    }


    public void testThatProfileSettingsOverrideServiceSettings() {
        Settings profileSettings = Settings.builder()
                .put("keystore.path", "path")
                .put("keystore.password", "password")
                .put("keystore.key_password", "key")
                .put("keystore.algorithm", "algo")
                .put("truststore.path", "trust path")
                .put("truststore.password", "password for trust")
                .put("truststore.algorithm", "trusted")
                .put("protocol", "ssl")
                .put("session.cache_size", "3")
                .put("session.cache_timeout", "10m")
                .build();

        Settings serviceSettings = Settings.builder()
                .put("xpack.security.ssl.keystore.path", "comp path")
                .put("xpack.security.ssl.keystore.password", "comp password")
                .put("xpack.security.ssl.keystore.key_password", "comp key")
                .put("xpack.security.ssl.keystore.algorithm", "comp algo")
                .put("xpack.security.ssl.truststore.path", "comp trust path")
                .put("xpack.security.ssl.truststore.password", "comp password for trust")
                .put("xpack.security.ssl.truststore.algorithm", "comp trusted")
                .put("xpack.security.ssl.protocol", "tls")
                .put("xpack.security.ssl.session.cache_size", "7")
                .put("xpack.security.ssl.session.cache_timeout", "20m")
                .build();

        SSLConfiguration globalSettings = new Global(serviceSettings);
        SSLConfiguration sslConfiguration = new Custom(profileSettings, globalSettings);
        assertThat(sslConfiguration.keyConfig(), instanceOf(StoreKeyConfig.class));
        StoreKeyConfig ksKeyInfo = (StoreKeyConfig) sslConfiguration.keyConfig();
        assertThat(ksKeyInfo.keyStorePath, is(equalTo("path")));
        assertThat(ksKeyInfo.keyStorePassword, is(equalTo("password")));
        assertThat(ksKeyInfo.keyPassword, is(equalTo("key")));
        assertThat(ksKeyInfo.keyStoreAlgorithm, is(equalTo("algo")));
        assertThat(sslConfiguration.trustConfig(), instanceOf(StoreTrustConfig.class));
        StoreTrustConfig ksTrustInfo = (StoreTrustConfig) sslConfiguration.trustConfig();
        assertThat(ksTrustInfo.trustStorePath, is(equalTo("trust path")));
        assertThat(ksTrustInfo.trustStorePassword, is(equalTo("password for trust")));
        assertThat(ksTrustInfo.trustStoreAlgorithm, is(equalTo("trusted")));
        assertThat(sslConfiguration.protocol(), is(equalTo("ssl")));
        assertThat(sslConfiguration.sessionCacheSize(), is(equalTo(3)));
        assertThat(sslConfiguration.sessionCacheTimeout(), is(equalTo(TimeValue.timeValueMinutes(10L))));
    }

    public void testThatEmptySettingsAreEqual() {
        SSLConfiguration sslConfiguration = new Global(Settings.EMPTY);
        SSLConfiguration sslConfiguration1 = new Global(Settings.EMPTY);
        assertThat(sslConfiguration.equals(sslConfiguration1), is(equalTo(true)));
        assertThat(sslConfiguration1.equals(sslConfiguration), is(equalTo(true)));
        assertThat(sslConfiguration.equals(sslConfiguration), is(equalTo(true)));
        assertThat(sslConfiguration1.equals(sslConfiguration1), is(equalTo(true)));

        SSLConfiguration profileSSLConfiguration = new Custom(Settings.EMPTY, sslConfiguration);
        assertThat(sslConfiguration.equals(profileSSLConfiguration), is(equalTo(true)));
        assertThat(profileSSLConfiguration.equals(sslConfiguration), is(equalTo(true)));
        assertThat(profileSSLConfiguration.equals(profileSSLConfiguration), is(equalTo(true)));
    }

    public void testThatSettingsWithDifferentKeystoresAreNotEqual() {
        SSLConfiguration sslConfiguration = new Global(Settings.builder()
                .put("xpack.security.ssl.keystore.path", "path").build());
        SSLConfiguration sslConfiguration1 = new Global(Settings.builder()
                .put("xpack.security.ssl.keystore.path", "path1").build());
        assertThat(sslConfiguration.equals(sslConfiguration1), is(equalTo(false)));
        assertThat(sslConfiguration1.equals(sslConfiguration), is(equalTo(false)));
        assertThat(sslConfiguration.equals(sslConfiguration), is(equalTo(true)));
        assertThat(sslConfiguration1.equals(sslConfiguration1), is(equalTo(true)));
    }

    public void testThatSettingsWithDifferentProtocolsAreNotEqual() {
        SSLConfiguration sslConfiguration = new Global(Settings.builder()
                .put("xpack.security.ssl.protocol", "ssl").build());
        SSLConfiguration sslConfiguration1 = new Global(Settings.builder()
                .put("xpack.security.ssl.protocol", "tls").build());
        assertThat(sslConfiguration.equals(sslConfiguration1), is(equalTo(false)));
        assertThat(sslConfiguration1.equals(sslConfiguration), is(equalTo(false)));
        assertThat(sslConfiguration.equals(sslConfiguration), is(equalTo(true)));
        assertThat(sslConfiguration1.equals(sslConfiguration1), is(equalTo(true)));
    }

    public void testThatSettingsWithDifferentTruststoresAreNotEqual() {
        SSLConfiguration sslConfiguration = new Global(Settings.builder()
                .put("xpack.security.ssl.truststore.path", "/trust").build());
        SSLConfiguration sslConfiguration1 = new Global(Settings.builder()
                .put("xpack.security.ssl.truststore.path", "/truststore").build());
        assertThat(sslConfiguration.equals(sslConfiguration1), is(equalTo(false)));
        assertThat(sslConfiguration1.equals(sslConfiguration), is(equalTo(false)));
        assertThat(sslConfiguration.equals(sslConfiguration), is(equalTo(true)));
        assertThat(sslConfiguration1.equals(sslConfiguration1), is(equalTo(true)));
    }

    public void testThatEmptySettingsHaveSameHashCode() {
        SSLConfiguration sslConfiguration = new Global(Settings.EMPTY);
        SSLConfiguration sslConfiguration1 = new Global(Settings.EMPTY);
        assertThat(sslConfiguration.hashCode(), is(equalTo(sslConfiguration1.hashCode())));

        SSLConfiguration profileSettings = new Custom(Settings.EMPTY, sslConfiguration);
        assertThat(profileSettings.hashCode(), is(equalTo(sslConfiguration.hashCode())));
    }

    public void testThatSettingsWithDifferentKeystoresHaveDifferentHashCode() {
        SSLConfiguration sslConfiguration = new Global(Settings.builder()
                .put("xpack.security.ssl.keystore.path", "path").build());
        SSLConfiguration sslConfiguration1 = new Global(Settings.builder()
                .put("xpack.security.ssl.keystore.path", "path1").build());
        assertThat(sslConfiguration.hashCode(), is(not(equalTo(sslConfiguration1.hashCode()))));
    }

    public void testThatSettingsWithDifferentProtocolsHaveDifferentHashCode() {
        SSLConfiguration sslConfiguration = new Global(Settings.builder()
                .put("xpack.security.ssl.protocol", "ssl").build());
        SSLConfiguration sslConfiguration1 = new Global(Settings.builder()
                .put("xpack.security.ssl.protocol", "tls").build());
        assertThat(sslConfiguration.hashCode(), is(not(equalTo(sslConfiguration1.hashCode()))));
    }

    public void testThatSettingsWithDifferentTruststoresHaveDifferentHashCode() {
        SSLConfiguration sslConfiguration = new Global(Settings.builder()
                .put("xpack.security.ssl.truststore.path", "/trust").build());
        SSLConfiguration sslConfiguration1 = new Global(Settings.builder()
                .put("xpack.security.ssl.truststore.path", "/truststore").build());
        assertThat(sslConfiguration.hashCode(), is(not(equalTo(sslConfiguration1.hashCode()))));
    }

    public void testConfigurationUsingPEMKeyFiles() {
        Environment env = randomBoolean() ? null :
                new Environment(Settings.builder().put("path.home", createTempDir()).build());
        Settings settings = Settings.builder()
                .put("xpack.security.ssl.key.path", getDataPath("/org/elasticsearch/shield/transport/ssl/certs/simple/testnode.pem"))
                .put("xpack.security.ssl.key.password", "testnode")
                .put("xpack.security.ssl.cert", getDataPath("/org/elasticsearch/shield/transport/ssl/certs/simple/testnode.crt"))
                .build();

        SSLConfiguration config = new Global(settings);
        assertThat(config.keyConfig(), instanceOf(PEMKeyConfig.class));
        PEMKeyConfig keyConfig = (PEMKeyConfig) config.keyConfig();
        KeyManager[] keyManagers = keyConfig.keyManagers(env, null, null);
        assertThat(keyManagers.length, is(1));
        assertThat(config.trustConfig(), sameInstance(keyConfig));
        TrustManager[] trustManagers = keyConfig.trustManagers(env, null, null);
        assertThat(trustManagers.length, is(1));
    }

    public void testConfigurationUsingPEMKeyAndTrustFiles() {
        Environment env = randomBoolean() ? null :
                new Environment(Settings.builder().put("path.home", createTempDir()).build());
        Settings settings = Settings.builder()
                .put("xpack.security.ssl.key.path", getDataPath("/org/elasticsearch/shield/transport/ssl/certs/simple/testnode.pem"))
                .put("xpack.security.ssl.key.password", "testnode")
                .put("xpack.security.ssl.cert", getDataPath("/org/elasticsearch/shield/transport/ssl/certs/simple/testnode.crt"))
                .putArray("xpack.security.ssl.ca",
                        getDataPath("/org/elasticsearch/shield/transport/ssl/certs/simple/testnode.crt").toString(),
                        getDataPath("/org/elasticsearch/shield/transport/ssl/certs/simple/testclient.crt").toString())
                .build();

        SSLConfiguration config = new Global(settings);
        assertThat(config.keyConfig(), instanceOf(PEMKeyConfig.class));
        PEMKeyConfig keyConfig = (PEMKeyConfig) config.keyConfig();
        KeyManager[] keyManagers = keyConfig.keyManagers(env, null, null);
        assertThat(keyManagers.length, is(1));
        assertThat(config.trustConfig(), not(sameInstance(keyConfig)));
        assertThat(config.trustConfig(), instanceOf(PEMTrustConfig.class));
        TrustManager[] trustManagers = keyConfig.trustManagers(env, null, null);
        assertThat(trustManagers.length, is(1));
    }

    public void testReloadingKeyStore() throws Exception {
        Environment env = randomBoolean() ? null :
                new Environment(Settings.builder().put("path.home", createTempDir()).build());
        Path tempDir = createTempDir();
        Path keystorePath = tempDir.resolve("testnode.jks");
        Files.copy(getDataPath("/org/elasticsearch/shield/transport/ssl/certs/simple/testnode.jks"), keystorePath);
        Settings settings = Settings.builder()
                .put("xpack.security.ssl.keystore.path", keystorePath)
                .put("xpack.security.ssl.keystore.password", "testnode")
                .put(Global.INCLUDE_JDK_CERTS_SETTING.getKey(), randomBoolean())
                .build();

        SSLConfiguration config = new Global(settings);
        assertThat(config.keyConfig(), instanceOf(StoreKeyConfig.class));
        StoreKeyConfig keyConfig = (StoreKeyConfig) config.keyConfig();

        CountDownLatch latch = new CountDownLatch(2);
        AtomicReference<Exception> exceptionRef = new AtomicReference<>();
        Listener listener = createRefreshListener(latch, exceptionRef);

        ThreadPool threadPool = new TestThreadPool("reload");
        try {
            ResourceWatcherService resourceWatcherService =
                    new ResourceWatcherService(Settings.builder().put("resource.reload.interval.high", "1s").build(), threadPool).start();
            KeyManager[] keyManagers = keyConfig.keyManagers(env, resourceWatcherService, listener);
            assertThat(keyManagers.length, is(1));
            assertThat(keyManagers[0], instanceOf(X509ExtendedKeyManager.class));
            X509ExtendedKeyManager keyManager = (X509ExtendedKeyManager) keyManagers[0];
            String[] aliases = keyManager.getServerAliases("RSA", null);
            assertNotNull(aliases);
            assertThat(aliases.length, is(1));
            assertThat(aliases[0], is("testnode"));
            TrustManager[] trustManagers = keyConfig.trustManagers(env, resourceWatcherService, listener);
            assertThat(trustManagers.length, is(1));
            assertThat(trustManagers[0], instanceOf(X509ExtendedTrustManager.class));
            X509ExtendedTrustManager trustManager = (X509ExtendedTrustManager) trustManagers[0];
            Certificate[] certificates = trustManager.getAcceptedIssuers();
            final int trustedCount = certificates.length;
            assertThat(latch.getCount(), is(2L));

            KeyStore keyStore = KeyStore.getInstance("jks");
            keyStore.load(null, null);
            Path updated = tempDir.resolve("updated.jks");
            try (OutputStream out = Files.newOutputStream(updated)) {
                keyStore.store(out, "testnode".toCharArray());
            }
            atomicMoveIfPossible(updated, keystorePath);
            latch.await();
            assertThat(exceptionRef.get(), is(nullValue()));
            aliases = keyManager.getServerAliases("RSA", null);
            assertThat(aliases, is(nullValue()));
            certificates = trustManager.getAcceptedIssuers();
            assertThat(trustedCount - certificates.length, is(5));
        } finally {
            threadPool.shutdown();
        }
    }

    public void testReloadingPEMKeyConfig() throws Exception {
        Environment env = randomBoolean() ? null :
                new Environment(Settings.builder().put("path.home", createTempDir()).build());
        Path tempDir = createTempDir();
        Path keyPath = tempDir.resolve("testnode.pem");
        Path certPath = tempDir.resolve("testnode.crt");
        Path clientCertPath = tempDir.resolve("testclient.crt");
        Files.copy(getDataPath("/org/elasticsearch/shield/transport/ssl/certs/simple/testnode.pem"), keyPath);
        Files.copy(getDataPath("/org/elasticsearch/shield/transport/ssl/certs/simple/testnode.crt"), certPath);
        Files.copy(getDataPath("/org/elasticsearch/shield/transport/ssl/certs/simple/testclient.crt"), clientCertPath);
        Settings settings = Settings.builder()
                .put("xpack.security.ssl.key.path", keyPath)
                .put("xpack.security.ssl.key.password", "testnode")
                .put("xpack.security.ssl.cert", certPath)
                .putArray("xpack.security.ssl.ca", certPath.toString(), clientCertPath.toString())
                .put(Global.INCLUDE_JDK_CERTS_SETTING.getKey(), randomBoolean())
                .build();
        SSLConfiguration config = new Global(settings);
        assertThat(config.keyConfig(), instanceOf(PEMKeyConfig.class));
        PEMKeyConfig keyConfig = (PEMKeyConfig) config.keyConfig();

        CountDownLatch latch = new CountDownLatch(2);
        AtomicReference<Exception> exceptionRef = new AtomicReference<>();
        Listener listener = createRefreshListener(latch, exceptionRef);

        ThreadPool threadPool = new TestThreadPool("reload pem");
        try {
            ResourceWatcherService resourceWatcherService =
                    new ResourceWatcherService(Settings.builder().put("resource.reload.interval.high", "1s").build(), threadPool).start();
            KeyManager[] keyManagers = keyConfig.keyManagers(env, resourceWatcherService, listener);
            assertThat(keyManagers.length, is(1));
            assertThat(keyManagers[0], instanceOf(X509ExtendedKeyManager.class));
            X509ExtendedKeyManager keyManager = (X509ExtendedKeyManager) keyManagers[0];
            String[] aliases = keyManager.getServerAliases("RSA", null);
            assertThat(aliases, is(notNullValue()));
            assertThat(aliases.length, is(1));
            assertThat(aliases[0], is("key"));
            PrivateKey privateKey = keyManager.getPrivateKey(aliases[0]);
            TrustManager[] trustManagers = keyConfig.trustManagers(env, resourceWatcherService, listener);
            assertThat(trustManagers.length, is(1));
            assertThat(trustManagers[0], instanceOf(X509ExtendedTrustManager.class));
            X509ExtendedTrustManager trustManager = (X509ExtendedTrustManager) trustManagers[0];
            Certificate[] certificates = trustManager.getAcceptedIssuers();
            final int trustedCount = certificates.length;
            assertThat(latch.getCount(), is(2L));

            // make sure we wait enough to see a change. if time is within a second the file may not be seen as modified since the size is
            // the same!
            awaitBusy(() -> {
                try {
                    BasicFileAttributes attributes = Files.readAttributes(keyPath, BasicFileAttributes.class);
                    return System.currentTimeMillis() - attributes.lastModifiedTime().toMillis() >= 1000L;
                } catch (IOException e) {
                    throw new ElasticsearchException("io exception while checking time", e);
                }
            });
            Path updatedKeyPath = tempDir.resolve("updated.pem");
            KeyPair keyPair = CertUtils.generateKeyPair();
            try (OutputStream os = Files.newOutputStream(updatedKeyPath);
                 OutputStreamWriter osWriter = new OutputStreamWriter(os, StandardCharsets.UTF_8);
                 JcaPEMWriter writer = new JcaPEMWriter(osWriter)) {
                writer.writeObject(keyPair,
                        new JcePEMEncryptorBuilder("DES-EDE3-CBC").setProvider(CertUtils.BC_PROV).build("testnode".toCharArray()));
            }
            atomicMoveIfPossible(updatedKeyPath, keyPath);

            latch.await();
            assertThat(exceptionRef.get(), is(nullValue()));
            aliases = keyManager.getServerAliases("RSA", null);
            assertThat(aliases, is(notNullValue()));
            assertThat(aliases.length, is(1));
            assertThat(aliases[0], is("key"));
            assertThat(keyManager.getPrivateKey(aliases[0]), not(equalTo(privateKey)));
            assertThat(keyManager.getPrivateKey(aliases[0]), is(equalTo(keyPair.getPrivate())));
            certificates = trustManager.getAcceptedIssuers();
            assertThat(trustedCount - certificates.length, is(0));
        } finally {
            threadPool.shutdown();
        }
    }

    public void testReloadingTrustStore() throws Exception {
        Environment env = randomBoolean() ? null :
                new Environment(Settings.builder().put("path.home", createTempDir()).build());
        Path tempDir = createTempDir();
        Path trustStorePath = tempDir.resolve("testnode.jks");
        Files.copy(getDataPath("/org/elasticsearch/shield/transport/ssl/certs/simple/testnode.jks"), trustStorePath);
        Settings settings = Settings.builder()
                .put("xpack.security.ssl.truststore.path", trustStorePath)
                .put("xpack.security.ssl.truststore.password", "testnode")
                .put(Global.INCLUDE_JDK_CERTS_SETTING.getKey(), randomBoolean())
                .build();

        SSLConfiguration config = new Global(settings);
        assertThat(config.trustConfig(), instanceOf(StoreTrustConfig.class));
        StoreTrustConfig trustConfig = (StoreTrustConfig) config.trustConfig();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> exceptionRef = new AtomicReference<>();
        Listener listener = createRefreshListener(latch, exceptionRef);

        ThreadPool threadPool = new TestThreadPool("reload");
        try {
            ResourceWatcherService resourceWatcherService =
                    new ResourceWatcherService(Settings.builder().put("resource.reload.interval.high", "1s").build(), threadPool).start();
            TrustManager[] trustManagers = trustConfig.trustManagers(env, resourceWatcherService, listener);
            assertThat(trustManagers.length, is(1));
            assertThat(trustManagers[0], instanceOf(X509ExtendedTrustManager.class));
            X509ExtendedTrustManager trustManager = (X509ExtendedTrustManager) trustManagers[0];
            Certificate[] certificates = trustManager.getAcceptedIssuers();
            final int trustedCount = certificates.length;

            assertThat(latch.getCount(), is(1L));

            Path updatedTruststore = tempDir.resolve("updated.jks");
            KeyStore keyStore = KeyStore.getInstance("jks");
            keyStore.load(null, null);
            try (OutputStream out = Files.newOutputStream(updatedTruststore)) {
                keyStore.store(out, "testnode".toCharArray());
            }
            atomicMoveIfPossible(updatedTruststore, trustStorePath);
            latch.await();
            assertThat(exceptionRef.get(), is(nullValue()));
            certificates = trustManager.getAcceptedIssuers();
            assertThat(trustedCount - certificates.length, is(5));
        } finally {
            threadPool.shutdown();
        }
    }

    public void testReloadingPEMTrustConfig() throws Exception {
        Environment env = randomBoolean() ? null :
                new Environment(Settings.builder().put("path.home", createTempDir()).build());
        Path tempDir = createTempDir();
        Path clientCertPath = tempDir.resolve("testclient.crt");
        Files.copy(getDataPath("/org/elasticsearch/shield/transport/ssl/certs/simple/testclient.crt"), clientCertPath);
        Settings settings = Settings.builder()
                .putArray("xpack.security.ssl.ca", clientCertPath.toString())
                .put(Global.INCLUDE_JDK_CERTS_SETTING.getKey(), false)
                .build();
        SSLConfiguration config = new Global(settings);
        assertThat(config.trustConfig(), instanceOf(PEMTrustConfig.class));
        PEMTrustConfig trustConfig = (PEMTrustConfig) config.trustConfig();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> exceptionRef = new AtomicReference<>();
        Listener listener = createRefreshListener(latch, exceptionRef);

        ThreadPool threadPool = new TestThreadPool("reload");
        try {
            ResourceWatcherService resourceWatcherService =
                    new ResourceWatcherService(Settings.builder().put("resource.reload.interval.high", "1s").build(), threadPool).start();
            TrustManager[] trustManagers = trustConfig.trustManagers(env, resourceWatcherService, listener);
            assertThat(trustManagers.length, is(1));
            assertThat(trustManagers[0], instanceOf(X509ExtendedTrustManager.class));
            X509ExtendedTrustManager trustManager = (X509ExtendedTrustManager) trustManagers[0];
            Certificate[] certificates = trustManager.getAcceptedIssuers();
            assertThat(certificates.length, is(1));
            assertThat(((X509Certificate)certificates[0]).getSubjectX500Principal().getName(), containsString("Test Client"));
            assertThat(latch.getCount(), is(1L));

            Path updatedCert = tempDir.resolve("updated.crt");
            Files.copy(getDataPath("/org/elasticsearch/shield/transport/ssl/certs/simple/testnode.crt"), updatedCert,
                    StandardCopyOption.REPLACE_EXISTING);
            atomicMoveIfPossible(updatedCert, clientCertPath);
            latch.await();
            assertThat(exceptionRef.get(), is(nullValue()));
            Certificate[] updatedCerts = trustManager.getAcceptedIssuers();
            assertThat(updatedCerts.length, is(1));
            assertThat(((X509Certificate)updatedCerts[0]).getSubjectX500Principal().getName(), containsString("Test Node"));
            assertThat(updatedCerts[0], not(equalTo(certificates[0])));
        } finally {
            threadPool.shutdown();
        }
    }

    public void testReloadingKeyStoreException() throws Exception {
        Environment env = randomBoolean() ? null :
                new Environment(Settings.builder().put("path.home", createTempDir()).build());
        Path tempDir = createTempDir();
        Path keystorePath = tempDir.resolve("testnode.jks");
        Files.copy(getDataPath("/org/elasticsearch/shield/transport/ssl/certs/simple/testnode.jks"), keystorePath);
        Settings settings = Settings.builder()
                .put("xpack.security.ssl.keystore.path", keystorePath)
                .put("xpack.security.ssl.keystore.password", "testnode")
                .put(Global.INCLUDE_JDK_CERTS_SETTING.getKey(), randomBoolean())
                .build();

        SSLConfiguration config = new Global(settings);
        assertThat(config.keyConfig(), instanceOf(StoreKeyConfig.class));
        StoreKeyConfig keyConfig = (StoreKeyConfig) config.keyConfig();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> exceptionRef = new AtomicReference<>();
        Listener listener = createRefreshListener(latch, exceptionRef);

        ThreadPool threadPool = new TestThreadPool("reload");
        try {
            ResourceWatcherService resourceWatcherService =
                    new ResourceWatcherService(Settings.builder().put("resource.reload.interval.high", "1s").build(), threadPool).start();
            KeyManager[] keyManagers = keyConfig.keyManagers(env, resourceWatcherService, listener);
            X509ExtendedKeyManager keyManager = (X509ExtendedKeyManager) keyManagers[0];
            String[] aliases = keyManager.getServerAliases("RSA", null);
            assertNotNull(aliases);
            assertThat(aliases.length, is(1));
            assertThat(aliases[0], is("testnode"));
            assertThat(latch.getCount(), is(1L));

            // truncate the keystore
            try (OutputStream out = Files.newOutputStream(keystorePath)) {
            }
            latch.await();
            assertThat(exceptionRef.get(), notNullValue());
            assertThat(exceptionRef.get(), instanceOf(ElasticsearchException.class));
            assertThat(keyManager.getServerAliases("RSA", null), equalTo(aliases));
        } finally {
            threadPool.shutdown();
        }
    }

    public void testReloadingPEMKeyConfigException() throws Exception {
        Environment env = randomBoolean() ? null :
                new Environment(Settings.builder().put("path.home", createTempDir()).build());
        Path tempDir = createTempDir();
        Path keyPath = tempDir.resolve("testnode.pem");
        Path certPath = tempDir.resolve("testnode.crt");
        Path clientCertPath = tempDir.resolve("testclient.crt");
        Files.copy(getDataPath("/org/elasticsearch/shield/transport/ssl/certs/simple/testnode.pem"), keyPath);
        Files.copy(getDataPath("/org/elasticsearch/shield/transport/ssl/certs/simple/testnode.crt"), certPath);
        Files.copy(getDataPath("/org/elasticsearch/shield/transport/ssl/certs/simple/testclient.crt"), clientCertPath);
        Settings settings = Settings.builder()
                .put("xpack.security.ssl.key.path", keyPath)
                .put("xpack.security.ssl.key.password", "testnode")
                .put("xpack.security.ssl.cert", certPath)
                .putArray("xpack.security.ssl.ca", certPath.toString(), clientCertPath.toString())
                .put(Global.INCLUDE_JDK_CERTS_SETTING.getKey(), randomBoolean())
                .build();
        SSLConfiguration config = new Global(settings);
        assertThat(config.keyConfig(), instanceOf(PEMKeyConfig.class));
        PEMKeyConfig keyConfig = (PEMKeyConfig) config.keyConfig();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> exceptionRef = new AtomicReference<>();
        Listener listener = createRefreshListener(latch, exceptionRef);

        ThreadPool threadPool = new TestThreadPool("reload pem");
        try {
            ResourceWatcherService resourceWatcherService =
                    new ResourceWatcherService(Settings.builder().put("resource.reload.interval.high", "1s").build(), threadPool).start();
            KeyManager[] keyManagers = keyConfig.keyManagers(env, resourceWatcherService, listener);
            assertThat(keyManagers.length, is(1));
            assertThat(keyManagers[0], instanceOf(X509ExtendedKeyManager.class));
            X509ExtendedKeyManager keyManager = (X509ExtendedKeyManager) keyManagers[0];
            String[] aliases = keyManager.getServerAliases("RSA", null);
            assertThat(aliases, is(notNullValue()));
            assertThat(aliases.length, is(1));
            assertThat(aliases[0], is("key"));
            PrivateKey privateKey = keyManager.getPrivateKey(aliases[0]);
            assertThat(latch.getCount(), is(1L));

            // pick a random file to truncate
            Path toTruncate = randomFrom(keyPath, certPath);

            // truncate the file
            try (OutputStream os = Files.newOutputStream(toTruncate)) {
            }

            latch.await();
            assertThat(exceptionRef.get(), is(instanceOf(ElasticsearchException.class)));
            assertThat(keyManager.getServerAliases("RSA", null), equalTo(aliases));
            assertThat(keyManager.getPrivateKey(aliases[0]), is(equalTo(privateKey)));
        } finally {
            threadPool.shutdown();
        }
    }

    public void testTrustStoreReloadException() throws Exception {
        Environment env = randomBoolean() ? null :
                new Environment(Settings.builder().put("path.home", createTempDir()).build());
        Path tempDir = createTempDir();
        Path trustStorePath = tempDir.resolve("testnode.jks");
        Files.copy(getDataPath("/org/elasticsearch/shield/transport/ssl/certs/simple/testnode.jks"), trustStorePath);
        Settings settings = Settings.builder()
                .put("xpack.security.ssl.truststore.path", trustStorePath)
                .put("xpack.security.ssl.truststore.password", "testnode")
                .put(Global.INCLUDE_JDK_CERTS_SETTING.getKey(), randomBoolean())
                .build();

        SSLConfiguration config = new Global(settings);
        assertThat(config.trustConfig(), instanceOf(StoreTrustConfig.class));
        StoreTrustConfig trustConfig = (StoreTrustConfig) config.trustConfig();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> exceptionRef = new AtomicReference<>();
        Listener listener = createRefreshListener(latch, exceptionRef);

        ThreadPool threadPool = new TestThreadPool("reload");
        try {
            ResourceWatcherService resourceWatcherService =
                    new ResourceWatcherService(Settings.builder().put("resource.reload.interval.high", "1s").build(), threadPool).start();
            TrustManager[] trustManagers = trustConfig.trustManagers(env, resourceWatcherService, listener);
            assertThat(trustManagers.length, is(1));
            assertThat(trustManagers[0], instanceOf(X509ExtendedTrustManager.class));
            X509ExtendedTrustManager trustManager = (X509ExtendedTrustManager) trustManagers[0];
            Certificate[] certificates = trustManager.getAcceptedIssuers();

            // truncate the truststore
            try (OutputStream os = Files.newOutputStream(trustStorePath)) {
            }

            latch.await();
            assertThat(exceptionRef.get(), instanceOf(ElasticsearchException.class));
            assertThat(trustManager.getAcceptedIssuers(), equalTo(certificates));
        } finally {
            threadPool.shutdown();
        }
    }

    public void testPEMTrustReloadException() throws Exception {
        Environment env = randomBoolean() ? null :
                new Environment(Settings.builder().put("path.home", createTempDir()).build());
        Path tempDir = createTempDir();
        Path clientCertPath = tempDir.resolve("testclient.crt");
        Files.copy(getDataPath("/org/elasticsearch/shield/transport/ssl/certs/simple/testclient.crt"), clientCertPath);
        Settings settings = Settings.builder()
                .putArray("xpack.security.ssl.ca", clientCertPath.toString())
                .put(Global.INCLUDE_JDK_CERTS_SETTING.getKey(), false)
                .build();
        SSLConfiguration config = new Global(settings);
        assertThat(config.trustConfig(), instanceOf(PEMTrustConfig.class));
        PEMTrustConfig trustConfig = (PEMTrustConfig) config.trustConfig();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> exceptionRef = new AtomicReference<>();
        Listener listener = createRefreshListener(latch, exceptionRef);

        ThreadPool threadPool = new TestThreadPool("reload");
        try {
            ResourceWatcherService resourceWatcherService =
                    new ResourceWatcherService(Settings.builder().put("resource.reload.interval.high", "1s").build(), threadPool).start();
            TrustManager[] trustManagers = trustConfig.trustManagers(env, resourceWatcherService, listener);
            assertThat(trustManagers.length, is(1));
            assertThat(trustManagers[0], instanceOf(X509ExtendedTrustManager.class));
            X509ExtendedTrustManager trustManager = (X509ExtendedTrustManager) trustManagers[0];
            Certificate[] certificates = trustManager.getAcceptedIssuers();
            assertThat(certificates.length, is(1));
            assertThat(((X509Certificate) certificates[0]).getSubjectX500Principal().getName(), containsString("Test Client"));
            assertThat(latch.getCount(), is(1L));

            // write bad file
            Path updatedCert = tempDir.resolve("updated.crt");
            try (OutputStream os = Files.newOutputStream(updatedCert)) {
                os.write(randomByte());
            }
            atomicMoveIfPossible(updatedCert, clientCertPath);

            latch.await();
            assertThat(exceptionRef.get(), instanceOf(ElasticsearchException.class));
            assertThat(trustManager.getAcceptedIssuers(), equalTo(certificates));
        } finally {
            threadPool.shutdown();
        }
    }

    private Listener createRefreshListener(CountDownLatch latch, AtomicReference<Exception> exceptionRef) {
        return new Listener() {
            @Override
            public void onReload() {
                logger.info("refresh called");
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                logger.error("exception " + e);
                exceptionRef.set(e);
                latch.countDown();
            }
        };
    }

    private void atomicMoveIfPossible(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
