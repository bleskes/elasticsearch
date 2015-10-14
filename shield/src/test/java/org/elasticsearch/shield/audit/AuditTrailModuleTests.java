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

package org.elasticsearch.shield.audit;

import org.elasticsearch.Version;
import org.elasticsearch.common.inject.Guice;
import org.elasticsearch.common.inject.Injector;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsModule;
import org.elasticsearch.indices.breaker.CircuitBreakerModule;
import org.elasticsearch.shield.audit.logfile.LoggingAuditTrail;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.threadpool.ThreadPoolModule;
import org.elasticsearch.transport.TransportModule;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 *
 */
public class AuditTrailModuleTests extends ESTestCase {
    public void testEnabled() throws Exception {
        Settings settings = Settings.builder()
                .put("client.type", "node")
                .put("shield.audit.enabled", false)
                .build();
        Injector injector = Guice.createInjector(new SettingsModule(settings), new AuditTrailModule(settings));
        AuditTrail auditTrail = injector.getInstance(AuditTrail.class);
        assertThat(auditTrail, is(AuditTrail.NOOP));
    }

    public void testDisabledByDefault() throws Exception {
        Settings settings = Settings.builder()
                .put("client.type", "node").build();
        Injector injector = Guice.createInjector(new SettingsModule(settings), new AuditTrailModule(settings));
        AuditTrail auditTrail = injector.getInstance(AuditTrail.class);
        assertThat(auditTrail, is(AuditTrail.NOOP));
    }

    public void testLogfile() throws Exception {
        Settings settings = Settings.builder()
                .put("shield.audit.enabled", true)
                .put("client.type", "node")
                .build();
        ThreadPool pool = new ThreadPool("testLogFile");
        try {
            Injector injector = Guice.createInjector(new SettingsModule(settings), new AuditTrailModule(settings), new TransportModule(settings), new CircuitBreakerModule(settings), new ThreadPoolModule(pool), new Version.Module(Version.CURRENT));
            AuditTrail auditTrail = injector.getInstance(AuditTrail.class);
            assertThat(auditTrail, instanceOf(AuditTrailService.class));
            AuditTrailService service = (AuditTrailService) auditTrail;
            assertThat(service.auditTrails, notNullValue());
            assertThat(service.auditTrails.length, is(1));
            assertThat(service.auditTrails[0], instanceOf(LoggingAuditTrail.class));
        } finally {
            pool.shutdown();
        }
    }

    public void testUnknownOutput() throws Exception {
        Settings settings = Settings.builder()
                .put("shield.audit.enabled", true)
                .put("shield.audit.outputs" , "foo")
                .put("client.type", "node")
                .build();
        try {
            Guice.createInjector(new SettingsModule(settings), new AuditTrailModule(settings));
            fail("Expect initialization to fail when an unknown audit trail output is configured");
        } catch (Throwable t) {
            // expected
        }
    }
}
