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

package org.elasticsearch.watcher.actions.hipchat.service;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.watcher.support.template.TemplateEngine;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;

/**
 *
 */
public class HipChatMessage implements ToXContent {

    final String body;
    final @Nullable String[] rooms;
    final @Nullable String[] users;
    final @Nullable String from;
    final @Nullable Format format;
    final @Nullable Color color;
    final @Nullable Boolean notify;

    public HipChatMessage(String body, String[] rooms, String[] users, String from, Format format, Color color, Boolean notify) {
        this.body = body;
        this.rooms = rooms;
        this.users = users;
        this.from = from;
        this.format = format;
        this.color = color;
        this.notify = notify;
    }

    public String getBody() {
        return body;
    }

    public String[] getRooms() {
        return rooms;
    }

    @Nullable
    public String[] getUsers() {
        return users;
    }

    @Nullable
    public String getFrom() {
        return from;
    }

    @Nullable
    public Format getFormat() {
        return format;
    }

    @Nullable
    public Color getColor() {
        return color;
    }

    @Nullable
    public Boolean getNotify() {
        return notify;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HipChatMessage that = (HipChatMessage) o;

        if (!body.equals(that.body)) return false;
        if (!Arrays.equals(rooms, that.rooms)) return false;
        if (!Arrays.equals(users, that.users)) return false;
        if (from != null ? !from.equals(that.from) : that.from != null) return false;
        if (format != that.format) return false;
        if (color != that.color) return false;
        return !(notify != null ? !notify.equals(that.notify) : that.notify != null);
    }

    @Override
    public int hashCode() {
        int result = body.hashCode();
        result = 31 * result + (rooms != null ? Arrays.hashCode(rooms) : 0);
        result = 31 * result + (users != null ? Arrays.hashCode(users) : 0);
        result = 31 * result + (from != null ? from.hashCode() : 0);
        result = 31 * result + (format != null ? format.hashCode() : 0);
        result = 31 * result + (color != null ? color.hashCode() : 0);
        result = 31 * result + (notify != null ? notify.hashCode() : 0);
        return result;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return toXContent(builder, params, true);
    }

    public XContentBuilder toXContent(XContentBuilder builder, Params params, boolean includeTargets) throws IOException {
        builder.startObject();
        if (from != null) {
            builder.field(Field.FROM.getPreferredName(), from);
        }
        if (includeTargets) {
            if (rooms != null && rooms.length > 0) {
                builder.array(Field.ROOM.getPreferredName(), rooms);
            }
            if (users != null && users.length > 0) {
                builder.array(Field.USER.getPreferredName(), users);
            }
        }
        builder.field(Field.BODY.getPreferredName(), body);
        if (format != null) {
            builder.field(Field.FORMAT.getPreferredName(), format, params);
        }
        if (color != null) {
            builder.field(Field.COLOR.getPreferredName(), color, params);
        }
        if (notify != null) {
            builder.field(Field.NOTIFY.getPreferredName(), notify);
        }
        return builder.endObject();
    }

    public static class Template implements ToXContent {

        final org.elasticsearch.watcher.support.template.Template body;
        final @Nullable org.elasticsearch.watcher.support.template.Template[] rooms;
        final @Nullable org.elasticsearch.watcher.support.template.Template[] users;
        final @Nullable String from;
        final @Nullable Format format;
        final @Nullable org.elasticsearch.watcher.support.template.Template color;
        final @Nullable Boolean notify;

        public Template(org.elasticsearch.watcher.support.template.Template body,
                        org.elasticsearch.watcher.support.template.Template[] rooms,
                        org.elasticsearch.watcher.support.template.Template[] users,
                        String from,
                        Format format,
                        org.elasticsearch.watcher.support.template.Template color,
                        Boolean notify) {
            this.rooms = rooms;
            this.users = users;
            this.body = body;
            this.from = from;
            this.format = format;
            this.color = color;
            this.notify = notify;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Template template = (Template) o;

            if (!body.equals(template.body)) return false;
            if (!Arrays.equals(rooms, template.rooms)) return false;
            if (!Arrays.equals(users, template.users)) return false;
            if (from != null ? !from.equals(template.from) : template.from != null) return false;
            if (format != template.format) return false;
            if (color != null ? !color.equals(template.color) : template.color != null) return false;
            return !(notify != null ? !notify.equals(template.notify) : template.notify != null);
        }

        @Override
        public int hashCode() {
            int result = body.hashCode();
            result = 31 * result + (rooms != null ? Arrays.hashCode(rooms) : 0);
            result = 31 * result + (users != null ? Arrays.hashCode(users) : 0);
            result = 31 * result + (from != null ? from.hashCode() : 0);
            result = 31 * result + (format != null ? format.hashCode() : 0);
            result = 31 * result + (color != null ? color.hashCode() : 0);
            result = 31 * result + (notify != null ? notify.hashCode() : 0);
            return result;
        }

        public HipChatMessage render(TemplateEngine engine, Map<String, Object> model) {
            String body = engine.render(this.body, model);
            String[] rooms = null;
            if (this.rooms != null) {
                rooms = new String[this.rooms.length];
                for (int i = 0; i < this.rooms.length; i++) {
                    rooms[i] = engine.render(this.rooms[i], model);
                }
            }
            String[] users = null;
            if (this.users != null) {
                users = new String[this.users.length];
                for (int i = 0; i < this.users.length; i++) {
                    users[i] = engine.render(this.users[i], model);
                }
            }
            Color color = this.color == null ? null : Color.resolve(engine.render(this.color, model), null);
            return new HipChatMessage(body, rooms, users, from, format, color, notify);
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject();
            if (from != null) {
                builder.field(Field.FROM.getPreferredName(), from);
            }
            if (rooms != null && rooms.length > 0) {
                builder.startArray(Field.ROOM.getPreferredName());
                for (org.elasticsearch.watcher.support.template.Template room : rooms) {
                    room.toXContent(builder, params);
                }
                builder.endArray();
            }
            if (users != null && users.length > 0) {
                builder.startArray(Field.USER.getPreferredName());
                for (org.elasticsearch.watcher.support.template.Template user : users) {
                    user.toXContent(builder, params);
                }
                builder.endArray();
            }
            builder.field(Field.BODY.getPreferredName(), body, params);
            if (format != null) {
                builder.field(Field.FORMAT.getPreferredName(), format, params);
            }
            if (color != null) {
                builder.field(Field.COLOR.getPreferredName(), color, params);
            }
            if (notify != null) {
                builder.field(Field.NOTIFY.getPreferredName(), notify);
            }
            return builder.endObject();
        }

        public static Template parse(XContentParser parser) throws IOException {
            org.elasticsearch.watcher.support.template.Template body = null;
            org.elasticsearch.watcher.support.template.Template[] rooms = null;
            org.elasticsearch.watcher.support.template.Template[] users = null;
            String from = null;
            org.elasticsearch.watcher.support.template.Template color = null;
            Boolean notify = null;
            HipChatMessage.Format messageFormat = null;

            String currentFieldName = null;
            XContentParser.Token token;
            while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
                if (token == XContentParser.Token.FIELD_NAME) {
                    currentFieldName = parser.currentName();
                } else if (ParseFieldMatcher.STRICT.match(currentFieldName, Field.FROM)) {
                    from = parser.text();
                } else if (ParseFieldMatcher.STRICT.match(currentFieldName, Field.ROOM)) {
                    List<org.elasticsearch.watcher.support.template.Template> templates = new ArrayList<>();
                    if (token == XContentParser.Token.START_ARRAY) {
                        while ((token = parser.nextToken()) != XContentParser.Token.END_ARRAY) {
                            try {
                                templates.add(org.elasticsearch.watcher.support.template.Template.parse(parser));
                            } catch (ElasticsearchParseException epe) {
                                throw new ElasticsearchParseException("failed to parse hipchat message. failed to parse [{}] field", epe, Field.ROOM.getPreferredName());
                            }
                        }
                    } else {
                        try {
                            templates.add(org.elasticsearch.watcher.support.template.Template.parse(parser));
                        } catch (ElasticsearchParseException epe) {
                            throw new ElasticsearchParseException("failed to parse hipchat message. failed to parse [{}] field", epe, Field.ROOM.getPreferredName());
                        }
                    }
                    rooms = templates.toArray(new org.elasticsearch.watcher.support.template.Template[templates.size()]);
                } else if (ParseFieldMatcher.STRICT.match(currentFieldName, Field.USER)) {
                    List<org.elasticsearch.watcher.support.template.Template> templates = new ArrayList<>();
                    if (token == XContentParser.Token.START_ARRAY) {
                        while ((token = parser.nextToken()) != XContentParser.Token.END_ARRAY) {
                            try {
                                templates.add(org.elasticsearch.watcher.support.template.Template.parse(parser));
                            } catch (ElasticsearchParseException epe) {
                                throw new ElasticsearchParseException("failed to parse hipchat message. failed to parse [{}] field", epe, Field.USER.getPreferredName());
                            }
                        }
                    } else {
                        try {
                            templates.add(org.elasticsearch.watcher.support.template.Template.parse(parser));
                        } catch (ElasticsearchParseException epe) {
                            throw new ElasticsearchParseException("failed to parse hipchat message. failed to parse [{}] field", epe, Field.USER.getPreferredName());
                        }
                    }
                    users = templates.toArray(new org.elasticsearch.watcher.support.template.Template[templates.size()]);
                } else if (ParseFieldMatcher.STRICT.match(currentFieldName, Field.COLOR)) {
                    try {
                        color = org.elasticsearch.watcher.support.template.Template.parse(parser);
                    } catch (ElasticsearchParseException | IllegalArgumentException e) {
                        throw new ElasticsearchParseException("failed to parse hipchat message. failed to parse [{}] field", e, Field.COLOR.getPreferredName());
                    }
                } else if (ParseFieldMatcher.STRICT.match(currentFieldName, Field.NOTIFY)) {
                    if (token == XContentParser.Token.VALUE_BOOLEAN) {
                        notify = parser.booleanValue();
                    } else {
                        throw new ElasticsearchParseException("failed to parse hipchat message. failed to parse [{}] field, expected a boolean value but found [{}]",  Field.NOTIFY.getPreferredName(), token);
                    }
                } else if (ParseFieldMatcher.STRICT.match(currentFieldName, Field.BODY)) {
                    try {
                        body = org.elasticsearch.watcher.support.template.Template.parse(parser);
                    } catch (ElasticsearchParseException pe) {
                        throw new ElasticsearchParseException("failed to parse hipchat message. failed to parse [{}] field", pe, Field.BODY.getPreferredName());
                    }
                } else if (ParseFieldMatcher.STRICT.match(currentFieldName, Field.FORMAT)) {
                    try {
                        messageFormat = HipChatMessage.Format.parse(parser);
                    } catch (IllegalArgumentException ilae) {
                        throw new ElasticsearchParseException("failed to parse hipchat message. failed to parse [{}] field", ilae, Field.FORMAT.getPreferredName());
                    }
                } else {
                    throw new ElasticsearchParseException("failed to parse hipchat message. unexpected token [{}]", token);
                }
            }

            if (body == null) {
                throw new ElasticsearchParseException("failed to parse hipchat message. missing required [{}] field", Field.BODY.getPreferredName());
            }

            return new HipChatMessage.Template(body, rooms, users, from, messageFormat, color, notify);
        }

        public static class Builder {

            final org.elasticsearch.watcher.support.template.Template body;
            final List<org.elasticsearch.watcher.support.template.Template> rooms = new ArrayList<>();
            final List<org.elasticsearch.watcher.support.template.Template> users = new ArrayList<>();
            @Nullable String from;
            @Nullable Format format;
            @Nullable org.elasticsearch.watcher.support.template.Template color;
            @Nullable Boolean notify;

            public Builder(org.elasticsearch.watcher.support.template.Template body) {
                this.body = body;
            }

            public Builder addRooms(org.elasticsearch.watcher.support.template.Template... rooms) {
                this.rooms.addAll(Arrays.asList(rooms));
                return this;
            }

            public Builder addUsers(org.elasticsearch.watcher.support.template.Template... users) {
                this.users.addAll(Arrays.asList(users));
                return this;
            }

            public Builder setFrom(String from) {
                this.from = from;
                return this;
            }

            public Builder setFormat(Format format) {
                this.format = format;
                return this;
            }

            public Builder setColor(org.elasticsearch.watcher.support.template.Template color) {
                this.color = color;
                return this;
            }

            public Builder setNotify(boolean notify) {
                this.notify = notify;
                return this;
            }

            public Template build() {
                return new Template(
                        body,
                        rooms.isEmpty() ? null : rooms.toArray(new org.elasticsearch.watcher.support.template.Template[rooms.size()]),
                        users.isEmpty() ? null : users.toArray(new org.elasticsearch.watcher.support.template.Template[users.size()]),
                        from,
                        format,
                        color,
                        notify);
            }
        }
    }


    public enum Color implements ToXContent {
        YELLOW, GREEN, RED, PURPLE, GRAY, RANDOM;

        private final org.elasticsearch.watcher.support.template.Template template = org.elasticsearch.watcher.support.template.Template.inline(name()).build();

        public org.elasticsearch.watcher.support.template.Template asTemplate() {
            return template;
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            return builder.value(name().toLowerCase(Locale.ROOT));
        }

        public String value() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Color parse(XContentParser parser) throws IOException {
            return Color.valueOf(parser.text().toUpperCase(Locale.ROOT));
        }

        public static Color resolve(String value, Color defaultValue) {
            if (value == null) {
                return defaultValue;
            }
            return Color.valueOf(value.toUpperCase(Locale.ROOT));
        }

        public static Color resolve(Settings settings, String setting, Color defaultValue) {
            return resolve(settings.get(setting), defaultValue);
        }

        public static boolean validate(String value) {
            try {
                Color.valueOf(value.toUpperCase(Locale.ROOT));
                return true;
            } catch (IllegalArgumentException ilae) {
                return false;
            }
        }
    }

    /**
     *
     */
    public enum Format implements ToXContent {

        TEXT,
        HTML;

        private final org.elasticsearch.watcher.support.template.Template template = org.elasticsearch.watcher.support.template.Template.inline(name()).build();

        public org.elasticsearch.watcher.support.template.Template asTemplate() {
            return template;
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            return builder.value(name().toLowerCase(Locale.ROOT));
        }

        public String value() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Format parse(XContentParser parser) throws IOException {
            return Format.valueOf(parser.text().toUpperCase(Locale.ROOT));
        }

        public static Format resolve(String value, Format defaultValue) {
            if (value == null) {
                return defaultValue;
            }
            return Format.valueOf(value.toUpperCase(Locale.ROOT));
        }

        public static Format resolve(Settings settings, String setting, Format defaultValue) {
            return resolve(settings.get(setting), defaultValue);
        }

        public static boolean validate(String value) {
            try {
                Format.valueOf(value.toUpperCase(Locale.ROOT));
                return true;
            } catch (IllegalArgumentException ilae) {
                return false;
            }
        }
    }

    public interface Field {
        ParseField ROOM = new ParseField("room");
        ParseField USER = new ParseField("user");
        ParseField BODY = new ParseField("body");
        ParseField FROM = new ParseField("from");
        ParseField COLOR = new ParseField("color");
        ParseField NOTIFY = new ParseField("notify");
        ParseField FORMAT = new ParseField("format");
    }
}
