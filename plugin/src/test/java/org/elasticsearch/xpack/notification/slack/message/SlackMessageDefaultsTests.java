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

package org.elasticsearch.xpack.notification.slack.message;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.test.ESTestCase;

import static org.hamcrest.Matchers.is;

/**
 *
 */
public class SlackMessageDefaultsTests extends ESTestCase {
    public void testConstructor() throws Exception {
        Settings settings = randomSettings();
        SlackMessageDefaults defaults = new SlackMessageDefaults(settings);
        assertThat(defaults.from, is(settings.get("from", null)));
        assertThat(defaults.to, is(settings.getAsArray("to", null)));
        assertThat(defaults.text, is(settings.get("text", null)));
        assertThat(defaults.icon, is(settings.get("icon", null)));
        assertThat(defaults.attachment.fallback, is(settings.get("attachment.fallback", null)));
        assertThat(defaults.attachment.color, is(settings.get("attachment.color", null)));
        assertThat(defaults.attachment.pretext, is(settings.get("attachment.pretext", null)));
        assertThat(defaults.attachment.authorName, is(settings.get("attachment.author_name", null)));
        assertThat(defaults.attachment.authorLink, is(settings.get("attachment.author_link", null)));
        assertThat(defaults.attachment.authorIcon, is(settings.get("attachment.author_icon", null)));
        assertThat(defaults.attachment.title, is(settings.get("attachment.title", null)));
        assertThat(defaults.attachment.titleLink, is(settings.get("attachment.title_link", null)));
        assertThat(defaults.attachment.text, is(settings.get("attachment.text", null)));
        assertThat(defaults.attachment.imageUrl, is(settings.get("attachment.image_url", null)));
        assertThat(defaults.attachment.thumbUrl, is(settings.get("attachment.thumb_url", null)));
        assertThat(defaults.attachment.field.title, is(settings.get("attachment.field.title", null)));
        assertThat(defaults.attachment.field.value, is(settings.get("attachment.field.value", null)));
        assertThat(defaults.attachment.field.isShort, is(settings.getAsBoolean("attachment.field.short", null)));
    }

    public static Settings randomSettings() {
        Settings.Builder settings = Settings.builder();
        if (randomBoolean()) {
            settings.put("from", randomAsciiOfLength(10));
        }
        if (randomBoolean()) {
            String[] to = new String[randomIntBetween(1, 3)];
            for (int i = 0; i < to.length; i++) {
                to[i] = randomAsciiOfLength(10);
            }
            settings.putArray("to", to);
        }
        if (randomBoolean()) {
            settings.put("text", randomAsciiOfLength(10));
        }
        if (randomBoolean()) {
            settings.put("icon", randomAsciiOfLength(10));
        }
        if (randomBoolean()) {
            settings.put("attachment.fallback", randomAsciiOfLength(10));
        }
        if (randomBoolean()) {
            settings.put("attachment.color", randomAsciiOfLength(10));
        }
        if (randomBoolean()) {
            settings.put("attachment.pretext", randomAsciiOfLength(10));
        }
        if (randomBoolean()) {
            settings.put("attachment.author_name", randomAsciiOfLength(10));
        }
        if (randomBoolean()) {
            settings.put("attachment.author_link", randomAsciiOfLength(10));
        }
        if (randomBoolean()) {
            settings.put("attachment.author_icon", randomAsciiOfLength(10));
        }
        if (randomBoolean()) {
            settings.put("attachment.title", randomAsciiOfLength(10));
        }
        if (randomBoolean()) {
            settings.put("attachment.title_link", randomAsciiOfLength(10));
        }
        if (randomBoolean()) {
            settings.put("attachment.text", randomAsciiOfLength(10));
        }
        if (randomBoolean()) {
            settings.put("attachment.image_url", randomAsciiOfLength(10));
        }
        if (randomBoolean()) {
            settings.put("attachment.thumb_url", randomAsciiOfLength(10));
        }
        if (randomBoolean()) {
            settings.put("attachment.field.title", randomAsciiOfLength(10));
        }
        if (randomBoolean()) {
            settings.put("attachment.field.value", randomAsciiOfLength(10));
        }
        if (randomBoolean()) {
            settings.put("attachment.field.short", randomBoolean());
        }
        return settings.build();
    }
}
