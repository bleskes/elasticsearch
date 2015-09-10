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

import org.elasticsearch.Version;
import org.elasticsearch.action.search.SearchAction;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.set.Sets;
import org.elasticsearch.shield.authz.Permission;
import org.elasticsearch.shield.authz.Privilege;
import org.elasticsearch.test.ESTestCase;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;

public class IndicesPermissionTests extends ESTestCase {

    public void testAuthorize() {
        IndexMetaData.Builder imbBuilder = IndexMetaData.builder("_index")
                .settings(Settings.builder()
                                .put(IndexMetaData.SETTING_NUMBER_OF_SHARDS, 1)
                                .put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, 1)
                                .put(IndexMetaData.SETTING_VERSION_CREATED, Version.CURRENT)
                )
                .putAlias(AliasMetaData.builder("_alias"));
        MetaData md = MetaData.builder().put(imbBuilder).build();

        // basics:
        BytesReference query = new BytesArray("{}");
        List<String> fields = Arrays.asList("_field");
        Permission.Global.Role role = Permission.Global.Role.builder("_role").add(fields, query, Privilege.Index.ALL, "_index").build();
        IndicesAccessControl permissions = role.authorize(SearchAction.NAME, Sets.newHashSet("_index"), md);
        assertThat(permissions.getIndexPermissions("_index"), notNullValue());
        assertThat(permissions.getIndexPermissions("_index").getFields().size(), equalTo(1));
        assertThat(permissions.getIndexPermissions("_index").getFields().iterator().next(), equalTo("_field"));
        assertThat(permissions.getIndexPermissions("_index").getQueries().size(), equalTo(1));
        assertThat(permissions.getIndexPermissions("_index").getQueries().iterator().next(), equalTo(query));

        // no document level security:
        role = Permission.Global.Role.builder("_role").add(fields, null, Privilege.Index.ALL, "_index").build();
        permissions = role.authorize(SearchAction.NAME, Sets.newHashSet("_index"), md);
        assertThat(permissions.getIndexPermissions("_index"), notNullValue());
        assertThat(permissions.getIndexPermissions("_index").getFields().size(), equalTo(1));
        assertThat(permissions.getIndexPermissions("_index").getFields().iterator().next(), equalTo("_field"));
        assertThat(permissions.getIndexPermissions("_index").getQueries(), nullValue());

        // no field level security:
        role = Permission.Global.Role.builder("_role").add(null, query, Privilege.Index.ALL, "_index").build();
        permissions = role.authorize(SearchAction.NAME, Sets.newHashSet("_index"), md);
        assertThat(permissions.getIndexPermissions("_index"), notNullValue());
        assertThat(permissions.getIndexPermissions("_index").getFields(), nullValue());
        assertThat(permissions.getIndexPermissions("_index").getQueries().size(), equalTo(1));
        assertThat(permissions.getIndexPermissions("_index").getQueries().iterator().next(), equalTo(query));

        // index group associated with an alias:
        role = Permission.Global.Role.builder("_role").add(fields, query, Privilege.Index.ALL, "_alias").build();
        permissions = role.authorize(SearchAction.NAME, Sets.newHashSet("_alias"), md);
        assertThat(permissions.getIndexPermissions("_index"), notNullValue());
        assertThat(permissions.getIndexPermissions("_index").getFields().size(), equalTo(1));
        assertThat(permissions.getIndexPermissions("_index").getFields().iterator().next(), equalTo("_field"));
        assertThat(permissions.getIndexPermissions("_index").getQueries().size(), equalTo(1));
        assertThat(permissions.getIndexPermissions("_index").getQueries().iterator().next(), equalTo(query));
    }

}
