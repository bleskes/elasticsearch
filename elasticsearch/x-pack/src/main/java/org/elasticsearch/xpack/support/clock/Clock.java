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

package org.elasticsearch.xpack.support.clock;

import org.elasticsearch.common.unit.TimeValue;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 *
 */
public interface Clock {

    long millis();

    long nanos();

    DateTime nowUTC();

    DateTime now(DateTimeZone timeZone);

    TimeValue timeElapsedSince(DateTime time);

}
