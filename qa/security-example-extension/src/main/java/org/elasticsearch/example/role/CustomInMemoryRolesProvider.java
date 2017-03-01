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

package org.elasticsearch.example.role;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.xpack.security.authz.RoleDescriptor;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * A custom roles provider implementation for testing that serves
 * static roles from memory.
 */
public class CustomInMemoryRolesProvider
    extends AbstractComponent
    implements BiConsumer<Set<String>, ActionListener<Set<RoleDescriptor>>> {

    public static final String INDEX = "foo";
    public static final String ROLE_A = "roleA";
    public static final String ROLE_B = "roleB";

    private final Map<String, String> rolePermissionSettings;

    public CustomInMemoryRolesProvider(Settings settings, Map<String, String> rolePermissionSettings) {
        super(settings);
        this.rolePermissionSettings = rolePermissionSettings;
    }

    @Override
    public void accept(Set<String> roles, ActionListener<Set<RoleDescriptor>> listener) {
        Set<RoleDescriptor> roleDescriptors = new HashSet<>();
        for (String role : roles) {
            if (rolePermissionSettings.containsKey(role)) {
                roleDescriptors.add(
                new RoleDescriptor(role, new String[] { "all" },
                    new RoleDescriptor.IndicesPrivileges[] {
                        RoleDescriptor.IndicesPrivileges.builder()
                            .privileges(rolePermissionSettings.get(role))
                            .indices(INDEX)
                            .grantedFields("*")
                            .build()
                    }, null)
                );
            }
        }

        listener.onResponse(roleDescriptors);
    }
}
