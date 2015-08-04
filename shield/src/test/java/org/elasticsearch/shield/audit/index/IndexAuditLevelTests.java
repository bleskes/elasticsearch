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

package org.elasticsearch.shield.audit.index;

import org.elasticsearch.test.ESTestCase;
import org.junit.Test;

import java.util.EnumSet;
import java.util.Locale;

import static org.hamcrest.Matchers.*;

public class IndexAuditLevelTests extends ESTestCase {

    @Test
    public void testAllIndexAuditLevel() {
        EnumSet<IndexAuditLevel> enumSet = IndexAuditLevel.parse(new String[] { "_all" });
        IndexAuditLevel[] levels = IndexAuditLevel.values();
        assertThat(enumSet.size(), is(levels.length));
        for (IndexAuditLevel level : levels) {
            assertThat(enumSet.contains(level), is(true));
        }
    }

    @Test
    public void testExcludeHasPreference() {
        EnumSet<IndexAuditLevel> enumSet = IndexAuditLevel.parse(new String[] { "_all" }, new String[] { "_all" });
        assertThat(enumSet.size(), is(0));
    }

    @Test
    public void testExcludeHasPreferenceSingle() {
        String excluded = randomFrom(IndexAuditLevel.values()).toString().toLowerCase(Locale.ROOT);
        EnumSet<IndexAuditLevel> enumSet = IndexAuditLevel.parse(new String[] { "_all" }, new String[] { excluded });
        EnumSet<IndexAuditLevel> expected = EnumSet.allOf(IndexAuditLevel.class);
        expected.remove(IndexAuditLevel.valueOf(excluded.toUpperCase(Locale.ROOT)));
        assertThat(enumSet, equalTo(expected));
    }

}
