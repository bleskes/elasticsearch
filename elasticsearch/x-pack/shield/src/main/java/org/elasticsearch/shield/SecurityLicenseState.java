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

import org.elasticsearch.license.core.License.OperationMode;
import org.elasticsearch.license.plugin.core.LicenseState;
import org.elasticsearch.license.plugin.core.Licensee.Status;


/**
 * This class serves to decouple shield code that needs to check the license state from the {@link SecurityLicensee} as the
 * tight coupling causes issues with guice injection and circular dependencies
 */
public class SecurityLicenseState {

    // we initialize the licensee status to enabled with trial operation mode to ensure no
    // legitimate requests are blocked before initial license plugin notification
    protected volatile Status status = Status.ENABLED;

    /**
     * @return true if the license allows for security features to be enabled (authc, authz, ip filter, audit, etc)
     */
    public boolean securityEnabled() {
        return status.getMode() != OperationMode.BASIC;
    }

    /**
     * Indicates whether the stats and health API calls should be allowed. If a license is expired and past the grace
     * period then we deny these calls.
     *
     * @return true if the license allows for the stats and health APIs to be used.
     */
    public boolean statsAndHealthEnabled() {
        return status.getLicenseState() != LicenseState.DISABLED;
    }

    /**
     * Determine if Document Level Security (DLS) and Field Level Security (FLS) should be enabled.
     * <p>
     * DLS and FLS are only disabled when the mode is not:
     * <ul>
     * <li>{@link OperationMode#PLATINUM}</li>
     * <li>{@link OperationMode#TRIAL}</li>
     * </ul>
     * Note: This does not consider the <em>state</em> of the license so that Security does not suddenly leak information!
     *
     * @return {@code true} to enable DLS and FLS. Otherwise {@code false}.
     */
    public boolean documentAndFieldLevelSecurityEnabled() {
        Status status = this.status;
        return status.getMode() == OperationMode.TRIAL || status.getMode() == OperationMode.PLATINUM;
    }

    /**
     * Determine if Custom Realms should be enabled.
     * <p>
     * Custom Realms are only disabled when the mode is not:
     * <ul>
     * <li>{@link OperationMode#PLATINUM}</li>
     * <li>{@link OperationMode#TRIAL}</li>
     * </ul>
     * Note: This does not consider the <em>state</em> of the license so that Security does not suddenly block requests!
     *
     * @return {@code true} to enable Custom Realms. Otherwise {@code false}.
     */
    public boolean customRealmsEnabled() {
        Status status = this.status;
        return status.getMode() == OperationMode.TRIAL || status.getMode() == OperationMode.PLATINUM;
    }

    void updateStatus(Status status) {
        this.status = status;
    }
}
