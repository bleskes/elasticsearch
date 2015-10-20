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
import org.elasticsearch.action.get.MultiGetAction;
import org.elasticsearch.action.search.MultiSearchAction;
import org.elasticsearch.action.search.SearchAction;
import org.elasticsearch.action.suggest.SuggestAction;
import org.elasticsearch.shield.support.AutomatonPredicate;
import org.elasticsearch.shield.support.Automatons;
import org.elasticsearch.test.ESTestCase;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.function.Predicate;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 *
 */
public class PrivilegeTests extends ESTestCase {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    public void testName() throws Exception {
        Privilege.Name name12 = new Privilege.Name("name1", "name2");
        Privilege.Name name34 = new Privilege.Name("name3", "name4");
        Privilege.Name name1234 = randomBoolean() ? name12.add(name34) : name34.add(name12);
        assertThat(name1234, equalTo(new Privilege.Name("name1", "name2", "name3", "name4")));

        Privilege.Name name1 = name12.remove(new Privilege.Name("name2"));
        assertThat(name1, equalTo(new Privilege.Name("name1")));

        Privilege.Name name = name1.remove(new Privilege.Name("name1"));
        assertThat(name, is(Privilege.Name.NONE));

        Privilege.Name none = new Privilege.Name("name1", "name2", "none").remove(name12);
        assertThat(none, is(Privilege.Name.NONE));
    }

    public void testSubActionPattern() throws Exception {
        AutomatonPredicate predicate = new AutomatonPredicate(Automatons.patterns("foo" + Privilege.SUB_ACTION_SUFFIX_PATTERN));
        assertThat(predicate.test("foo[n][nodes]"), is(true));
        assertThat(predicate.test("foo[n]"), is(true));
        assertThat(predicate.test("bar[n][nodes]"), is(false));
        assertThat(predicate.test("[n][nodes]"), is(false));
    }

    public void testCluster() throws Exception {
        Privilege.Name name = new Privilege.Name("monitor");
        Privilege.Cluster cluster = Privilege.Cluster.get(name);
        assertThat(cluster, is(Privilege.Cluster.MONITOR));

        // since "all" implies "monitor", this should collapse to All
        name = new Privilege.Name("monitor", "all");
        cluster = Privilege.Cluster.get(name);
        assertThat(cluster, is(Privilege.Cluster.ALL));

        name = new Privilege.Name("monitor", "none");
        cluster = Privilege.Cluster.get(name);
        assertThat(cluster, is(Privilege.Cluster.MONITOR));

        Privilege.Name name2 = new Privilege.Name("none", "monitor");
        Privilege.Cluster cluster2 = Privilege.Cluster.get(name2);
        assertThat(cluster, is(cluster2));
    }

    public void testClusterTemplateActions() throws Exception {
        Privilege.Name name = new Privilege.Name("indices:admin/template/delete");
        Privilege.Cluster cluster = Privilege.Cluster.get(name);
        assertThat(cluster, notNullValue());
        assertThat(cluster.predicate().test("indices:admin/template/delete"), is(true));

        name = new Privilege.Name("indices:admin/template/get");
        cluster = Privilege.Cluster.get(name);
        assertThat(cluster, notNullValue());
        assertThat(cluster.predicate().test("indices:admin/template/get"), is(true));

        name = new Privilege.Name("indices:admin/template/put");
        cluster = Privilege.Cluster.get(name);
        assertThat(cluster, notNullValue());
        assertThat(cluster.predicate().test("indices:admin/template/put"), is(true));
    }

    public void testClusterInvalidName() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        Privilege.Name actionName = new Privilege.Name("foobar");
        Privilege.Cluster.get(actionName);
    }

    public void testClusterAction() throws Exception {
        Privilege.Name actionName = new Privilege.Name("cluster:admin/snapshot/delete");
        Privilege.Cluster cluster = Privilege.Cluster.get(actionName);
        assertThat(cluster, notNullValue());
        assertThat(cluster.predicate().test("cluster:admin/snapshot/delete"), is(true));
        assertThat(cluster.predicate().test("cluster:admin/snapshot/dele"), is(false));
    }

    public void testClusterAddCustom() throws Exception {
        Privilege.Cluster.addCustom("foo", "cluster:bar");
        boolean found = false;
        for (Privilege.Cluster cluster : Privilege.Cluster.values()) {
            if ("foo".equals(cluster.name.toString())) {
                found = true;
                assertThat(cluster.predicate().test("cluster:bar"), is(true));
            }
        }
        assertThat(found, is(true));
        Privilege.Cluster cluster = Privilege.Cluster.get(new Privilege.Name("foo"));
        assertThat(cluster, notNullValue());
        assertThat(cluster.name().toString(), is("foo"));
        assertThat(cluster.predicate().test("cluster:bar"), is(true));
    }

    public void testClusterAddCustomInvalidPattern() throws Exception {
        try {
            Privilege.Cluster.addCustom("foo", "bar");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("cannot register custom cluster privilege [foo]"));
            assertThat(e.getMessage(), containsString("must follow the 'cluster:*' format"));
        }
    }

    public void testClusterAddCustomAlreadyExists() throws Exception {
        try {
            Privilege.Cluster.addCustom("all", "bar");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("cannot register custom cluster privilege [all]"));
            assertThat(e.getMessage(), containsString("must follow the 'cluster:*' format"));
        }
    }

    public void testIndexAction() throws Exception {
        Privilege.Name actionName = new Privilege.Name("indices:admin/mapping/delete");
        Privilege.Index index = Privilege.Index.get(actionName);
        assertThat(index, notNullValue());
        assertThat(index.predicate().test("indices:admin/mapping/delete"), is(true));
        assertThat(index.predicate().test("indices:admin/mapping/dele"), is(false));
    }

    public void testIndexCollapse() throws Exception {
        Privilege.Index[] values = Privilege.Index.values().toArray(new Privilege.Index[Privilege.Index.values().size()]);
        Privilege.Index first = values[randomIntBetween(0, values.length-1)];
        Privilege.Index second = values[randomIntBetween(0, values.length-1)];

        Privilege.Name name = new Privilege.Name(first.name().toString(), second.name().toString());
        Privilege.Index index = Privilege.Index.get(name);

        if (first.implies(second)) {
            assertThat(index, is(first));
        }

        if (second.implies(first)) {
            assertThat(index, is(second));
        }
    }

    public void testIndexImplies() throws Exception {
        Privilege.Index[] values = Privilege.Index.values().toArray(new Privilege.Index[Privilege.Index.values().size()]);
        Privilege.Index first = values[randomIntBetween(0, values.length-1)];
        Privilege.Index second = values[randomIntBetween(0, values.length-1)];

        Privilege.Name name = new Privilege.Name(first.name().toString(), second.name().toString());
        Privilege.Index index = Privilege.Index.get(name);

        assertThat(index.implies(first), is(true));
        assertThat(index.implies(second), is(true));

        if (first.implies(second)) {
            assertThat(index, is(first));
        }

        if (second.implies(first)) {
            if (index != second) {
                Privilege.Index idx = Privilege.Index.get(name);
                idx.name().toString();
            }
            assertThat(index, is(second));
        }

        for (Privilege.Index other : Privilege.Index.values()) {
            if (first.implies(other) || second.implies(other) || index.isAlias(other)) {
                assertThat("index privilege [" + index + "] should imply [" + other + "]", index.implies(other), is(true));
            } else if (other.implies(first) && other.implies(second)) {
                assertThat("index privilege [" + index + "] should not imply [" + other + "]", index.implies(other), is(false));
            }
        }
    }

    public void testIndexAddCustom() throws Exception {
        Privilege.Index.addCustom("foo", "indices:bar");
        boolean found = false;
        for (Privilege.Index index : Privilege.Index.values()) {
            if ("foo".equals(index.name.toString())) {
                found = true;
                assertThat(index.predicate().test("indices:bar"), is(true));
            }
        }
        assertThat(found, is(true));
        Privilege.Index index = Privilege.Index.get(new Privilege.Name("foo"));
        assertThat(index, notNullValue());
        assertThat(index.name().toString(), is("foo"));
        assertThat(index.predicate().test("indices:bar"), is(true));
    }

    public void testIndexAddCustomInvalidPattern() throws Exception {
        try {
            Privilege.Index.addCustom("foo", "bar");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("cannot register custom index privilege [foo]"));
            assertThat(e.getMessage(), containsString("must follow the 'indices:*' format"));
        }
    }

    public void testIndexAddCustomAlreadyExists() throws Exception {
        try {
            Privilege.Index.addCustom("all", "bar");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("cannot register custom index privilege [all]"));
            assertThat(e.getMessage(), containsString("must follow the 'indices:*' format"));
        }
    }

    public void testSystem() throws Exception {
        Predicate<String> predicate = Privilege.SYSTEM.predicate();
        assertThat(predicate.test("indices:monitor/whatever"), is(true));
        assertThat(predicate.test("cluster:monitor/whatever"), is(true));
        assertThat(predicate.test("cluster:admin/snapshot/status[nodes]"), is(false));
        assertThat(predicate.test("internal:whatever"), is(true));
        assertThat(predicate.test("indices:whatever"), is(false));
        assertThat(predicate.test("cluster:whatever"), is(false));
        assertThat(predicate.test("cluster:admin/snapshot/status"), is(false));
        assertThat(predicate.test("whatever"), is(false));
        assertThat(predicate.test("cluster:admin/reroute"), is(true));
        assertThat(predicate.test("cluster:admin/whatever"), is(false));
        assertThat(predicate.test("indices:admin/mapping/put"), is(true));
        assertThat(predicate.test("indices:admin/mapping/whatever"), is(false));
    }

    public void testSearchPrivilege() throws Exception {
        Predicate<String> predicate = Privilege.Index.SEARCH.predicate();
        assertThat(predicate.test(SearchAction.NAME), is(true));
        assertThat(predicate.test(SearchAction.NAME + "/whatever"), is(true));
        assertThat(predicate.test(MultiSearchAction.NAME), is(true));
        assertThat(predicate.test(MultiSearchAction.NAME + "/whatever"), is(true));
        assertThat(predicate.test(SuggestAction.NAME), is(true));
        assertThat(predicate.test(SuggestAction.NAME + "/whatever"), is(true));

        assertThat(predicate.test(GetAction.NAME), is(false));
        assertThat(predicate.test(GetAction.NAME + "/whatever"), is(false));
        assertThat(predicate.test(MultiGetAction.NAME), is(false));
        assertThat(predicate.test(MultiGetAction.NAME + "/whatever"), is(false));
    }

    public void testGetPrivilege() throws Exception {
        Predicate<String> predicate = Privilege.Index.GET.predicate();
        assertThat(predicate.test(GetAction.NAME), is(true));
        assertThat(predicate.test(GetAction.NAME + "/whatever"), is(true));
        assertThat(predicate.test(MultiGetAction.NAME), is(true));
        assertThat(predicate.test(MultiGetAction.NAME + "/whatever"), is(true));
    }
}
