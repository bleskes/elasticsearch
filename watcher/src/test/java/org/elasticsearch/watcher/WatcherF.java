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

import org.elasticsearch.bootstrap.Elasticsearch;
import org.elasticsearch.license.plugin.LicensePlugin;

/**
 * Main class to easily run Watcher from a IDE.
 * It sets all the options to run the Watcher plugin and access it from Sense, but doesn't run with Shield.
 *
 * In order to run this class set configure the following:
 * 1) Set `-Des.path.home=` to a directory containing an ES config directory
 */
public class WatcherF {

    public static void main(String[] args) throws Throwable {
        System.setProperty("es.http.cors.enabled", "true");
        System.setProperty("es.http.cors.allow-origin", "*");
        System.setProperty("es.script.inline", "on");
        System.setProperty("es.shield.enabled", "false");
        System.setProperty("es.security.manager.enabled", "false");
        System.setProperty("es.plugins.load_classpath_plugins", "false");

        // this is for the `test-watcher-integration` group level integration in HipChat
        System.setProperty("es.watcher.actions.hipchat.service.account.integration.profile", "integration");
        System.setProperty("es.watcher.actions.hipchat.service.account.integration.auth_token", "huuS9v7ccuOy3ZBWWWr1vt8Lqu3sQnLUE81nrLZU");
        System.setProperty("es.watcher.actions.hipchat.service.account.integration.room", "test-watcher");

        // this is for the Watcher Test account in HipChat
        System.setProperty("es.watcher.actions.hipchat.service.account.user.profile", "user");
        System.setProperty("es.watcher.actions.hipchat.service.account.user.auth_token", "FYVx16oDH78ZW9r13wtXbcszyoyA7oX5tiMWg9X0");

        // this is for the `test-watcher-v1` notification token
        System.setProperty("es.watcher.actions.hipchat.service.account.v1.profile", "v1");
        System.setProperty("es.watcher.actions.hipchat.service.account.v1.auth_token", "a734baf62df618b96dda55b323fc30");



        System.setProperty("es.plugin.types", WatcherPlugin.class.getName() + "," + LicensePlugin.class.getName());
        System.setProperty("es.cluster.name", WatcherF.class.getSimpleName());

        Elasticsearch.main(new String[]{"start"});
    }

}
