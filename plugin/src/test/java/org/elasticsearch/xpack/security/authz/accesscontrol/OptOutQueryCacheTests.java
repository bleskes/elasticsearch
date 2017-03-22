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


package org.elasticsearch.xpack.security.authz.accesscontrol;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.RandomIndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.Weight;
import org.apache.lucene.store.Directory;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.security.authz.permission.FieldPermissions;
import org.elasticsearch.xpack.security.authz.permission.FieldPermissionsDefinition;
import org.junit.After;
import org.junit.Before;

import java.io.IOException;
import java.util.HashSet;

/** Simple tests for opt out query cache*/
public class OptOutQueryCacheTests extends ESTestCase {
    IndexSearcher searcher;
    Directory dir;
    RandomIndexWriter w;
    DirectoryReader reader;

    @Before
    void initLuceneStuff() throws IOException {
        dir = newDirectory();
        w = new RandomIndexWriter(random(), dir);
        reader = w.getReader();
        searcher = newSearcher(reader);
    }

    @After
    void closeLuceneStuff() throws IOException {
        w.close();
        dir.close();
        reader.close();
    }
    public void testOptOutQueryCacheSafetyCheck() throws IOException {

        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(new TermQuery(new Term("foo", "bar")), BooleanClause.Occur.MUST);
        builder.add(new TermQuery(new Term("no", "baz")), BooleanClause.Occur.MUST_NOT);
        Weight weight = builder.build().createWeight(searcher, false);

        // whenever the allowed fields match the fields in the query and we do not deny access to any fields we allow caching.
        IndicesAccessControl.IndexAccessControl permissions = new IndicesAccessControl.IndexAccessControl(true,
                new FieldPermissions(fieldPermissionDef(new String[]{"foo", "no"}, null)), new HashSet<>());
        assertTrue(OptOutQueryCache.cachingIsSafe(weight, permissions));

        permissions = new IndicesAccessControl.IndexAccessControl(true,
                new FieldPermissions(fieldPermissionDef(new String[]{"foo", "no"}, new String[]{})), new HashSet<>());
        assertTrue(OptOutQueryCache.cachingIsSafe(weight, permissions));

        permissions = new IndicesAccessControl.IndexAccessControl(true,
                new FieldPermissions(fieldPermissionDef(new String[]{"*"}, new String[]{})), new HashSet<>());
        assertTrue(OptOutQueryCache.cachingIsSafe(weight, permissions));

        permissions = new IndicesAccessControl.IndexAccessControl(true,
                new FieldPermissions(fieldPermissionDef(new String[]{"*"}, null)), new HashSet<>());
        assertTrue(OptOutQueryCache.cachingIsSafe(weight, permissions));

        permissions = new IndicesAccessControl.IndexAccessControl(true,
                new FieldPermissions(fieldPermissionDef(new String[]{"*"}, new String[]{"oof"})), new HashSet<>());
        assertTrue(OptOutQueryCache.cachingIsSafe(weight, permissions));

        permissions = new IndicesAccessControl.IndexAccessControl(true,
                new FieldPermissions(fieldPermissionDef(new String[]{"f*", "n*"}, new String[]{})), new HashSet<>());
        assertTrue(OptOutQueryCache.cachingIsSafe(weight, permissions));

        // check we don't cache if a field is not allowed
        permissions = new IndicesAccessControl.IndexAccessControl(true,
                new FieldPermissions(fieldPermissionDef(new String[]{"foo"}, null)), new HashSet<>());
        assertFalse(OptOutQueryCache.cachingIsSafe(weight, permissions));

        permissions = new IndicesAccessControl.IndexAccessControl(true,
                new FieldPermissions(fieldPermissionDef(new String[]{"a*"}, new String[]{"aa"})), new HashSet<>());
        assertFalse(OptOutQueryCache.cachingIsSafe(weight, permissions));

        permissions = new IndicesAccessControl.IndexAccessControl(true,
                new FieldPermissions(fieldPermissionDef(null, new String[]{"no"})), new HashSet<>());
        assertFalse(OptOutQueryCache.cachingIsSafe(weight, permissions));

        permissions = new IndicesAccessControl.IndexAccessControl(true,
                new FieldPermissions(fieldPermissionDef(null, new String[]{"*"})), new HashSet<>());
        assertFalse(OptOutQueryCache.cachingIsSafe(weight, permissions));

        permissions = new IndicesAccessControl.IndexAccessControl(true,
                new FieldPermissions(fieldPermissionDef(new String[]{"foo", "no"}, new String[]{"no"})), new HashSet<>());
        assertFalse(OptOutQueryCache.cachingIsSafe(weight, permissions));

        permissions = new IndicesAccessControl.IndexAccessControl(true,
                new FieldPermissions(fieldPermissionDef(new String[]{}, new String[]{})), new HashSet<>());
        assertFalse(OptOutQueryCache.cachingIsSafe(weight, permissions));

        permissions = new IndicesAccessControl.IndexAccessControl(true,
                new FieldPermissions(fieldPermissionDef(new String[]{}, null)), new HashSet<>());
        assertFalse(OptOutQueryCache.cachingIsSafe(weight, permissions));
    }

    private static FieldPermissionsDefinition fieldPermissionDef(String[] granted, String[] denied) {
        return new FieldPermissionsDefinition(granted, denied);
    }
}
