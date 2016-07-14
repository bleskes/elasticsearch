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

package org.elasticsearch.xpack.security;

import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.license.core.License;
import org.elasticsearch.license.plugin.core.AbstractLicenseeComponent;

/**
 *
 */
public class SecurityLicensee extends AbstractLicenseeComponent {

    private final SecurityLicenseState securityLicenseState;

    public SecurityLicensee(Settings settings, SecurityLicenseState securityLicenseState) {
        super(settings, Security.NAME);
        this.securityLicenseState = securityLicenseState;
    }

    @Override
    public void onChange(Status status) {
        super.onChange(status);
        securityLicenseState.updateStatus(status);
    }

    @Override
    public String[] expirationMessages() {
        return new String[]{
                "Cluster health, cluster stats and indices stats operations are blocked",
                "All data operations (read and write) continue to work"
        };
    }

    @Override
    public String[] acknowledgmentMessages(License.OperationMode currentMode, License.OperationMode newMode) {
        switch (newMode) {
            case BASIC:
                switch (currentMode) {
                    case TRIAL:
                    case STANDARD:
                    case GOLD:
                    case PLATINUM:
                        return new String[] {
                            "The following X-Pack security functionality will be disabled: authentication, authorization, " +
                            "ip filtering, and auditing. Please restart your node after applying the license.",
                            "Field and document level access control will be disabled.",
                            "Custom realms will be ignored."
                        };
                }
                break;
            case GOLD:
                switch (currentMode) {
                    case BASIC:
                    case STANDARD:
                    // ^^ though technically it was already disabled, it's not bad to remind them
                    case TRIAL:
                    case PLATINUM:
                        return new String[] {
                            "Field and document level access control will be disabled.",
                            "Custom realms will be ignored."
                        };
                }
                break;
            case STANDARD:
                switch (currentMode) {
                    case BASIC:
                    // ^^ though technically it was already disabled, it's not bad to remind them
                    case GOLD:
                    case PLATINUM:
                    case TRIAL:
                        return new String[] {
                                "Authentication will be limited to the native realms.",
                                "IP filtering and auditing will be disabled.",
                                "Field and document level access control will be disabled.",
                                "Custom realms will be ignored."
                        };
                }
        }
        return Strings.EMPTY_ARRAY;
    }
}
