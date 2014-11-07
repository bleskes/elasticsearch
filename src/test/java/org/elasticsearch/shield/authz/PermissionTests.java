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

package org.elasticsearch.shield.authz;

import org.elasticsearch.action.get.GetAction;
import org.elasticsearch.common.base.Predicate;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.junit.Before;
import org.junit.Test;

import static org.elasticsearch.shield.authz.Privilege.Index.*;
import static org.hamcrest.Matchers.is;

/**
 *
 */
public class PermissionTests extends ElasticsearchTestCase {

    private Permission.Global.Role permission;

    @Before
    public void init() {
        Permission.Global.Role.Builder builder = Permission.Global.Role.builder("test");
        builder.add(union(SEARCH, MONITOR), "test_*", "/foo.*/");
        builder.add(union(READ), "baz_*foo", "/fool.*bar/");
        builder.add(union(MONITOR), "/bar.*/");
        permission = builder.build();
    }

    @Test
    public void testAllowedIndicesMatcher_Action() throws Exception {
        testAllowedIndicesMatcher(permission.indices().allowedIndicesMatcher(GetAction.NAME));
    }

    @Test
    public void testAllowedIndicesMatcher_Action_Caching() throws Exception {
        Predicate<String> matcher1 = permission.indices().allowedIndicesMatcher(GetAction.NAME);
        Predicate<String> matcher2 = permission.indices().allowedIndicesMatcher(GetAction.NAME);
        assertThat(matcher1, is(matcher2));
    }

    private void testAllowedIndicesMatcher(Predicate<String> indicesMatcher) {
        assertThat(indicesMatcher.apply("test_123"), is(true));
        assertThat(indicesMatcher.apply("foobar"), is(true));
        assertThat(indicesMatcher.apply("fool"), is(true));
        assertThat(indicesMatcher.apply("fool2bar"), is(true));
        assertThat(indicesMatcher.apply("barbapapa"), is(false));
    }


}
