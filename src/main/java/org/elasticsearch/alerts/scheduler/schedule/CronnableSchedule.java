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

package org.elasticsearch.alerts.scheduler.schedule;

import java.util.Arrays;
import java.util.Objects;

/**
 *
 */
public abstract class CronnableSchedule implements Schedule {

    protected final String[] crons;

    public CronnableSchedule(String... crons) {
        this.crons = crons;
        Arrays.sort(crons);
    }

    public String[] crons() {
        return crons;
    }

    @Override
    public int hashCode() {
        return Objects.hash((Object[]) crons);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final CronnableSchedule other = (CronnableSchedule) obj;
        return Objects.deepEquals(this.crons, other.crons);
    }
}
