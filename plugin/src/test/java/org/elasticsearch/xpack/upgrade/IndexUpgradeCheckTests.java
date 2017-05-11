/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2017] Elasticsearch Incorporated. All Rights Reserved.
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

package org.elasticsearch.xpack.upgrade;

import org.elasticsearch.Version;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.UUIDs;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.test.ESTestCase;

import java.util.Collections;

import static org.hamcrest.core.IsEqual.equalTo;

public class IndexUpgradeCheckTests extends ESTestCase {

    public void testGenericUpgradeCheck() {
        IndexUpgradeCheck check = new GenericIndexUpgradeCheck();
        assertThat(check.getName(), equalTo("generic"));
        IndexMetaData goodIndex = newTestIndexMeta("good", Settings.EMPTY);
        IndexMetaData badIndex = newTestIndexMeta("bad",
                Settings.builder().put(IndexMetaData.SETTING_VERSION_CREATED, Version.fromString("2.0.0")).build());

        assertThat(check.actionRequired(goodIndex, Collections.emptyMap(), ClusterState.EMPTY_STATE),
                equalTo(UpgradeActionRequired.UP_TO_DATE));
        assertThat(check.actionRequired(badIndex, Collections.emptyMap(), ClusterState.EMPTY_STATE),
                equalTo(UpgradeActionRequired.REINDEX));
    }

    public void testInternalUpgradeCheck() {
        IndexUpgradeCheck check = new InternalIndexUpgradeCheck();
        assertThat(check.getName(), equalTo("inner"));
        IndexMetaData goodKibanaIndex = newTestIndexMeta(".kibana", Settings.EMPTY);
        assertThat(check.actionRequired(goodKibanaIndex, Collections.emptyMap(), ClusterState.EMPTY_STATE),
                equalTo(UpgradeActionRequired.UPGRADE));

        IndexMetaData renamedKibanaIndex = newTestIndexMeta(".kibana2", Settings.EMPTY);
        assertThat(check.actionRequired(renamedKibanaIndex, Collections.emptyMap(), ClusterState.EMPTY_STATE),
                equalTo(UpgradeActionRequired.NOT_APPLICABLE));

        assertThat(check.actionRequired(renamedKibanaIndex, Collections.singletonMap("kibana_indices", ".kibana*"),
                ClusterState.EMPTY_STATE), equalTo(UpgradeActionRequired.UPGRADE));

        assertThat(check.actionRequired(renamedKibanaIndex, Collections.singletonMap("kibana_indices", ".kibana1,.kibana2"),
                ClusterState.EMPTY_STATE), equalTo(UpgradeActionRequired.UPGRADE));

        IndexMetaData watcherIndex = newTestIndexMeta(".watches", Settings.EMPTY);
        assertThat(check.actionRequired(watcherIndex, Collections.singletonMap("kibana_indices", ".kibana*"), ClusterState.EMPTY_STATE),
                equalTo(UpgradeActionRequired.UPGRADE));

        IndexMetaData securityIndex = newTestIndexMeta(".security", Settings.EMPTY);
        assertThat(check.actionRequired(securityIndex, Collections.singletonMap("kibana_indices", ".kibana*"), ClusterState.EMPTY_STATE),
                equalTo(UpgradeActionRequired.UPGRADE));
    }

    public static IndexMetaData newTestIndexMeta(String name, Settings indexSettings) {
        Settings build = Settings.builder().put(IndexMetaData.SETTING_VERSION_CREATED, Version.CURRENT)
                .put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, 1)
                .put(IndexMetaData.SETTING_NUMBER_OF_SHARDS, 1)
                .put(IndexMetaData.SETTING_CREATION_DATE, 1)
                .put(IndexMetaData.SETTING_INDEX_UUID, UUIDs.randomBase64UUID())
                .put(IndexMetaData.SETTING_VERSION_UPGRADED, Version.V_5_0_0_beta1)
                .put(indexSettings)
                .build();
        return IndexMetaData.builder(name).settings(build).build();
    }

}
