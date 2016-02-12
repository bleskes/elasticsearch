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

package org.elasticsearch.integration;

import org.elasticsearch.ElasticsearchSecurityException;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.percolate.PercolateResponse;
import org.elasticsearch.action.percolate.PercolateSourceBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.termvectors.MultiTermVectorsResponse;
import org.elasticsearch.action.termvectors.TermVectorsRequest;
import org.elasticsearch.action.termvectors.TermVectorsResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.IndicesRequestCache;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.children.Children;
import org.elasticsearch.search.aggregations.bucket.global.Global;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.shield.Shield;
import org.elasticsearch.shield.authc.support.Hasher;
import org.elasticsearch.shield.authc.support.SecuredString;
import org.elasticsearch.test.ShieldIntegTestCase;
import org.elasticsearch.xpack.XPackPlugin;

import java.util.Collections;

import static org.elasticsearch.index.query.QueryBuilders.hasChildQuery;
import static org.elasticsearch.index.query.QueryBuilders.hasParentQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.elasticsearch.shield.authc.support.UsernamePasswordToken.BASIC_AUTH_HEADER;
import static org.elasticsearch.shield.authc.support.UsernamePasswordToken.basicAuthHeaderValue;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertAcked;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertHitCount;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertNoFailures;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertSearchHits;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 */
public class DocumentLevelSecurityTests extends ShieldIntegTestCase {

    protected static final SecuredString USERS_PASSWD = new SecuredString("change_me".toCharArray());
    protected static final String USERS_PASSWD_HASHED = new String(Hasher.BCRYPT.hash(USERS_PASSWD));

    @Override
    protected String configUsers() {
        return super.configUsers() +
                "user1:" + USERS_PASSWD_HASHED + "\n" +
                "user2:" + USERS_PASSWD_HASHED + "\n" ;
    }

    @Override
    protected String configUsersRoles() {
        return super.configUsersRoles() +
                "role1:user1\n" +
                "role2:user2\n";
    }
    @Override
    protected String configRoles() {
        return super.configRoles() +
                "\nrole1:\n" +
                "  cluster: all\n" +
                "  indices:\n" +
                "    '*':\n" +
                "      privileges: ALL\n" +
                "      query: \n" +
                "        term: \n" +
                "          field1: value1\n" +
                "role2:\n" +
                "  cluster: all\n" +
                "  indices:\n" +
                "    '*':\n" +
                "      privileges: ALL\n" +
                "      query: '{\"term\" : {\"field2\" : \"value2\"}}'"; // <-- query defined as json in a string
    }

    @Override
    public Settings nodeSettings(int nodeOrdinal) {
        return Settings.builder()
                .put(super.nodeSettings(nodeOrdinal))
                .put(XPackPlugin.featureEnabledSetting(Shield.DLS_FLS_FEATURE), true)
                .build();
    }

    public void testSimpleQuery() throws Exception {
        assertAcked(client().admin().indices().prepareCreate("test")
                        .addMapping("type1", "field1", "type=string", "field2", "type=string")
        );
        client().prepareIndex("test", "type1", "1").setSource("field1", "value1")
                .setRefresh(true)
                .get();
        client().prepareIndex("test", "type1", "2").setSource("field2", "value2")
                .setRefresh(true)
                .get();

        SearchResponse response = client()
                .filterWithHeader(Collections.singletonMap(BASIC_AUTH_HEADER, basicAuthHeaderValue("user1", USERS_PASSWD)))
                .prepareSearch("test")
                .setQuery(randomBoolean() ? QueryBuilders.termQuery("field1", "value1") : QueryBuilders.matchAllQuery())
                .get();
        assertHitCount(response, 1);
        assertSearchHits(response, "1");
        response = client().filterWithHeader(Collections.singletonMap(BASIC_AUTH_HEADER, basicAuthHeaderValue("user2", USERS_PASSWD)))
                .prepareSearch("test")
                .setQuery(randomBoolean() ? QueryBuilders.termQuery("field2", "value2") : QueryBuilders.matchAllQuery())
                .get();
        assertHitCount(response, 1);
        assertSearchHits(response, "2");
    }

    public void testGetApi() throws Exception {
        assertAcked(client().admin().indices().prepareCreate("test")
                        .addMapping("type1", "field1", "type=string", "field2", "type=string")
        );

        client().prepareIndex("test", "type1", "1").setSource("field1", "value1")
                .get();
        client().prepareIndex("test", "type1", "2").setSource("field2", "value2")
                .get();

        Boolean realtime = randomFrom(true, false, null);
        GetResponse response = client()
                .filterWithHeader(Collections.singletonMap(BASIC_AUTH_HEADER, basicAuthHeaderValue("user1", USERS_PASSWD)))
                .prepareGet("test", "type1", "1")
                .setRealtime(realtime)
                .setRefresh(true)
                .get();
        assertThat(response.isExists(), is(true));
        assertThat(response.getId(), equalTo("1"));
        response = client().filterWithHeader(Collections.singletonMap(BASIC_AUTH_HEADER, basicAuthHeaderValue("user2", USERS_PASSWD)))
                .prepareGet("test", "type1", "2")
                .setRealtime(realtime)
                .setRefresh(true)
                .get();
        assertThat(response.isExists(), is(true));
        assertThat(response.getId(), equalTo("2"));

        response = client().filterWithHeader(Collections.singletonMap(BASIC_AUTH_HEADER, basicAuthHeaderValue("user2", USERS_PASSWD)))
                .prepareGet("test", "type1", "1")
                .setRealtime(realtime)
                .setRefresh(true)
                .get();
        assertThat(response.isExists(), is(false));
        response = client().filterWithHeader(Collections.singletonMap(BASIC_AUTH_HEADER, basicAuthHeaderValue("user1", USERS_PASSWD)))
                .prepareGet("test", "type1", "2")
                .setRealtime(realtime)
                .setRefresh(true)
                .get();
        assertThat(response.isExists(), is(false));
    }

    public void testMGetApi() throws Exception {
        assertAcked(client().admin().indices().prepareCreate("test")
                        .addMapping("type1", "field1", "type=string", "field2", "type=string")
        );

        client().prepareIndex("test", "type1", "1").setSource("field1", "value1")
                .get();
        client().prepareIndex("test", "type1", "2").setSource("field2", "value2")
                .get();

        Boolean realtime = randomFrom(true, false, null);
        MultiGetResponse response = client()
                .filterWithHeader(Collections.singletonMap(BASIC_AUTH_HEADER, basicAuthHeaderValue("user1", USERS_PASSWD)))
                .prepareMultiGet()
                .add("test", "type1", "1")
                .setRealtime(realtime)
                .setRefresh(true)
                .get();
        assertThat(response.getResponses()[0].isFailed(), is(false));
        assertThat(response.getResponses()[0].getResponse().isExists(), is(true));
        assertThat(response.getResponses()[0].getResponse().getId(), equalTo("1"));

        response = client().filterWithHeader(Collections.singletonMap(BASIC_AUTH_HEADER, basicAuthHeaderValue("user2", USERS_PASSWD)))
                .prepareMultiGet()
                .add("test", "type1", "2")
                .setRealtime(realtime)
                .setRefresh(true)
                .get();
        assertThat(response.getResponses()[0].isFailed(), is(false));
        assertThat(response.getResponses()[0].getResponse().isExists(), is(true));
        assertThat(response.getResponses()[0].getResponse().getId(), equalTo("2"));

        response = client().filterWithHeader(Collections.singletonMap(BASIC_AUTH_HEADER, basicAuthHeaderValue("user2", USERS_PASSWD)))
                .prepareMultiGet()
                .add("test", "type1", "1")
                .setRealtime(realtime)
                .setRefresh(true)
                .get();
        assertThat(response.getResponses()[0].isFailed(), is(false));
        assertThat(response.getResponses()[0].getResponse().isExists(), is(false));

        response = client().filterWithHeader(Collections.singletonMap(BASIC_AUTH_HEADER, basicAuthHeaderValue("user1", USERS_PASSWD)))
                .prepareMultiGet()
                .add("test", "type1", "2")
                .setRealtime(realtime)
                .setRefresh(true)
                .get();
        assertThat(response.getResponses()[0].isFailed(), is(false));
        assertThat(response.getResponses()[0].getResponse().isExists(), is(false));
    }

    public void testTVApi() throws Exception {
        assertAcked(client().admin().indices().prepareCreate("test")
                        .addMapping("type1", "field1", "type=string,term_vector=with_positions_offsets_payloads",
                                "field2", "type=string,term_vector=with_positions_offsets_payloads")
        );
        client().prepareIndex("test", "type1", "1").setSource("field1", "value1")
                .setRefresh(true)
                .get();
        client().prepareIndex("test", "type1", "2").setSource("field2", "value2")
                .setRefresh(true)
                .get();

        Boolean realtime = randomFrom(true, false, null);
        TermVectorsResponse response = client()
                .filterWithHeader(Collections.singletonMap(BASIC_AUTH_HEADER, basicAuthHeaderValue("user1", USERS_PASSWD)))
                .prepareTermVectors("test", "type1", "1")
                .setRealtime(realtime)
                .get();
        assertThat(response.isExists(), is(true));
        assertThat(response.getId(), is("1"));

        response = client().filterWithHeader(Collections.singletonMap(BASIC_AUTH_HEADER, basicAuthHeaderValue("user2", USERS_PASSWD)))
                .prepareTermVectors("test", "type1", "2")
                .setRealtime(realtime)
                .get();
        assertThat(response.isExists(), is(true));
        assertThat(response.getId(), is("2"));

        response = client().filterWithHeader(Collections.singletonMap(BASIC_AUTH_HEADER, basicAuthHeaderValue("user2", USERS_PASSWD)))
                .prepareTermVectors("test", "type1", "1")
                .setRealtime(realtime)
                .get();
        assertThat(response.isExists(), is(false));

        response = client().filterWithHeader(Collections.singletonMap(BASIC_AUTH_HEADER, basicAuthHeaderValue("user1", USERS_PASSWD)))
                .prepareTermVectors("test", "type1", "2")
                .setRealtime(realtime)
                .get();
        assertThat(response.isExists(), is(false));
    }

    public void testMTVApi() throws Exception {
        assertAcked(client().admin().indices().prepareCreate("test")
                        .addMapping("type1", "field1", "type=string,term_vector=with_positions_offsets_payloads",
                                "field2", "type=string,term_vector=with_positions_offsets_payloads")
        );
        client().prepareIndex("test", "type1", "1").setSource("field1", "value1")
                .setRefresh(true)
                .get();
        client().prepareIndex("test", "type1", "2").setSource("field2", "value2")
                .setRefresh(true)
                .get();

        Boolean realtime = randomFrom(true, false, null);
        MultiTermVectorsResponse response = client()
                .filterWithHeader(Collections.singletonMap(BASIC_AUTH_HEADER, basicAuthHeaderValue("user1", USERS_PASSWD)))
                .prepareMultiTermVectors()
                .add(new TermVectorsRequest("test", "type1", "1").realtime(realtime))
                .get();
        assertThat(response.getResponses().length, equalTo(1));
        assertThat(response.getResponses()[0].getResponse().isExists(), is(true));
        assertThat(response.getResponses()[0].getResponse().getId(), is("1"));

        response = client().filterWithHeader(Collections.singletonMap(BASIC_AUTH_HEADER, basicAuthHeaderValue("user2", USERS_PASSWD)))
                .prepareMultiTermVectors()
                .add(new TermVectorsRequest("test", "type1", "2").realtime(realtime))
                .get();
        assertThat(response.getResponses().length, equalTo(1));
        assertThat(response.getResponses()[0].getResponse().isExists(), is(true));
        assertThat(response.getResponses()[0].getResponse().getId(), is("2"));

        response = client().filterWithHeader(Collections.singletonMap(BASIC_AUTH_HEADER, basicAuthHeaderValue("user2", USERS_PASSWD)))
                .prepareMultiTermVectors()
                .add(new TermVectorsRequest("test", "type1", "1").realtime(realtime))
                .get();
        assertThat(response.getResponses().length, equalTo(1));
        assertThat(response.getResponses()[0].getResponse().isExists(), is(false));

        response = client().filterWithHeader(Collections.singletonMap(BASIC_AUTH_HEADER, basicAuthHeaderValue("user1", USERS_PASSWD)))
                .prepareMultiTermVectors()
                .add(new TermVectorsRequest("test", "type1", "2").realtime(realtime))
                .get();
        assertThat(response.getResponses().length, equalTo(1));
        assertThat(response.getResponses()[0].getResponse().isExists(), is(false));
    }

    public void testGlobalAggregation() throws Exception {
        assertAcked(client().admin().indices().prepareCreate("test")
                        .addMapping("type1", "field1", "type=string", "field2", "type=string")
        );
        client().prepareIndex("test", "type1", "1").setSource("field1", "value1")
                .setRefresh(true)
                .get();
        client().prepareIndex("test", "type1", "2").setSource("field2", "value2")
                .setRefresh(true)
                .get();

        SearchResponse response = client().prepareSearch("test")
                .addAggregation(AggregationBuilders.global("global").subAggregation(AggregationBuilders.terms("field2").field("field2")))
                .get();
        assertHitCount(response, 2);
        assertSearchHits(response, "1", "2");

        Global globalAgg = response.getAggregations().get("global");
        assertThat(globalAgg.getDocCount(), equalTo(2L));
        Terms termsAgg = globalAgg.getAggregations().get("field2");
        assertThat(termsAgg.getBuckets().get(0).getKeyAsString(), equalTo("value2"));
        assertThat(termsAgg.getBuckets().get(0).getDocCount(), equalTo(1L));

        response = client().filterWithHeader(Collections.singletonMap(BASIC_AUTH_HEADER, basicAuthHeaderValue("user1", USERS_PASSWD)))
                .prepareSearch("test")
                .addAggregation(AggregationBuilders.global("global").subAggregation(AggregationBuilders.terms("field2").field("field2")))
                .get();
        assertHitCount(response, 1);
        assertSearchHits(response, "1");

        globalAgg = response.getAggregations().get("global");
        assertThat(globalAgg.getDocCount(), equalTo(1L));
        termsAgg = globalAgg.getAggregations().get("field2");
        assertThat(termsAgg.getBuckets().size(), equalTo(0));
    }

    public void testChildrenAggregation() throws Exception {
        assertAcked(client().admin().indices().prepareCreate("test")
                        .addMapping("type1", "field1", "type=string", "field2", "type=string")
                        .addMapping("type2", "_parent", "type=type1", "field3", "type=string")
        );
        client().prepareIndex("test", "type1", "1").setSource("field1", "value1")
                .setRefresh(true)
                .get();
        client().prepareIndex("test", "type2", "2").setSource("field3", "value3")
                .setParent("1")
                .setRefresh(true)
                .get();

        SearchResponse response = client().prepareSearch("test")
                .setTypes("type1")
                .addAggregation(AggregationBuilders.children("children").childType("type2")
                        .subAggregation(AggregationBuilders.terms("field3").field("field3")))
                .get();
        assertHitCount(response, 1);
        assertSearchHits(response, "1");

        Children children = response.getAggregations().get("children");
        assertThat(children.getDocCount(), equalTo(1L));
        Terms termsAgg = children.getAggregations().get("field3");
        assertThat(termsAgg.getBuckets().get(0).getKeyAsString(), equalTo("value3"));
        assertThat(termsAgg.getBuckets().get(0).getDocCount(), equalTo(1L));

        response = client().filterWithHeader(Collections.singletonMap(BASIC_AUTH_HEADER, basicAuthHeaderValue("user1", USERS_PASSWD)))
                .prepareSearch("test")
                .setTypes("type1")
                .addAggregation(AggregationBuilders.children("children").childType("type2")
                        .subAggregation(AggregationBuilders.terms("field3").field("field3")))
                .get();
        assertHitCount(response, 1);
        assertSearchHits(response, "1");

        children = response.getAggregations().get("children");
        assertThat(children.getDocCount(), equalTo(0L));
        termsAgg = children.getAggregations().get("field3");
        assertThat(termsAgg.getBuckets().size(), equalTo(0));
    }

    public void testParentChild() {
        assertAcked(prepareCreate("test")
                .addMapping("parent")
                .addMapping("child", "_parent", "type=parent", "field1", "type=string", "field2", "type=string"));
        ensureGreen();

        // index simple data
        client().prepareIndex("test", "parent", "p1").setSource("field1", "value1").get();
        client().prepareIndex("test", "child", "c1").setSource("field2", "value2").setParent("p1").get();
        client().prepareIndex("test", "child", "c2").setSource("field2", "value2").setParent("p1").get();
        refresh();

        SearchResponse searchResponse = client().prepareSearch("test")
                .setQuery(hasChildQuery("child", matchAllQuery()))
                .get();
        assertHitCount(searchResponse, 1L);
        assertThat(searchResponse.getHits().totalHits(), equalTo(1L));
        assertThat(searchResponse.getHits().getAt(0).id(), equalTo("p1"));

        searchResponse = client().prepareSearch("test")
                .setQuery(hasParentQuery("parent", matchAllQuery()))
                .addSort("_id", SortOrder.ASC)
                .get();
        assertHitCount(searchResponse, 2L);
        assertThat(searchResponse.getHits().getAt(0).id(), equalTo("c1"));
        assertThat(searchResponse.getHits().getAt(1).id(), equalTo("c2"));

        // Both user1 and user2 can't see field1 and field2, no parent/child query should yield results:
        searchResponse = client().filterWithHeader(Collections.singletonMap(BASIC_AUTH_HEADER, basicAuthHeaderValue("user1", USERS_PASSWD)))
                .prepareSearch("test")
                .setQuery(hasChildQuery("child", matchAllQuery()))
                .get();
        assertHitCount(searchResponse, 0L);

        searchResponse = client().filterWithHeader(Collections.singletonMap(BASIC_AUTH_HEADER, basicAuthHeaderValue("user2", USERS_PASSWD)))
                .prepareSearch("test")
                .setQuery(hasChildQuery("child", matchAllQuery()))
                .get();
        assertHitCount(searchResponse, 0L);

        searchResponse = client().filterWithHeader(Collections.singletonMap(BASIC_AUTH_HEADER, basicAuthHeaderValue("user1", USERS_PASSWD)))
                .prepareSearch("test")
                .setQuery(hasParentQuery("parent", matchAllQuery()))
                .get();
        assertHitCount(searchResponse, 0L);

        searchResponse = client().filterWithHeader(Collections.singletonMap(BASIC_AUTH_HEADER, basicAuthHeaderValue("user2", USERS_PASSWD)))
                .prepareSearch("test")
                .setQuery(hasParentQuery("parent", matchAllQuery()))
                .get();
        assertHitCount(searchResponse, 0L);
    }

    public void testPercolateApi() {
        assertAcked(client().admin().indices().prepareCreate("test")
                        .addMapping(".percolator", "field1", "type=string", "field2", "type=string")
        );
        client().prepareIndex("test", ".percolator", "1")
                .setSource("{\"query\" : { \"match_all\" : {} }, \"field1\" : \"value1\"}")
                .setRefresh(true)
                .get();

        // Percolator without a query just evaluates all percolator queries that are loaded, so we have a match:
        PercolateResponse response = client()
                .filterWithHeader(Collections.singletonMap(BASIC_AUTH_HEADER, basicAuthHeaderValue("user1", USERS_PASSWD)))
                .preparePercolate()
                .setDocumentType("type")
                .setPercolateDoc(new PercolateSourceBuilder.DocBuilder().setDoc("{}"))
                .get();
        assertThat(response.getCount(), equalTo(1L));
        assertThat(response.getMatches()[0].getId().string(), equalTo("1"));

        // Percolator with a query on a document that the current user can see. Percolator will have one query to evaluate, so there is a
        // match:
        response = client().filterWithHeader(Collections.singletonMap(BASIC_AUTH_HEADER, basicAuthHeaderValue("user1", USERS_PASSWD)))
                .preparePercolate()
                .setDocumentType("type")
                .setPercolateQuery(termQuery("field1", "value1"))
                .setPercolateDoc(new PercolateSourceBuilder.DocBuilder().setDoc("{}"))
                .get();
        assertThat(response.getCount(), equalTo(1L));
        assertThat(response.getMatches()[0].getId().string(), equalTo("1"));

        // Percolator with a query on a document that the current user can't see. Percolator will not have queries to evaluate, so there
        // is no match:
        response = client().filterWithHeader(Collections.singletonMap(BASIC_AUTH_HEADER, basicAuthHeaderValue("user2", USERS_PASSWD)))
                .preparePercolate()
                .setDocumentType("type")
                .setPercolateQuery(termQuery("field1", "value1"))
                .setPercolateDoc(new PercolateSourceBuilder.DocBuilder().setDoc("{}"))
                .get();
        assertThat(response.getCount(), equalTo(0L));

        assertAcked(client().admin().indices().prepareClose("test"));
        assertAcked(client().admin().indices().prepareOpen("test"));
        ensureGreen("test");

        // Ensure that the query loading that happens at startup has permissions to load the percolator queries:
        response = client().filterWithHeader(Collections.singletonMap(BASIC_AUTH_HEADER, basicAuthHeaderValue("user1", USERS_PASSWD)))
                .preparePercolate()
                .setDocumentType("type")
                .setPercolateDoc(new PercolateSourceBuilder.DocBuilder().setDoc("{}"))
                .get();
        assertThat(response.getCount(), equalTo(1L));
        assertThat(response.getMatches()[0].getId().string(), equalTo("1"));
    }

    public void testRequestCache() throws Exception {
        assertAcked(client().admin().indices().prepareCreate("test")
                .setSettings(Settings.builder().put(IndicesRequestCache.INDEX_CACHE_REQUEST_ENABLED_SETTING.getKey(), true))
                .addMapping("type1", "field1", "type=string", "field2", "type=string")
        );
        client().prepareIndex("test", "type1", "1").setSource("field1", "value1")
                .get();
        client().prepareIndex("test", "type1", "2").setSource("field2", "value2")
                .get();
        refresh();

        int max = scaledRandomIntBetween(4, 32);
        for (int i = 0; i < max; i++) {
            Boolean requestCache = randomFrom(true, null);
            SearchResponse response = client()
                    .filterWithHeader(Collections.singletonMap(BASIC_AUTH_HEADER, basicAuthHeaderValue("user1", USERS_PASSWD)))
                    .prepareSearch("test")
                    .setSize(0)
                    .setQuery(termQuery("field1", "value1"))
                    .setRequestCache(requestCache)
                    .get();
            assertNoFailures(response);
            assertHitCount(response, 1);
            response = client().filterWithHeader(Collections.singletonMap(BASIC_AUTH_HEADER, basicAuthHeaderValue("user2", USERS_PASSWD)))
                    .prepareSearch("test")
                    .setSize(0)
                    .setQuery(termQuery("field1", "value1"))
                    .setRequestCache(requestCache)
                    .get();
            assertNoFailures(response);
            assertHitCount(response, 0);
        }
    }

    public void testUpdateApiIsBlocked() throws Exception {
        assertAcked(client().admin().indices().prepareCreate("test")
                .addMapping("type", "field1", "type=string", "field2", "type=string")
        );
        client().prepareIndex("test", "type", "1").setSource("field1", "value1")
                .setRefresh(true)
                .get();

        // With document level security enabled the update is not allowed:
        try {
            client().filterWithHeader(Collections.singletonMap(BASIC_AUTH_HEADER, basicAuthHeaderValue("user1", USERS_PASSWD)))
                    .prepareUpdate("test", "type", "1").setDoc("field1", "value2")
                    .get();
            fail("failed, because update request shouldn't be allowed if document level security is enabled");
        } catch (ElasticsearchSecurityException e) {
            assertThat(e.status(), equalTo(RestStatus.BAD_REQUEST));
            assertThat(e.getMessage(), equalTo("Can't execute an update request if field or document level security is enabled"));
        }
        assertThat(client().prepareGet("test", "type", "1").get().getSource().get("field1").toString(), equalTo("value1"));

        // With no document level security enabled the update is allowed:
        client().prepareUpdate("test", "type", "1").setDoc("field1", "value2")
                .get();
        assertThat(client().prepareGet("test", "type", "1").get().getSource().get("field1").toString(), equalTo("value2"));

        // With document level security enabled the update in bulk is not allowed:
        try {
            client().filterWithHeader(Collections.singletonMap(BASIC_AUTH_HEADER, basicAuthHeaderValue("user1", USERS_PASSWD)))
                    .prepareBulk()
                    .add(new UpdateRequest("test", "type", "1").doc("field1", "value3"))
                    .get();
            fail("failed, because bulk request with updates shouldn't be allowed if field or document level security is enabled");
        } catch (ElasticsearchSecurityException e) {
            assertThat(e.status(), equalTo(RestStatus.BAD_REQUEST));
            assertThat(e.getMessage(),
                    equalTo("Can't execute an bulk request with update requests embedded if field or document level security is enabled"));
        }
        assertThat(client().prepareGet("test", "type", "1").get().getSource().get("field1").toString(), equalTo("value2"));

        client().prepareBulk()
                .add(new UpdateRequest("test", "type", "1").doc("field1", "value3"))
                .get();
        assertThat(client().prepareGet("test", "type", "1").get().getSource().get("field1").toString(), equalTo("value3"));
    }

}
