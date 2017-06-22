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

package org.elasticsearch.xpack.security.transport.netty3;

import org.elasticsearch.common.network.NetworkService;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.BigArrays;
import org.elasticsearch.env.Environment;
import org.elasticsearch.http.HttpTransportSettings;
import org.elasticsearch.http.NullDispatcher;
import org.elasticsearch.http.netty3.Netty3HttpMockUtil;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.security.transport.filter.IPFilter;
import org.elasticsearch.xpack.ssl.SSLClientAuth;
import org.elasticsearch.xpack.ssl.SSLService;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.handler.ssl.SslHandler;
import org.junit.After;
import org.junit.Before;

import javax.net.ssl.SSLEngine;

import java.nio.file.Path;
import java.util.Locale;

import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;

public class SecurityNetty3HttpServerTransportTests extends ESTestCase {

    private SSLService sslService;
    private Environment env;

    @Before
    public void createSSLService() throws Exception {
        Path testnodeStore = getDataPath("/org/elasticsearch/xpack/security/transport/ssl/certs/simple/testnode.jks");
        Settings settings = Settings.builder()
                .put("xpack.ssl.keystore.path", testnodeStore)
                .put("xpack.ssl.keystore.password", "testnode")
                .put("path.home", createTempDir())
                .build();
        env = new Environment(settings);
        sslService = new SSLService(settings, env);
    }

    public void testDefaultClientAuth() throws Exception {
        Settings settings = Settings.builder()
                .put(env.settings())
                .put("xpack.security.http.ssl.enabled", true).build();
        sslService = new SSLService(settings, env);
        SecurityNetty3HttpServerTransport transport = new SecurityNetty3HttpServerTransport(settings, mock(NetworkService.class),
                mock(BigArrays.class), mock(IPFilter.class), sslService, mock(ThreadPool.class), xContentRegistry(),
                new NullDispatcher());
        Netty3HttpMockUtil.setOpenChannelsHandlerToMock(transport);
        ChannelPipelineFactory factory = transport.configureServerChannelPipelineFactory();
        assertThat(factory.getPipeline().get(SslHandler.class).getEngine().getNeedClientAuth(), is(false));
        assertThat(factory.getPipeline().get(SslHandler.class).getEngine().getWantClientAuth(), is(false));
        assertWarnings("http type [security3] is deprecated");
    }

    public void testOptionalClientAuth() throws Exception {
        String value = randomFrom(SSLClientAuth.OPTIONAL.name(), SSLClientAuth.OPTIONAL.name().toLowerCase(Locale.ROOT));
        Settings settings = Settings.builder()
                .put(env.settings())
                .put("xpack.security.http.ssl.enabled", true)
                .put("xpack.security.http.ssl.client_authentication", value).build();
        sslService = new SSLService(settings, env);
        SecurityNetty3HttpServerTransport transport = new SecurityNetty3HttpServerTransport(settings, mock(NetworkService.class),
                mock(BigArrays.class), mock(IPFilter.class), sslService, mock(ThreadPool.class), xContentRegistry(),
                new NullDispatcher());
        Netty3HttpMockUtil.setOpenChannelsHandlerToMock(transport);
        ChannelPipelineFactory factory = transport.configureServerChannelPipelineFactory();
        assertThat(factory.getPipeline().get(SslHandler.class).getEngine().getNeedClientAuth(), is(false));
        assertThat(factory.getPipeline().get(SslHandler.class).getEngine().getWantClientAuth(), is(true));
        assertWarnings("http type [security3] is deprecated");
    }

    public void testRequiredClientAuth() throws Exception {
        Settings settings = Settings.builder()
                .put(env.settings())
                .put("xpack.security.http.ssl.enabled", true)
                .put("xpack.security.http.ssl.client_authentication", SSLClientAuth.REQUIRED).build();
        sslService = new SSLService(settings, env);
        SecurityNetty3HttpServerTransport transport = new SecurityNetty3HttpServerTransport(settings, mock(NetworkService.class),
                mock(BigArrays.class), mock(IPFilter.class), sslService, mock(ThreadPool.class), xContentRegistry(),
                new NullDispatcher());
        Netty3HttpMockUtil.setOpenChannelsHandlerToMock(transport);
        ChannelPipelineFactory factory = transport.configureServerChannelPipelineFactory();
        assertThat(factory.getPipeline().get(SslHandler.class).getEngine().getNeedClientAuth(), is(true));
        assertThat(factory.getPipeline().get(SslHandler.class).getEngine().getWantClientAuth(), is(false));
        assertWarnings("http type [security3] is deprecated");
    }

    public void testNoClientAuth() throws Exception {
        Settings settings = Settings.builder()
                .put(env.settings())
                .put("xpack.security.http.ssl.enabled", true)
                .put("xpack.security.http.ssl.client_authentication", SSLClientAuth.NONE).build();
        sslService = new SSLService(settings, env);
        SecurityNetty3HttpServerTransport transport = new SecurityNetty3HttpServerTransport(settings, mock(NetworkService.class),
                mock(BigArrays.class), mock(IPFilter.class), sslService, mock(ThreadPool.class), xContentRegistry(),
                new NullDispatcher());
        Netty3HttpMockUtil.setOpenChannelsHandlerToMock(transport);
        ChannelPipelineFactory factory = transport.configureServerChannelPipelineFactory();
        assertThat(factory.getPipeline().get(SslHandler.class).getEngine().getNeedClientAuth(), is(false));
        assertThat(factory.getPipeline().get(SslHandler.class).getEngine().getWantClientAuth(), is(false));
        assertWarnings("http type [security3] is deprecated");
    }

    public void testCustomSSLConfiguration() throws Exception {
        Settings settings = Settings.builder()
                .put(env.settings())
                .put("xpack.security.http.ssl.enabled", true).build();
        sslService = new SSLService(settings, env);
        SecurityNetty3HttpServerTransport transport = new SecurityNetty3HttpServerTransport(settings, mock(NetworkService.class),
                mock(BigArrays.class), mock(IPFilter.class), sslService, mock(ThreadPool.class), xContentRegistry(),
                new NullDispatcher());
        Netty3HttpMockUtil.setOpenChannelsHandlerToMock(transport);
        ChannelPipelineFactory factory = transport.configureServerChannelPipelineFactory();
        SSLEngine defaultEngine = factory.getPipeline().get(SslHandler.class).getEngine();
        assertWarnings("http type [security3] is deprecated");

        settings = Settings.builder()
                .put(env.settings())
                .put("xpack.security.http.ssl.enabled", true)
                .put("xpack.security.http.ssl.supported_protocols", "TLSv1.2")
                .build();
        sslService = new SSLService(settings, new Environment(settings));
        transport = new SecurityNetty3HttpServerTransport(settings, mock(NetworkService.class),
                mock(BigArrays.class), mock(IPFilter.class), sslService, mock(ThreadPool.class), xContentRegistry(),
                new NullDispatcher());
        Netty3HttpMockUtil.setOpenChannelsHandlerToMock(transport);
        factory = transport.configureServerChannelPipelineFactory();
        SSLEngine customEngine = factory.getPipeline().get(SslHandler.class).getEngine();
        assertThat(customEngine.getEnabledProtocols(), arrayContaining("TLSv1.2"));
        assertThat(customEngine.getEnabledProtocols(), not(equalTo(defaultEngine.getEnabledProtocols())));
        assertWarnings("http type [security3] is deprecated");
    }

    public void testDisablesCompressionByDefaultForSsl() throws Exception {
        Settings settings = Settings.builder()
                .put("xpack.security.http.ssl.enabled", true).build();

        Settings.Builder pluginSettingsBuilder = Settings.builder();
        SecurityNetty3HttpServerTransport.overrideSettings(pluginSettingsBuilder, settings);
        assertThat(HttpTransportSettings.SETTING_HTTP_COMPRESSION.get(pluginSettingsBuilder.build()), is(false));
    }

    public void testLeavesCompressionOnIfNotSsl() throws Exception {
        Settings settings = Settings.builder()
                .put("xpack.security.http.ssl.enabled", false).build();
        Settings.Builder pluginSettingsBuilder = Settings.builder();
        SecurityNetty3HttpServerTransport.overrideSettings(pluginSettingsBuilder, settings);
        assertThat(pluginSettingsBuilder.build().isEmpty(), is(true));
    }

    public void testDoesNotChangeExplicitlySetCompression() throws Exception {
        Settings settings = Settings.builder()
                .put("xpack.security.http.ssl.enabled", true)
                .put(HttpTransportSettings.SETTING_HTTP_COMPRESSION.getKey(), true)
                .build();

        Settings.Builder pluginSettingsBuilder = Settings.builder();
        SecurityNetty3HttpServerTransport.overrideSettings(pluginSettingsBuilder, settings);
        assertThat(pluginSettingsBuilder.build().isEmpty(), is(true));
    }

    public void testThatExceptionIsThrownWhenConfiguredWithoutSslKey() throws Exception {
        Settings settings = Settings.builder()
                .put("xpack.ssl.truststore.path",
                        getDataPath("/org/elasticsearch/xpack/security/transport/ssl/certs/simple/testnode.jks"))
                .put("xpack.ssl.truststore.password", "testnode")
                .put("xpack.security.http.ssl.enabled", true)
                .put("path.home", createTempDir())
                .build();
        env = new Environment(settings);
        sslService = new SSLService(settings, env);
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class,
                () -> new SecurityNetty3HttpServerTransport(settings, mock(NetworkService.class),
                mock(BigArrays.class), mock(IPFilter.class), sslService, mock(ThreadPool.class), xContentRegistry(),
                new NullDispatcher()));
        assertThat(e.getMessage(), containsString("key must be provided"));
        assertWarnings("http type [security3] is deprecated");
    }

    public void testNoExceptionWhenConfiguredWithoutSslKeySSLDisabled() throws Exception {
        Settings settings = Settings.builder()
                .put("xpack.ssl.truststore.path",
                        getDataPath("/org/elasticsearch/xpack/security/transport/ssl/certs/simple/testnode.jks"))
                .put("xpack.ssl.truststore.password", "testnode")
                .put("path.home", createTempDir())
                .build();
        env = new Environment(settings);
        sslService = new SSLService(settings, env);
        SecurityNetty3HttpServerTransport transport = new SecurityNetty3HttpServerTransport(settings, mock(NetworkService.class),
                mock(BigArrays.class), mock(IPFilter.class), sslService, mock(ThreadPool.class), xContentRegistry(),
                new NullDispatcher());
        assertNotNull(transport.configureServerChannelPipelineFactory());
        assertWarnings("http type [security3] is deprecated");
    }
}
