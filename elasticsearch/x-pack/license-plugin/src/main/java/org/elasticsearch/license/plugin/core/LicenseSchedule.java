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

import org.elasticsearch.license.core.License;
import org.elasticsearch.xpack.scheduler.SchedulerEngine;

import static org.elasticsearch.license.plugin.core.LicensesService.GRACE_PERIOD_DURATION;
import static org.elasticsearch.license.plugin.core.LicensesService.getLicenseState;

public class LicenseSchedule implements SchedulerEngine.Schedule {

    private final License license;

    LicenseSchedule(License license) {
        this.license = license;
    }

    @Override
    public long nextScheduledTimeAfter(long startTime, long time) {
        long nextScheduledTime = -1;
        switch (getLicenseState(license, time)) {
            case ENABLED:
                nextScheduledTime = license.expiryDate();
                break;
            case GRACE_PERIOD:
                nextScheduledTime = license.expiryDate() + GRACE_PERIOD_DURATION.getMillis();
                break;
            case DISABLED:
                if (license.issueDate() > time) {
                    nextScheduledTime = license.issueDate();
                }
                break;
        }
        return nextScheduledTime;
    }
}