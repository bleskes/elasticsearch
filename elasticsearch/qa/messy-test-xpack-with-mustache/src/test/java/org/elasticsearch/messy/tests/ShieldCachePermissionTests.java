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

package org.elasticsearch.messy.tests;

import org.elasticsearch.Version;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.cache.query.terms.TermsLookup;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.script.Template;
import org.elasticsearch.script.mustache.MustachePlugin;
import org.elasticsearch.script.mustache.MustacheScriptEngineService;
import org.elasticsearch.shield.authc.support.SecuredString;
import org.elasticsearch.test.ShieldIntegTestCase;
import org.elasticsearch.test.ShieldSettingsSource;
import org.junit.Before;
import org.junit.BeforeClass;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static org.elasticsearch.shield.authc.support.UsernamePasswordToken.basicAuthHeaderValue;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

@ShieldIntegTestCase.AwaitsFix(bugUrl = "clean up test to not use mustache templates, otherwise needs many resources here")
public class ShieldCachePermissionTests extends ShieldIntegTestCase {
    static final String READ_ONE_IDX_USER = "read_user";
    
    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        Collection<Class<? extends Plugin>> types = new ArrayList<>();
        types.addAll(super.nodePlugins());
        types.add(MustachePlugin.class);
        return types;
    }

    @Override
    public String configUsers() {
        return super.configUsers()
                + READ_ONE_IDX_USER + ":" + ShieldSettingsSource.DEFAULT_PASSWORD_HASHED + "\n";
    }

    @Override
    public String configRoles() {
        return super.configRoles()
                + "\nread_one_idx:\n"
                + "  indices:\n"
                + "    'data': READ\n";
    }

    @Override
    public String configUsersRoles() {
        return super.configUsersRoles()
                + "read_one_idx:" + READ_ONE_IDX_USER + "\n";
    }

    @BeforeClass
    public static void checkVersion() {
        assumeTrue("These tests are only valid with elasticsearch 1.6.0+", Version.CURRENT.id >= 1060099);
    }

    @Before
    public void loadData() {
        index("data", "a", "1", "{ \"name\": \"John\", \"token\": \"token1\" }");
        index("tokens", "tokens", "1", "{ \"group\": \"1\", \"tokens\": [\"token1\", \"token2\"] }");
        client().preparePutIndexedScript().setOpType(IndexRequest.OpType.CREATE).setSource("{\n" +
                "\"template\": {\n" +
                "  \"query\": {\n" +
                "    \"exists\": {\n" +
                "      \"field\": \"{{name}}\"\n" +
                "     }\n" +
                "   }\n" +
                " }\n" +
                "}")
                .setScriptLang("mustache")
                .setId("testTemplate")
                .execute().actionGet();
        refresh();
    }

    public void testThatTermsFilterQueryDoesntLeakData() {
        SearchResponse response = client().prepareSearch("data").setTypes("a").setQuery(QueryBuilders.constantScoreQuery(
                QueryBuilders.termsLookupQuery("token", new TermsLookup("tokens", "tokens", "1", "tokens"))))
                .execute().actionGet();
        assertThat(response.isTimedOut(), is(false));
        assertThat(response.getHits().hits().length, is(1));

        // Repeat with unauthorized user!!!!
        try {
            response = client().filterWithHeader(Collections.singletonMap("Authorization", basicAuthHeaderValue(READ_ONE_IDX_USER,
                    new SecuredString("changeme".toCharArray()))))
                    .prepareSearch("data").setTypes("a").setQuery(QueryBuilders.constantScoreQuery(
                    QueryBuilders.termsLookupQuery("token", new TermsLookup("tokens", "tokens", "1", "tokens"))))
                    .execute().actionGet();
            fail("search phase exception should have been thrown! response was:\n" + response.toString());
        } catch (SearchPhaseExecutionException e) {
            assertThat(e.toString(), containsString("ElasticsearchSecurityException[action"));
            assertThat(e.toString(), containsString("unauthorized"));
        }
    }

    public void testThatScriptServiceDoesntLeakData() {
        SearchResponse response = client().prepareSearch("data").setTypes("a")
                .setTemplate(new Template("testTemplate", ScriptService.ScriptType.INDEXED, MustacheScriptEngineService.NAME, null,
                        Collections.<String, Object>singletonMap("name", "token")))
                .execute().actionGet();
        assertThat(response.isTimedOut(), is(false));
        assertThat(response.getHits().hits().length, is(1));

        // Repeat with unauthorized user!!!!
        try {
            response = client().filterWithHeader(Collections.singletonMap("Authorization", basicAuthHeaderValue(READ_ONE_IDX_USER,
                    new SecuredString("changeme".toCharArray()))))
                    .prepareSearch("data").setTypes("a")
                    .setTemplate(new Template("testTemplate", ScriptService.ScriptType.INDEXED, MustacheScriptEngineService.NAME, null,
                            Collections.<String, Object>singletonMap("name", "token")))
                    .execute().actionGet();
            fail("search phase exception should have been thrown! response was:\n" + response.toString());
        } catch (SearchPhaseExecutionException e) {
            assertThat(e.toString(), containsString("ElasticsearchSecurityException[action"));
            assertThat(e.toString(), containsString("unauthorized"));
        }
    }
}
