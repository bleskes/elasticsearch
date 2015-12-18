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
package org.elasticsearch.license.plugin.core;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.Singleton;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.license.core.License;

import java.util.Collections;
import java.util.List;

@Singleton
public class FoundLicensesService extends AbstractLifecycleComponent<FoundLicensesService> implements LicenseeRegistry, LicensesManagerService {

    @Inject
    public FoundLicensesService(Settings settings) {
        super(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void register(Licensee licensee) {
        licensee.onChange(new Licensee.Status(License.OperationMode.PLATINUM, LicenseState.ENABLED));
    }

    @Override
    protected void doStart() throws ElasticsearchException {
    }

    @Override
    protected void doStop() throws ElasticsearchException {
    }

    @Override
    protected void doClose() throws ElasticsearchException {
    }

    @Override
    public List<String> licenseesWithState(LicenseState state) {
        return Collections.<String>emptyList();
    }

    @Override
    public License getLicense() {
        return null;
    }
}
