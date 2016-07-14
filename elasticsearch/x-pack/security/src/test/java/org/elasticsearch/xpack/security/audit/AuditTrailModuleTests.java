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

package org.elasticsearch.xpack.security.audit;

import org.elasticsearch.common.inject.ModuleTestCase;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.xpack.security.audit.index.IndexAuditTrail;
import org.elasticsearch.xpack.security.audit.logfile.LoggingAuditTrail;

public class AuditTrailModuleTests extends ModuleTestCase {

    public void testEnabled() throws Exception {
        Settings settings = Settings.builder().put(AuditTrailModule.ENABLED_SETTING.getKey(), true).build();
        AuditTrailModule module = new AuditTrailModule(settings);
        assertBinding(module, AuditTrail.class, AuditTrailService.class);
        assertSetMultiBinding(module, AuditTrail.class, LoggingAuditTrail.class);
    }

    public void testDisabledByDefault() throws Exception {
        AuditTrailModule module = new AuditTrailModule(Settings.EMPTY);
        assertInstanceBinding(module, AuditTrail.class, x -> x == AuditTrail.NOOP);
    }

    public void testIndexAuditTrail() throws Exception {
        Settings settings = Settings.builder()
            .put(AuditTrailModule.ENABLED_SETTING.getKey(), true)
            .put(AuditTrailModule.OUTPUTS_SETTING.getKey(), "index").build();
        AuditTrailModule module = new AuditTrailModule(settings);
        assertSetMultiBinding(module, AuditTrail.class, IndexAuditTrail.class);
    }

    public void testIndexAndLoggingAuditTrail() throws Exception {
        Settings settings = Settings.builder()
            .put(AuditTrailModule.ENABLED_SETTING.getKey(), true)
            .put(AuditTrailModule.OUTPUTS_SETTING.getKey(), "index,logfile").build();
        AuditTrailModule module = new AuditTrailModule(settings);
        assertSetMultiBinding(module, AuditTrail.class, IndexAuditTrail.class, LoggingAuditTrail.class);
    }

    public void testUnknownOutput() throws Exception {
        Settings settings = Settings.builder()
                .put(AuditTrailModule.ENABLED_SETTING.getKey(), true)
                .put(AuditTrailModule.OUTPUTS_SETTING.getKey(), "foo").build();
        AuditTrailModule module = new AuditTrailModule(settings);
        assertBindingFailure(module, "unknown audit trail output [foo]");
    }
}
