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
import java.util.Map;

import org.elasticsearch.watcher.ResourceWatcherService;
import org.elasticsearch.xpack.security.authc.AuthenticationFailureHandler;
import org.elasticsearch.xpack.security.authc.AuthenticationModule;
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
     * Implement this function to register custom extensions in the authentication module.
     */
    public void onModule(AuthenticationModule module) {}

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
     */
    public Map<String, Realm.Factory> getRealms() {
        return Collections.emptyMap();
    }
}
