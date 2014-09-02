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

import org.elasticsearch.common.inject.Guice;
import org.elasticsearch.common.inject.Injector;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsModule;
import org.elasticsearch.shield.audit.logfile.LoggingAuditTrail;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.junit.Test;

import static org.hamcrest.Matchers.*;

/**
 *
 */
public class AuditTrailModuleTests extends ElasticsearchTestCase {

    @Test
    public void testEnabled() throws Exception {
        Settings settings = ImmutableSettings.builder()
                .put("shield.audit.enabled", false)
                .build();
        Injector injector = Guice.createInjector(new SettingsModule(settings), new AuditTrailModule(settings));
        AuditTrail auditTrail = injector.getInstance(AuditTrail.class);
        assertThat(auditTrail, is(AuditTrail.NOOP));
    }

    @Test
    public void testDisabledByDefault() throws Exception {
        Settings settings = ImmutableSettings.EMPTY;
        Injector injector = Guice.createInjector(new SettingsModule(settings), new AuditTrailModule(settings));
        AuditTrail auditTrail = injector.getInstance(AuditTrail.class);
        assertThat(auditTrail, is(AuditTrail.NOOP));
    }

    @Test
    public void testLogfile() throws Exception {
        Settings settings = ImmutableSettings.builder()
                .put("shield.audit.enabled", true)
                .build();
        Injector injector = Guice.createInjector(new SettingsModule(settings), new AuditTrailModule(settings));
        AuditTrail auditTrail = injector.getInstance(AuditTrail.class);
        assertThat(auditTrail, instanceOf(AuditTrailService.class));
        AuditTrailService service = (AuditTrailService) auditTrail;
        assertThat(service.auditTrails, notNullValue());
        assertThat(service.auditTrails.length, is(1));
        assertThat(service.auditTrails[0], instanceOf(LoggingAuditTrail.class));
    }

    @Test
    public void testUnknownOutput() throws Exception {
        Settings settings = ImmutableSettings.builder()
                .put("shield.audit.enabled", true)
                .put("shield.audit.outputs" , "foo")
                .build();
        try {
            Guice.createInjector(new SettingsModule(settings), new AuditTrailModule(settings));
            fail("Expect initialization to fail when an unknown audit trail output is configured");
        } catch (Throwable t) {
            // expected
        }
    }

}
