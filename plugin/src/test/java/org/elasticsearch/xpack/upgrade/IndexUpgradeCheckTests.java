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
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.UUIDs;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.test.ESTestCase;

import java.io.IOException;
import java.util.Collections;

import static org.hamcrest.core.IsEqual.equalTo;

public class IndexUpgradeCheckTests extends ESTestCase {

    public void testKibanaUpgradeCheck() throws Exception {
        IndexUpgradeCheck check = Upgrade.getKibanaUpgradeCheckFactory(Settings.EMPTY).v2().apply(null, null);
        assertThat(check.getName(), equalTo("kibana"));
        IndexMetaData goodKibanaIndex = newTestIndexMeta(".kibana", Settings.EMPTY);
        assertThat(check.actionRequired(goodKibanaIndex, Collections.emptyMap()),
                equalTo(UpgradeActionRequired.UPGRADE));

        IndexMetaData renamedKibanaIndex = newTestIndexMeta(".kibana2", Settings.EMPTY);
        assertThat(check.actionRequired(renamedKibanaIndex, Collections.emptyMap()),
                equalTo(UpgradeActionRequired.NOT_APPLICABLE));

        assertThat(check.actionRequired(renamedKibanaIndex, Collections.singletonMap("kibana_indices", ".kibana*")
        ), equalTo(UpgradeActionRequired.UPGRADE));

        assertThat(check.actionRequired(renamedKibanaIndex, Collections.singletonMap("kibana_indices", ".kibana1,.kibana2")
        ), equalTo(UpgradeActionRequired.UPGRADE));

        IndexMetaData watcherIndex = newTestIndexMeta(".watches", Settings.EMPTY);
        assertThat(check.actionRequired(watcherIndex, Collections.singletonMap("kibana_indices", ".kibana*")),
                equalTo(UpgradeActionRequired.NOT_APPLICABLE));

        IndexMetaData securityIndex = newTestIndexMeta(".security", Settings.EMPTY);
        assertThat(check.actionRequired(securityIndex, Collections.singletonMap("kibana_indices", ".kibana*")),
                equalTo(UpgradeActionRequired.NOT_APPLICABLE));
    }

    public void testWatcherIndexUpgradeCheck() throws Exception{
        IndexUpgradeCheck check = Upgrade.getWatcherUpgradeCheckFactory(Settings.EMPTY).v2().apply(null, null);
        assertThat(check.getName(), equalTo("watcher"));

        IndexMetaData goodKibanaIndex = newTestIndexMeta(".kibana", Settings.EMPTY);
        assertThat(check.actionRequired(goodKibanaIndex, Collections.emptyMap()),
                equalTo(UpgradeActionRequired.NOT_APPLICABLE));

        IndexMetaData watcherIndex = newTestIndexMeta(".watches", Settings.EMPTY);
        assertThat(check.actionRequired(watcherIndex, Collections.singletonMap("kibana_indices", ".kibana*")),
                equalTo(UpgradeActionRequired.UPGRADE));

        IndexMetaData watcherIndexWithAlias = newTestIndexMeta("my_watches", ".watches", Settings.EMPTY, "watch");
        assertThat(check.actionRequired(watcherIndexWithAlias, Collections.emptyMap()),
                equalTo(UpgradeActionRequired.UPGRADE));

        IndexMetaData watcherIndexWithAliasUpgraded = newTestIndexMeta("my_watches", ".watches", Settings.EMPTY, "doc");
        assertThat(check.actionRequired(watcherIndexWithAliasUpgraded, Collections.emptyMap()),
                equalTo(UpgradeActionRequired.UP_TO_DATE));
    }

    public static IndexMetaData newTestIndexMeta(String name, String alias, Settings indexSettings, String type) throws IOException {
        Settings build = Settings.builder().put(IndexMetaData.SETTING_VERSION_CREATED, Version.CURRENT)
                .put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, 1)
                .put(IndexMetaData.SETTING_NUMBER_OF_SHARDS, 1)
                .put(IndexMetaData.SETTING_CREATION_DATE, 1)
                .put(IndexMetaData.SETTING_INDEX_UUID, UUIDs.randomBase64UUID())
                .put(IndexMetaData.SETTING_VERSION_UPGRADED, Version.V_5_0_0_beta1)
                .put(indexSettings)
                .build();
        IndexMetaData.Builder builder = IndexMetaData.builder(name).settings(build);
        if (alias != null) {
            // Create alias
            builder.putAlias(AliasMetaData.newAliasMetaDataBuilder(alias).build());
        }
        if (type != null) {
            // Create fake type
            builder.putMapping(type, "{}");
        }
        return builder.build();
    }

    public static IndexMetaData newTestIndexMeta(String name, Settings indexSettings) throws IOException {
        return newTestIndexMeta(name, null, indexSettings, "foo");
    }

}
