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

package org.elasticsearch.xpack.extensions;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.elasticsearch.watcher.ResourceWatcherService;
import org.elasticsearch.xpack.security.authc.AuthenticationFailureHandler;
import org.elasticsearch.xpack.security.authc.Realm;


/**
 * An extension point allowing to plug in custom functionality in x-pack authentication module.
 */
public abstract class XPackExtension {
    /**
     * The name of the plugin.
     */
    public abstract String name();

    /**
     * The description of the plugin.
     */
    public abstract String description();

    /**
     * Returns headers which should be copied from REST requests to internal cluster requests.
     */
    public Collection<String> getRestHeaders() {
        return Collections.emptyList();
    }

    /**
     * Returns authentication realm implementations added by this extension.
     *
     * The key of the returned {@link Map} is the type name of the realm, and the value
     * is a {@link org.elasticsearch.xpack.security.authc.Realm.Factory} which will construct
     * that realm for use in authentication when that realm type is configured.
     *
     * @param resourceWatcherService Use to watch configuration files for changes
     */
    public Map<String, Realm.Factory> getRealms(ResourceWatcherService resourceWatcherService) {
        return Collections.emptyMap();
    }

    /**
     * Returns a handler for authentication failures, or null to use the default handler.
     *
     * Only one installed extension may have an authentication failure handler. If more than
     * one extension returns a non-null handler, an error is raised.
     */
    public AuthenticationFailureHandler getAuthenticationFailureHandler() {
        return null;
    }

    /**
     * Returns a list of settings that should be filtered from API calls. In most cases,
     * these settings are sensitive such as passwords.
     *
     * The value should be the full name of the setting or a wildcard that matches the
     * desired setting.
     */
    public List<String> getSettingsFilter() {
        return Collections.emptyList();
    }
}
