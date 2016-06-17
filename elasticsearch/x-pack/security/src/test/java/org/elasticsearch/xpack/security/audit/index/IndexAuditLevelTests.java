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

package org.elasticsearch.xpack.security.audit.index;

import org.elasticsearch.test.ESTestCase;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class IndexAuditLevelTests extends ESTestCase {
    public void testAllIndexAuditLevel() {
        EnumSet<IndexAuditLevel> enumSet = IndexAuditLevel.parse(Collections.singletonList("_all"));
        IndexAuditLevel[] levels = IndexAuditLevel.values();
        assertThat(enumSet.size(), is(levels.length));
        for (IndexAuditLevel level : levels) {
            assertThat(enumSet.contains(level), is(true));
        }
    }

    public void testExcludeHasPreference() {
        EnumSet<IndexAuditLevel> enumSet = IndexAuditLevel.parse(Collections.singletonList("_all"), Collections.singletonList("_all"));
        assertThat(enumSet.size(), is(0));
    }

    public void testExcludeHasPreferenceSingle() {
        String excluded = randomFrom(IndexAuditLevel.values()).toString().toLowerCase(Locale.ROOT);
        EnumSet<IndexAuditLevel> enumSet = IndexAuditLevel.parse(Collections.singletonList("_all"), Collections.singletonList(excluded));
        EnumSet<IndexAuditLevel> expected = EnumSet.allOf(IndexAuditLevel.class);
        expected.remove(IndexAuditLevel.valueOf(excluded.toUpperCase(Locale.ROOT)));
        assertThat(enumSet, equalTo(expected));
    }
}
