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
import org.elasticsearch.ExceptionsHelper;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.xpack.security.crypto.CryptoService;
import org.elasticsearch.xpack.security.crypto.InternalCryptoService;
import org.elasticsearch.test.SecurityIntegTestCase;

import java.util.Locale;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.test.SecurityTestsUtils.assertAuthorizationException;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertHitCount;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class ScrollIdSigningTests extends SecurityIntegTestCase {
    public void testSearchAndClearScroll() throws Exception {
        IndexRequestBuilder[] docs = new IndexRequestBuilder[randomIntBetween(20, 100)];
        for (int i = 0; i < docs.length; i++) {
            docs[i] = client().prepareIndex("idx", "type").setSource("field", "value");
        }
        indexRandom(true, docs);
        SearchResponse response = client().prepareSearch()
                .setQuery(matchAllQuery())
                .setScroll(TimeValue.timeValueMinutes(2))
                .setSize(randomIntBetween(1, 10)).get();

        int hits = 0;
        try {
            while (true) {
                assertSigned(response.getScrollId());
                assertHitCount(response, docs.length);
                hits += response.getHits().hits().length;
                response = client().prepareSearchScroll(response.getScrollId())
                        .setScroll(TimeValue.timeValueMinutes(2)).get();
                if (response.getHits().getHits().length == 0) {
                    break;
                }
            }
            assertThat(hits, equalTo(docs.length));
        } finally {
            clearScroll(response.getScrollId());
        }
    }

    public void testSearchScrollWithTamperedScrollId() throws Exception {
        IndexRequestBuilder[] docs = new IndexRequestBuilder[randomIntBetween(20, 100)];
        for (int i = 0; i < docs.length; i++) {
            docs[i] = client().prepareIndex("idx", "type").setSource("field", "value");
        }
        indexRandom(true, docs);
        SearchResponse response = client().prepareSearch()
                .setQuery(matchAllQuery())
                .setScroll(TimeValue.timeValueMinutes(2))
                .setSize(randomIntBetween(1, 10)).get();
        String scrollId = response.getScrollId();
        String tamperedScrollId = randomBoolean() ? scrollId.substring(randomIntBetween(1, 10)) :
                scrollId + randomAsciiOfLength(randomIntBetween(3, 10));
        try {
            client().prepareSearchScroll(tamperedScrollId).setScroll(TimeValue.timeValueMinutes(2)).get();
            fail("Expected an authorization exception to be thrown when scroll id is tampered");
        } catch (Exception e) {
            ElasticsearchSecurityException ese = (ElasticsearchSecurityException) ExceptionsHelper.unwrap(e,
                    ElasticsearchSecurityException.class);
            assertThat(ese, notNullValue());
            assertAuthorizationException(ese);
        } finally {
            clearScroll(scrollId);
        }
    }

    public void testClearScrollWithTamperedScrollId() throws Exception {
        IndexRequestBuilder[] docs = new IndexRequestBuilder[randomIntBetween(20, 100)];
        for (int i = 0; i < docs.length; i++) {
            docs[i] = client().prepareIndex("idx", "type").setSource("field", "value");
        }
        indexRandom(true, docs);
        SearchResponse response = client().prepareSearch()
                .setQuery(matchAllQuery())
                .setScroll(TimeValue.timeValueMinutes(2))
                .setSize(5).get();
        String scrollId = response.getScrollId();
        String tamperedScrollId = randomBoolean() ? scrollId.substring(randomIntBetween(1, 10)) :
                scrollId + randomAsciiOfLength(randomIntBetween(3, 10));
        try {
            client().prepareClearScroll().addScrollId(tamperedScrollId).get();
            fail("Expected an authorization exception to be thrown when scroll id is tampered");
        } catch (Exception e) {
            ElasticsearchSecurityException ese = (ElasticsearchSecurityException) ExceptionsHelper.unwrap(e,
                    ElasticsearchSecurityException.class);
            assertThat(ese, notNullValue());
            assertAuthorizationException(ese);
        } finally {
            clearScroll(scrollId);
        }
    }

    private void assertSigned(String scrollId) {
        CryptoService cryptoService = internalCluster().getDataNodeInstance(InternalCryptoService.class);
        String message = String.format(Locale.ROOT, "Expected scrollId [%s] to be signed, but was not", scrollId);
        assertThat(message, cryptoService.isSigned(scrollId), is(true));
    }
}
