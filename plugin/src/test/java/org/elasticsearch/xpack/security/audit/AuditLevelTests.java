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

package org.elasticsearch.xpack.security.audit;

import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.security.audit.AuditLevel;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class AuditLevelTests extends ESTestCase {
    public void testAllIndexAuditLevel() {
        EnumSet<AuditLevel> enumSet = AuditLevel.parse(Collections.singletonList("_all"));
        AuditLevel[] levels = AuditLevel.values();
        assertThat(enumSet.size(), is(levels.length));
        for (AuditLevel level : levels) {
            assertThat(enumSet.contains(level), is(true));
        }
    }

    public void testExcludeHasPreference() {
        EnumSet<AuditLevel> enumSet = AuditLevel.parse(Collections.singletonList("_all"), Collections.singletonList("_all"));
        assertThat(enumSet.size(), is(0));
    }

    public void testExcludeHasPreferenceSingle() {
        String excluded = randomFrom(AuditLevel.values()).toString().toLowerCase(Locale.ROOT);
        EnumSet<AuditLevel> enumSet = AuditLevel.parse(Collections.singletonList("_all"), Collections.singletonList(excluded));
        EnumSet<AuditLevel> expected = EnumSet.allOf(AuditLevel.class);
        expected.remove(AuditLevel.valueOf(excluded.toUpperCase(Locale.ROOT)));
        assertThat(enumSet, equalTo(expected));
    }
}
