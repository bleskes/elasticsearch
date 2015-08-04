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
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.test.ESTestCase;
import org.junit.Test;

import static org.elasticsearch.common.settings.Settings.settingsBuilder;
import static org.hamcrest.Matchers.equalTo;

public class MarvelSettingTests extends ESTestCase {

    @Test
    public void testBooleanMarvelSetting() {
        String name = randomAsciiOfLength(10);
        String description = randomAsciiOfLength(20);
        Boolean defaultValue = null;
        if (randomBoolean()) {
            defaultValue = randomBoolean();
        }

        MarvelSetting.BooleanSetting setting = MarvelSetting.booleanSetting(name, defaultValue, description);
        assertThat(setting.getName(), equalTo(name));
        assertThat(setting.getDescription(), equalTo(description));
        assertThat(setting.getDefaultValue(), equalTo(defaultValue));

        setting.onInit(settingsBuilder().build());
        assertThat(setting.getValue(), equalTo(defaultValue));

        setting.onInit(settingsBuilder().put(name, Boolean.FALSE).build());
        assertFalse(setting.getValue());

        setting.onRefresh(settingsBuilder().put(name, Boolean.TRUE).build());
        assertTrue(setting.getValue());
    }

    @Test
    public void testTimeValueMarvelSetting() {
        String name = randomAsciiOfLength(10);
        String description = randomAsciiOfLength(20);
        TimeValue defaultValue = null;
        if (randomBoolean()) {
            defaultValue = randomTimeValue();
        }

        MarvelSetting.TimeValueSetting setting = MarvelSetting.timeSetting(name, defaultValue, description);
        assertThat(setting.getName(), equalTo(name));
        assertThat(setting.getDescription(), equalTo(description));
        if (defaultValue == null) {
            assertNull(setting.getDefaultValue());
        } else {
            assertThat(setting.getDefaultValue().millis(), equalTo(defaultValue.millis()));
        }

        setting.onInit(settingsBuilder().build());
        if (defaultValue == null) {
            assertNull(setting.getValue());
        } else {
            assertThat(setting.getValue().millis(), equalTo(defaultValue.millis()));
        }

        setting.onInit(settingsBuilder().put(name, 15000L).build());
        assertThat(setting.getValue().millis(), equalTo(15000L));

        TimeValue updated = randomTimeValue();
        setting.onInit(settingsBuilder().put(name, updated.toString()).build());
        assertThat(setting.getValue().millis(), equalTo(updated.millis()));

        updated = randomTimeValue();
        setting.onRefresh(settingsBuilder().put(name, updated.toString()).build());
        assertThat(setting.getValue().millis(), equalTo(updated.millis()));
    }

    @Test
    public void testStringMarvelSetting() {
        String name = randomAsciiOfLength(10);
        String description = randomAsciiOfLength(20);
        String defaultValue = null;
        if (randomBoolean()) {
            defaultValue = randomAsciiOfLength(15);
        }

        MarvelSetting.StringSetting setting = MarvelSetting.stringSetting(name, defaultValue, description);
        assertThat(setting.getName(), equalTo(name));
        assertThat(setting.getDescription(), equalTo(description));
        assertThat(setting.getDefaultValue(), equalTo(defaultValue));

        setting.onInit(settingsBuilder().build());
        assertThat(setting.getValue(), equalTo(defaultValue));

        String updated = randomAsciiOfLength(15);
        setting.onInit(settingsBuilder().put(name, updated).build());
        assertThat(setting.getValue(), equalTo(updated));

        updated = randomAsciiOfLength(15);
        setting.onRefresh(settingsBuilder().put(name, updated).build());
        assertThat(setting.getValue(), equalTo(updated));
    }

    @Test
    public void testStringArrayMarvelSetting() {
        String name = randomAsciiOfLength(10);
        String description = randomAsciiOfLength(20);
        String[] defaultValue = null;
        if (randomBoolean()) {
            defaultValue = randomStringArray();
        }

        MarvelSetting.StringArraySetting setting = MarvelSetting.arraySetting(name, defaultValue, description);
        assertThat(setting.getName(), equalTo(name));
        assertThat(setting.getDescription(), equalTo(description));
        if (defaultValue == null) {
            assertNull(setting.getDefaultValue());
        } else {
            assertArrayEquals(setting.getDefaultValue(), defaultValue);
        }

        setting.onInit(settingsBuilder().build());
        if (defaultValue == null) {
            assertArrayEquals(setting.getValue(), Strings.EMPTY_ARRAY);
        } else {
            assertArrayEquals(setting.getValue(), defaultValue);
        }

        String[] updated = randomStringArray();
        setting.onInit(settingsBuilder().put(name, Strings.arrayToCommaDelimitedString(updated)).build());
        assertArrayEquals(setting.getValue(), updated);

        updated = randomStringArray();
        setting.onRefresh(settingsBuilder().put(name, Strings.arrayToCommaDelimitedString(updated)).build());
        assertArrayEquals(setting.getValue(), updated);
    }

    private TimeValue randomTimeValue() {
        return TimeValue.parseTimeValue(randomFrom("10ms", "1.5s", "1.5m", "1.5h", "1.5d", "1000d"), null, getClass().getSimpleName() + ".unit");
    }

    private String[] randomStringArray() {
        int n = randomIntBetween(1, 5);
        String[] values = new String[n];
        for (int i = 0; i < n; i++) {
            values[i] = randomAsciiOfLength(5);
        }
        return values;
    }
}
