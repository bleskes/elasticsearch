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

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.license.core.License;
import org.elasticsearch.license.plugin.core.AbstractLicenseeComponent;
import org.elasticsearch.license.plugin.core.Licensee;
import org.elasticsearch.license.plugin.core.LicenseeRegistry;

/**
 *
 */
public class SecurityLicensee extends AbstractLicenseeComponent<SecurityLicensee> implements Licensee {

    private final boolean isTribeNode;
    private final SecurityLicenseState shieldLicenseState;

    @Inject
    public SecurityLicensee(Settings settings, LicenseeRegistry clientService, SecurityLicenseState shieldLicenseState) {
        super(settings, Security.NAME, clientService);
        this.shieldLicenseState = shieldLicenseState;
        this.isTribeNode = settings.getGroups("tribe", true).isEmpty() == false;
    }

    @Override
    public void onChange(Status status) {
        super.onChange(status);
        shieldLicenseState.updateStatus(status);
    }

    @Override
    public String[] expirationMessages() {
        return new String[]{
                "Cluster health, cluster stats and indices stats operations are blocked",
                "All data operations (read and write) continue to work"
        };
    }

    @Override
    public String[] acknowledgmentMessages(License currentLicense, License newLicense) {
        switch (newLicense.operationMode()) {
            case BASIC:
                if (currentLicense != null) {
                    switch (currentLicense.operationMode()) {
                        case TRIAL:
                        case STANDARD:
                        case GOLD:
                        case PLATINUM:
                            return new String[] {
                                "The following Shield functionality will be disabled: authentication, authorization, ip filtering, " +
                                "auditing, SSL will be disabled on node restart. Please restart your node after applying the license.",
                                "Field and document level access control will be disabled",
                                "Custom realms will be ignored"
                            };
                    }
                }
                break;
            case GOLD:
                if (currentLicense != null) {
                    switch (currentLicense.operationMode()) {
                        case BASIC:
                        case STANDARD:
                        // ^^ though technically it was already disabled, it's not bad to remind them
                        case TRIAL:
                        case PLATINUM:
                            return new String[] {
                                "Field and document level access control will be disabled",
                                "Custom realms will be ignored"
                            };
                    }
                }
                break;
        }
        return Strings.EMPTY_ARRAY;
    }

    @Override
    protected void doStart() throws ElasticsearchException {
        // we rely on the initial licensee state to be enabled with trial operation mode
        // to ensure no operation is blocked due to not registering the licensee on a
        // tribe node
        if (isTribeNode == false) {
            super.doStart();
        }
    }
}
