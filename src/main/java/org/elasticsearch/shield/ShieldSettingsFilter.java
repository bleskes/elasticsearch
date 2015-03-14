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

package org.elasticsearch.shield;

import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.regex.Regex;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsFilter;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 *
 */
public class ShieldSettingsFilter extends AbstractComponent implements SettingsFilter.Filter {

    static final String HIDE_SETTINGS_SETTING = "shield.hide_settings";

    private final Set<String> removePatterns;

    @Inject
    public ShieldSettingsFilter(Settings settings, SettingsFilter settingsFilter) {
        super(settings);
        settingsFilter.addFilter(this);
        this.removePatterns = new CopyOnWriteArraySet<>();
        removePatterns.add(HIDE_SETTINGS_SETTING);
        Collections.addAll(removePatterns, settings.getAsArray(HIDE_SETTINGS_SETTING));
    }

    public void filterOut(String... patterns) {
        Collections.addAll(removePatterns, patterns);
    }

    @Override
    public void filter(ImmutableSettings.Builder settings) {
        for (Iterator<Map.Entry<String, String>> iter = settings.internalMap().entrySet().iterator(); iter.hasNext();) {
            Map.Entry<String, String> setting = iter.next();
            for (String regexp : removePatterns) {
                if (Regex.simpleMatch(regexp, setting.getKey())) {
                    iter.remove();
                }
            }
        }
    }
}
