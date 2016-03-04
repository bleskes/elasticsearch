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

package org.elasticsearch.shield.authz.accesscontrol;

import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.util.set.Sets;
import org.elasticsearch.test.ESTestCase;

import java.util.Collections;
import org.elasticsearch.shield.authz.accesscontrol.IndicesAccessControl.IndexAccessControl;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * Unit tests for {@link IndicesAccessControl}
 */
public class IndicesAccessControlTests extends ESTestCase {

    public void testEmptyIndicesAccessControl() {
        IndicesAccessControl indicesAccessControl = new IndicesAccessControl(true, Collections.emptyMap());
        assertThat(indicesAccessControl.isGranted(), is(true));
        assertThat(indicesAccessControl.getIndexPermissions(randomAsciiOfLengthBetween(3,20)), nullValue());
    }

    public void testMergeFields() {
        IndexAccessControl indexAccessControl = new IndexAccessControl(true, Sets.newHashSet("a", "c"), null);
        IndexAccessControl other = new IndexAccessControl(true, Sets.newHashSet("b"), null);

        IndexAccessControl merge1 = indexAccessControl.merge(other);
        assertThat(merge1.getFields(), containsInAnyOrder("a", "b", "c"));
        assertThat(merge1.isGranted(), is(true));
        assertThat(merge1.getQueries(), nullValue());

        IndexAccessControl merge2 = other.merge(indexAccessControl);
        assertThat(merge2.getFields(), containsInAnyOrder("a", "b", "c"));
        assertThat(merge2.isGranted(), is(true));
        assertThat(merge2.getQueries(), nullValue());
    }

    public void testMergeEmptyAndNullFields() {
        IndexAccessControl indexAccessControl = new IndexAccessControl(true, Collections.emptySet(), null);
        IndexAccessControl other = new IndexAccessControl(true, null, null);

        IndexAccessControl merge1 = indexAccessControl.merge(other);
        assertThat(merge1.getFields(), nullValue());
        assertThat(merge1.isGranted(), is(true));
        assertThat(merge1.getQueries(), nullValue());

        IndexAccessControl merge2 = other.merge(indexAccessControl);
        assertThat(merge2.getFields(), nullValue());
        assertThat(merge2.isGranted(), is(true));
        assertThat(merge2.getQueries(), nullValue());
    }

    public void testMergeNullFields() {
        IndexAccessControl indexAccessControl = new IndexAccessControl(true, Sets.newHashSet("a", "b"), null);
        IndexAccessControl other = new IndexAccessControl(true, null, null);

        IndexAccessControl merge1 = indexAccessControl.merge(other);
        assertThat(merge1.getFields(), nullValue());
        assertThat(merge1.isGranted(), is(true));
        assertThat(merge1.getQueries(), nullValue());

        IndexAccessControl merge2 = other.merge(indexAccessControl);
        assertThat(merge2.getFields(), nullValue());
        assertThat(merge2.isGranted(), is(true));
        assertThat(merge2.getQueries(), nullValue());
    }

    public void testMergeQueries() {
        BytesReference query1 = new BytesArray(new byte[] { 0x1 });
        BytesReference query2 = new BytesArray(new byte[] { 0x2 });
        IndexAccessControl indexAccessControl = new IndexAccessControl(true, null, Collections.singleton(query1));
        IndexAccessControl other = new IndexAccessControl(true, null, Collections.singleton(query2));

        IndexAccessControl merge1 = indexAccessControl.merge(other);
        assertThat(merge1.getFields(), nullValue());
        assertThat(merge1.isGranted(), is(true));
        assertThat(merge1.getQueries(), containsInAnyOrder(query1, query2));

        IndexAccessControl merge2 = other.merge(indexAccessControl);
        assertThat(merge2.getFields(), nullValue());
        assertThat(merge2.isGranted(), is(true));
        assertThat(merge1.getQueries(), containsInAnyOrder(query1, query2));
    }

    public void testMergeNullQuery() {
        BytesReference query1 = new BytesArray(new byte[] { 0x1 });
        IndexAccessControl indexAccessControl = new IndexAccessControl(true, null, Collections.singleton(query1));
        IndexAccessControl other = new IndexAccessControl(true, null, null);

        IndexAccessControl merge1 = indexAccessControl.merge(other);
        assertThat(merge1.getFields(), nullValue());
        assertThat(merge1.isGranted(), is(true));
        assertThat(merge1.getQueries(), nullValue());

        IndexAccessControl merge2 = other.merge(indexAccessControl);
        assertThat(merge2.getFields(), nullValue());
        assertThat(merge2.isGranted(), is(true));
        assertThat(merge1.getQueries(), nullValue());
    }
}
