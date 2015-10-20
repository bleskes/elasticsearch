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

package org.elasticsearch.marvel.agent.renderer.cluster;

import org.elasticsearch.Version;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.license.core.License;
import org.elasticsearch.marvel.agent.collector.cluster.ClusterInfoCollector;
import org.elasticsearch.marvel.agent.settings.MarvelSettings;
import org.elasticsearch.marvel.test.MarvelIntegTestCase;
import org.elasticsearch.test.ESIntegTestCase.ClusterScope;
import org.junit.After;
import org.junit.Before;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.elasticsearch.test.ESIntegTestCase.Scope.TEST;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertHitCount;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;

@ClusterScope(scope = TEST)
public class ClusterInfoTests extends MarvelIntegTestCase {
    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        return Settings.builder()
                .put(super.nodeSettings(nodeOrdinal))
                .put(MarvelSettings.INTERVAL, "-1")
                .put(MarvelSettings.COLLECTORS, ClusterInfoCollector.NAME)
                .build();
    }

    @Before
    public void init() throws Exception {
        updateMarvelInterval(3L, TimeUnit.SECONDS);
    }

    @After
    public void cleanup() throws Exception {
        updateMarvelInterval(-1, TimeUnit.SECONDS);
        wipeMarvelIndices();
    }

    public void testClusterInfo() throws Exception {
        securedEnsureGreen();

        final String clusterUUID = client().admin().cluster().prepareState().setMetaData(true).get().getState().metaData().clusterUUID();
        assertTrue(Strings.hasText(clusterUUID));

        logger.debug("--> waiting for the marvel data index to be created (it should have been created by the LicenseCollector)");
        awaitIndexExists(MarvelSettings.MARVEL_DATA_INDEX_NAME);

        logger.debug("--> waiting for cluster info collector to collect data");
        awaitMarvelDocsCount(equalTo(1L), ClusterInfoCollector.TYPE);

        logger.debug("--> retrieving cluster info document");
        GetResponse response = client().prepareGet(MarvelSettings.MARVEL_DATA_INDEX_NAME, ClusterInfoCollector.TYPE, clusterUUID).get();
        assertTrue(MarvelSettings.MARVEL_DATA_INDEX_NAME + " document does not exist", response.isExists());

        logger.debug("--> checking that the document contains all required information");

        logger.debug("--> checking that the document contains license information");
        assertThat(response.getIndex(), equalTo(MarvelSettings.MARVEL_DATA_INDEX_NAME));
        assertThat(response.getType(), equalTo(ClusterInfoCollector.TYPE));
        assertThat(response.getId(), equalTo(clusterUUID));

        Map<String, Object> source = response.getSource();
        assertThat((String) source.get(ClusterInfoRenderer.Fields.CLUSTER_NAME.underscore().toString()), equalTo(cluster().getClusterName()));
        assertThat((String) source.get(ClusterInfoRenderer.Fields.VERSION.underscore().toString()), equalTo(Version.CURRENT.toString()));

        Object licenseObj = source.get(ClusterInfoRenderer.Fields.LICENSE.underscore().toString());
        assertThat(licenseObj, instanceOf(Map.class));
        Map license = (Map) licenseObj;

        assertThat(license, instanceOf(Map.class));

        String uid = (String) license.get(ClusterInfoRenderer.Fields.UID.underscore().toString());
        assertThat(uid, not(isEmptyOrNullString()));

        String type = (String) license.get(ClusterInfoRenderer.Fields.TYPE.underscore().toString());
        assertThat(type, not(isEmptyOrNullString()));

        String status = (String) license.get(License.XFields.STATUS.underscore().toString());
        assertThat(status, not(isEmptyOrNullString()));

        Long expiryDate = (Long) license.get(License.XFields.EXPIRY_DATE_IN_MILLIS.underscore().toString());
        assertThat(expiryDate, greaterThan(0L));

        // We basically recompute the hash here
        String hkey = (String) license.get(ClusterInfoRenderer.Fields.HKEY.underscore().toString());
        String recalculated = ClusterInfoRenderer.hash(status, uid, type, String.valueOf(expiryDate), clusterUUID);
        assertThat(hkey, equalTo(recalculated));

        assertThat((String) license.get(License.XFields.ISSUER.underscore().toString()), not(isEmptyOrNullString()));
        assertThat((String) license.get(License.XFields.ISSUED_TO.underscore().toString()), not(isEmptyOrNullString()));
        assertThat((Long) license.get(License.XFields.ISSUE_DATE_IN_MILLIS.underscore().toString()), greaterThan(0L));
        assertThat((Integer) license.get(License.XFields.MAX_NODES.underscore().toString()), greaterThan(0));

        Object clusterStats = source.get(ClusterStatsRenderer.Fields.CLUSTER_STATS.underscore().toString());
        assertNotNull(clusterStats);
        assertThat(clusterStats, instanceOf(Map.class));
        assertThat(((Map) clusterStats).size(), greaterThan(0));

        assertMarvelTemplateInstalled();

        logger.debug("--> check that the cluster_info is not indexed");
        securedFlush();
        securedRefresh();

        assertHitCount(client().prepareSearch().setSize(0)
                .setIndices(MarvelSettings.MARVEL_DATA_INDEX_NAME)
                .setTypes(ClusterInfoCollector.TYPE)
                .setQuery(QueryBuilders.boolQuery()
                                .should(QueryBuilders.matchQuery(License.XFields.STATUS.underscore().toString(), License.Status.ACTIVE.label()))
                                .should(QueryBuilders.matchQuery(License.XFields.STATUS.underscore().toString(), License.Status.INVALID.label()))
                                .should(QueryBuilders.matchQuery(License.XFields.STATUS.underscore().toString(), License.Status.EXPIRED.label()))
                                .minimumNumberShouldMatch(1)
                ).get(), 0L);
    }
}
