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

package org.elasticsearch.example;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.example.realm.CustomAuthenticationFailureHandler;
import org.elasticsearch.example.realm.CustomRealm;
import org.elasticsearch.example.role.CustomInMemoryRolesProvider;
import org.elasticsearch.watcher.ResourceWatcherService;
import org.elasticsearch.xpack.security.authc.AuthenticationFailureHandler;
import org.elasticsearch.xpack.extensions.XPackExtension;
import org.elasticsearch.xpack.security.authc.Realm;
import org.elasticsearch.xpack.security.authz.RoleDescriptor;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import static org.elasticsearch.example.role.CustomInMemoryRolesProvider.ROLE_A;
import static org.elasticsearch.example.role.CustomInMemoryRolesProvider.ROLE_B;

/**
 * An example x-pack extension for testing custom realms and custom role providers.
 */
public class ExampleExtension extends XPackExtension {

    static {
        // check that the extension's policy works.
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            System.getSecurityManager().checkPrintJobAccess();
            return null;
        });
    }

    @Override
    public String name() {
        return "custom realm example";
    }

    @Override
    public String description() {
        return "a very basic implementation of a custom realm to validate it works";
    }

    @Override
    public Map<String, Realm.Factory> getRealms(ResourceWatcherService resourceWatcherService) {
        return Collections.singletonMap(CustomRealm.TYPE, CustomRealm::new);
    }

    @Override
    public AuthenticationFailureHandler getAuthenticationFailureHandler() {
        return new CustomAuthenticationFailureHandler();
    }

    @Override
    public Collection<String> getRestHeaders() {
        return Arrays.asList(CustomRealm.USER_HEADER, CustomRealm.PW_HEADER);
    }

    @Override
    public List<String> getSettingsFilter() {
        return Collections.singletonList("xpack.security.authc.realms.*.filtered_setting");
    }

    @Override
    public List<BiConsumer<Set<String>, ActionListener<Set<RoleDescriptor>>>>
    getRolesProviders(Settings settings, ResourceWatcherService resourceWatcherService) {
        CustomInMemoryRolesProvider rp1 = new CustomInMemoryRolesProvider(settings, Collections.singletonMap(ROLE_A, "read"));
        Map<String, String> roles = new HashMap<>();
        roles.put(ROLE_A, "all");
        roles.put(ROLE_B, "all");
        CustomInMemoryRolesProvider rp2 = new CustomInMemoryRolesProvider(settings, roles);
        return Arrays.asList(rp1, rp2);
    }
}
