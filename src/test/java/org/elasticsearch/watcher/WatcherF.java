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

package org.elasticsearch.watcher;

import org.elasticsearch.bootstrap.ElasticsearchF;
import org.elasticsearch.license.plugin.LicensePlugin;

/**
 * Main class to easily run Watcher from a IDE.
 * It sets all the options to run the Watcher plugin and access it from Sense, but doesn't run with Shield.
 *
 * During startup an error will be printed that the config directory can't be found, to fix this:
 * 1) Add a config directly to the top level project directory
 * 2) or set `-Des.path.home=` to a location where there is a config directory on your machine.
 */
public class WatcherF {

    public static void main(String[] args) {
        System.setProperty("es.http.cors.enabled", "true");
        System.setProperty("es.script.inline", "on");
        System.setProperty("es.shield.enabled", "false");
        System.setProperty("es.security.manager.enabled", "false");
        System.setProperty("es.plugins.load_classpath_plugins", "false");
        System.setProperty("es.plugin.types", WatcherPlugin.class.getName() + "," + LicensePlugin.class.getName());
        System.setProperty("es.cluster.name", WatcherF.class.getSimpleName());

        ElasticsearchF.main(args);
    }

}
