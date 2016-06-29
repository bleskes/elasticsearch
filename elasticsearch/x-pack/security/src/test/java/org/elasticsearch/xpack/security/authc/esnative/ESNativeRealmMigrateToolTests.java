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

package org.elasticsearch.shield.authc.esusers.tool;

import org.elasticsearch.cli.Command;
import org.elasticsearch.cli.CommandTestCase;
import org.elasticsearch.common.Strings;
import org.elasticsearch.xpack.security.authc.esnative.ESNativeRealmMigrateTool;
import org.elasticsearch.xpack.security.authz.RoleDescriptor;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;

/**
 * Unit tests for the {@code ESNativeRealmMigrateTool}
 */
public class ESNativeRealmMigrateToolTests extends CommandTestCase {

    @Override
    protected Command newCommand() {
        return new ESNativeRealmMigrateTool();
    }

    public void testUserJson() throws Exception {
        assertThat(ESNativeRealmMigrateTool.MigrateUserOrRoles.createUserJson(Strings.EMPTY_ARRAY, "hash".toCharArray()),
                equalTo("{\"password_hash\":\"hash\",\"roles\":[]}"));
        assertThat(ESNativeRealmMigrateTool.MigrateUserOrRoles.createUserJson(new String[]{"role1", "role2"}, "hash".toCharArray()),
                equalTo("{\"password_hash\":\"hash\",\"roles\":[\"role1\",\"role2\"]}"));
    }

    public void testRoleJson() throws Exception {
        RoleDescriptor.IndicesPrivileges ip = RoleDescriptor.IndicesPrivileges.builder()
                .indices(new String[]{"i1", "i2", "i3"})
                .privileges(new String[]{"all"})
                .fields(new String[]{"body"})
                .build();
        RoleDescriptor.IndicesPrivileges[] ips = new RoleDescriptor.IndicesPrivileges[1];
        ips[0] = ip;
        String[] cluster = Strings.EMPTY_ARRAY;
        String[] runAs = Strings.EMPTY_ARRAY;
        RoleDescriptor rd = new RoleDescriptor("rolename", cluster, ips, runAs);
        assertThat(ESNativeRealmMigrateTool.MigrateUserOrRoles.createRoleJson(rd),
                equalTo("{\"cluster\":[],\"indices\":[{\"names\":[\"i1\",\"i2\",\"i3\"]," +
                                "\"privileges\":[\"all\"],\"fields\":[\"body\"]}],\"run_as\":[]}"));

    }

}
