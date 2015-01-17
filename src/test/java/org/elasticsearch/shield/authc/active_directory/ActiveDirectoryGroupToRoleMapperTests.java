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

package org.elasticsearch.shield.authc.active_directory;

import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.shield.authc.RealmConfig;
import org.elasticsearch.shield.authc.support.ldap.AbstractGroupToRoleMapper;
import org.elasticsearch.shield.authc.support.ldap.AbstractGroupToRoleMapperTests;
import org.elasticsearch.watcher.ResourceWatcherService;

import java.nio.file.Path;

/**
 *
 */
public class ActiveDirectoryGroupToRoleMapperTests extends AbstractGroupToRoleMapperTests {

    @Override
    protected AbstractGroupToRoleMapper createMapper(Path file, ResourceWatcherService watcherService) {
        Settings adSettings = ImmutableSettings.builder()
                .put("files.role_mapping", file.toAbsolutePath())
                .build();
        RealmConfig config = new RealmConfig("ad-group-mapper-test", adSettings, settings, env);
        return new ActiveDirectoryGroupToRoleMapper(config, watcherService);
    }

}
