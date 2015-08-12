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

package org.elasticsearch.marvel.agent.settings;

import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.test.ESTestCase;
import org.junit.Test;

import static org.elasticsearch.common.settings.Settings.settingsBuilder;
import static org.hamcrest.Matchers.equalTo;

public class MarvelSettingsServiceTests extends ESTestCase {

    @Test
    public void testMarvelSettingService() {
        MarvelSettingsService service = new MarvelSettingsService(Settings.EMPTY);

        TimeValue indexStatsTimeout = service.indexStatsTimeout();
        assertNotNull(indexStatsTimeout);

        String[] indices = service.indices();
        assertNotNull(indices);

        TimeValue updatedIndexStatsTimeout = TimeValue.timeValueSeconds(60L);
        String[] updatedIndices = new String[]{"index-0", "index-1"};

        Settings settings = settingsBuilder()
                .put(service.indexStatsTimeout.getName(), updatedIndexStatsTimeout)
                .put(service.indices.getName(), Strings.arrayToCommaDelimitedString(updatedIndices))
                .build();

        service.onRefreshSettings(settings);

        assertThat(service.indexStatsTimeout(), equalTo(updatedIndexStatsTimeout));
        assertArrayEquals(service.indices(), updatedIndices);
    }
}
