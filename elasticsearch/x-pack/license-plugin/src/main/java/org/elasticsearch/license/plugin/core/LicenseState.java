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

import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.license.core.License;

import static org.elasticsearch.license.plugin.core.LicensesService.days;

/**
 * States of a registered licensee
 * based on the current license
 */
public enum LicenseState {

    /**
     * Active license is valid.
     *
     * When license expires
     * changes to {@link #GRACE_PERIOD}
     */
    ENABLED,

    /**
     * Active license expired
     * but grace period has not.
     *
     * When grace period expires
     * changes to {@link #DISABLED}.
     * When valid license is installed
     * changes back to {@link #ENABLED}
     */
    GRACE_PERIOD,

    /**
     * Grace period for active license
     * expired.
     *
     * When a valid license is installed
     * changes to {@link #ENABLED}, otherwise
     * remains unchanged
     */
    DISABLED;

    /**
     * Duration of grace period after a license has expired
     */
    public static final TimeValue GRACE_PERIOD_DURATION = days(7);

    public static LicenseState resolve(final License license, long time) {
        if (license == null) {
            return DISABLED;
        }
        if (license.issueDate() > time) {
            return DISABLED;
        }
        if (license.expiryDate() > time) {
            return ENABLED;
        }
        if ((license.expiryDate() + GRACE_PERIOD_DURATION.getMillis()) > time) {
            return GRACE_PERIOD;
        }
        return DISABLED;
    }
}
