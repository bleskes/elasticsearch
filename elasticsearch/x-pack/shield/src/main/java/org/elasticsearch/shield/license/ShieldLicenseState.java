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

package org.elasticsearch.shield.license;

import org.elasticsearch.license.plugin.core.LicenseState;
import org.elasticsearch.license.plugin.core.Licensee.Status;


/**
 * This class serves to decouple shield code that needs to check the license state from the {@link ShieldLicensee} as the
 * tight coupling causes issues with guice injection and circular dependencies
 */
public class ShieldLicenseState {

    // we initialize the licensee status to enabled with trial operation mode to ensure no
    // legitimate requests are blocked before initial license plugin notification
    protected volatile Status status = Status.ENABLED;

    /**
     * @return true if the license allows for security features to be enabled (authc, authz, ip filter, audit, etc)
     */
    public boolean securityEnabled() {
        return status.getMode().isPaid();
    }

    /**
     * Indicates whether the stats and health API calls should be allowed. If a license is expired and past the grace
     * period then we deny these calls.
     *
     * @return true if the license allows for the stats and health apis to be used.
     */
    public boolean statsAndHealthEnabled() {
        return status.getLicenseState() != LicenseState.DISABLED;
    }

    /**
     * @return true if the license enables DLS and FLS
     */
    public boolean documentAndFieldLevelSecurityEnabled() {
        return status.getMode().allFeaturesEnabled();
    }

    /**
     * @return true if the license enables the use of custom authentication realms
     */
    public boolean customRealmsEnabled() {
        return status.getMode().allFeaturesEnabled();
    }

    void updateStatus(Status status) {
        this.status = status;
    }
}
