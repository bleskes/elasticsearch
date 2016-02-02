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

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.inject.multibindings.Multibinder;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.set.Sets;
import org.elasticsearch.shield.audit.index.IndexAuditTrail;
import org.elasticsearch.shield.audit.logfile.LoggingAuditTrail;
import org.elasticsearch.shield.support.AbstractShieldModule;

import java.util.Set;

/**
 *
 */
public class AuditTrailModule extends AbstractShieldModule.Node {

    private final boolean enabled;

    public AuditTrailModule(Settings settings) {
        super(settings);
        enabled = auditingEnabled(settings);
    }

    @Override
    protected void configureNode() {
        if (!enabled) {
            bind(AuditTrail.class).toInstance(AuditTrail.NOOP);
            return;
        }
        String[] outputs = settings.getAsArray("shield.audit.outputs", new String[] { LoggingAuditTrail.NAME });
        if (outputs.length == 0) {
            bind(AuditTrail.class).toInstance(AuditTrail.NOOP);
            return;
        }
        bind(AuditTrail.class).to(AuditTrailService.class).asEagerSingleton();
        Multibinder<AuditTrail> binder = Multibinder.newSetBinder(binder(), AuditTrail.class);

        Set<String> uniqueOutputs = Sets.newHashSet(outputs);
        for (String output : uniqueOutputs) {
            switch (output) {
                case LoggingAuditTrail.NAME:
                    binder.addBinding().to(LoggingAuditTrail.class);
                    bind(LoggingAuditTrail.class).asEagerSingleton();
                    break;
                case IndexAuditTrail.NAME:
                    binder.addBinding().to(IndexAuditTrail.class);
                    bind(IndexAuditTrail.class).asEagerSingleton();
                    break;
                default:
                    throw new ElasticsearchException("unknown audit trail output [" + output + "]");
            }
        }
    }

    public static boolean auditingEnabled(Settings settings) {
        return settings.getAsBoolean("shield.audit.enabled", false);
    }

    public static boolean indexAuditLoggingEnabled(Settings settings) {
        if (auditingEnabled(settings)) {
            String[] outputs = settings.getAsArray("shield.audit.outputs");
            for (String output : outputs) {
                if (output.equals(IndexAuditTrail.NAME)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean fileAuditLoggingEnabled(Settings settings) {
        if (auditingEnabled(settings)) {
            String[] outputs = settings.getAsArray("shield.audit.outputs", new String[] { LoggingAuditTrail.NAME });
            for (String output : outputs) {
                if (output.equals(LoggingAuditTrail.NAME)) {
                    return true;
                }
            }
        }
        return false;
    }
}
