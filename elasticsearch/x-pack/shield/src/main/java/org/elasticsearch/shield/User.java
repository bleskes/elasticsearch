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

import org.elasticsearch.ElasticsearchSecurityException;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.inject.internal.Nullable;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * An authenticated user
 */
public class User implements ToXContent {

    private final String username;
    private final String[] roles;
    private final User runAs;
    private final Map<String, Object> metadata;

    private final @Nullable String fullName;
    private final @Nullable String email;

    public User(String username, String... roles) {
        this(username, roles, null, null, null);
    }

    public User(String username, String[] roles, User runAs) {
        this(username, roles, null, null, null, runAs);
    }

    public User(String username, String[] roles, String fullName, String email, Map<String, Object> metadata) {
        this.username = username;
        this.roles = roles == null ? Strings.EMPTY_ARRAY : roles;
        this.metadata = metadata != null ? Collections.unmodifiableMap(metadata) : Collections.emptyMap();
        this.fullName = fullName;
        this.email = email;
        this.runAs = null;
    }

    public User(String username, String[] roles, String fullName, String email, Map<String, Object> metadata, User runAs) {
        this.username = username;
        this.roles = roles == null ? Strings.EMPTY_ARRAY : roles;
        this.metadata = metadata != null ? Collections.unmodifiableMap(metadata) : Collections.emptyMap();
        this.fullName = fullName;
        this.email = email;
        assert (runAs == null || runAs.runAs() == null) : "the run_as user should not be a user that can run as";
        if (runAs == SystemUser.INSTANCE) {
            throw new ElasticsearchSecurityException("invalid run_as user");
        }
        this.runAs = runAs;
    }

    /**
     * @return  The principal of this user - effectively serving as the
     *          unique identity of of the user.
     */
    public String principal() {
        return this.username;
    }

    /**
     * @return  The roles this user is associated with. The roles are
     *          identified by their unique names and each represents as
     *          set of permissions
     */
    public String[] roles() {
        return this.roles;
    }

    /**
     * @return  The metadata that is associated with this user. Can never be {@code null}.
     */
    public Map<String, Object> metadata() {
        return metadata;
    }

    /**
     * @return  The full name of this user. May be {@code null}.
     */
    public String fullName() {
        return fullName;
    }

    /**
     * @return  The email of this user. May be {@code null}.
     */
    public String email() {
        return email;
    }

    /**
     * @return The user that will be used for run as functionality. If run as
     *         functionality is not being used, then <code>null</code> will be
     *         returned
     */
    public User runAs() {
        return runAs;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("User[username=").append(username);
        sb.append(",roles=[").append(Strings.arrayToCommaDelimitedString(roles)).append("]");
        sb.append(",fullName=").append(fullName);
        sb.append(",email=").append(email);
        sb.append(",metadata=");
        append(sb, metadata);
        if (runAs != null) {
            sb.append(",runAs=[").append(runAs.toString()).append("]");
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

        if (!username.equals(user.username)) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(roles, user.roles)) return false;
        if (runAs != null ? !runAs.equals(user.runAs) : user.runAs != null) return false;
        if (!metadata.equals(user.metadata)) return false;
        if (fullName != null ? !fullName.equals(user.fullName) : user.fullName != null) return false;
        return !(email != null ? !email.equals(user.email) : user.email != null);

    }

    @Override
    public int hashCode() {
        int result = username.hashCode();
        result = 31 * result + Arrays.hashCode(roles);
        result = 31 * result + (runAs != null ? runAs.hashCode() : 0);
        result = 31 * result + metadata.hashCode();
        result = 31 * result + (fullName != null ? fullName.hashCode() : 0);
        result = 31 * result + (email != null ? email.hashCode() : 0);
        return result;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(Fields.USERNAME.getPreferredName(), principal());
        builder.array(Fields.ROLES.getPreferredName(), roles());
        builder.field(Fields.FULL_NAME.getPreferredName(), fullName);
        builder.field(Fields.EMAIL.getPreferredName(), email);
        builder.field(Fields.METADATA.getPreferredName(), metadata);
        builder.endObject();
        return builder;
    }

    public static User readFrom(StreamInput input) throws IOException {
        if (input.readBoolean()) {
            String name = input.readString();
            switch (name) {
                case SystemUser.NAME:
                    return SystemUser.INSTANCE;
                case XPackUser.NAME:
                    return XPackUser.INSTANCE;
                default:
                    throw new IllegalStateException("invalid internal user");
            }
        }
        String username = input.readString();
        String[] roles = input.readStringArray();
        Map<String, Object> metadata = input.readMap();
        String fullName = input.readOptionalString();
        String email = input.readOptionalString();
        if (input.readBoolean()) {
            String runAsUsername = input.readString();
            String[] runAsRoles = input.readStringArray();
            Map<String, Object> runAsMetadata = input.readMap();
            String runAsFullName = input.readOptionalString();
            String runAsEmail = input.readOptionalString();
            User runAs = new User(runAsUsername, runAsRoles, runAsFullName, runAsEmail, runAsMetadata);
            return new User(username, roles, fullName, email, metadata, runAs);
        }
        return new User(username, roles, fullName, email, metadata);
    }

    public static void writeTo(User user, StreamOutput output) throws IOException {
        if (SystemUser.is(user)) {
            output.writeBoolean(true);
            output.writeString(SystemUser.NAME);
        } if (XPackUser.is(user)) {
            output.writeBoolean(true);
            output.writeString(XPackUser.NAME);
        } else {
            output.writeBoolean(false);
            output.writeString(user.username);
            output.writeStringArray(user.roles);
            output.writeMap(user.metadata);
            output.writeOptionalString(user.fullName);
            output.writeOptionalString(user.email);
            if (user.runAs == null) {
                output.writeBoolean(false);
            } else {
                output.writeBoolean(true);
                output.writeString(user.runAs.username);
                output.writeStringArray(user.runAs.roles);
                output.writeMap(user.runAs.metadata);
                output.writeOptionalString(user.runAs.fullName);
                output.writeOptionalString(user.runAs.email);
            }
        }
    }

    public static void append(StringBuilder sb, Object object) {
        if (object == null) {
            sb.append((Object) null);
        }
        if (object instanceof Map) {
            sb.append("{");
            for (Map.Entry<String, Object> entry : ((Map<String, Object>)object).entrySet()) {
                sb.append(entry.getKey()).append("=");
                append(sb, entry.getValue());
            }
            sb.append("}");

        } else if (object instanceof Collection) {
            sb.append("[");
            boolean first = true;
            for (Object item : (Collection) object) {
                if (!first) {
                    sb.append(",");
                }
                append(sb, item);
                first = false;
            }
            sb.append("]");
        } else if (object.getClass().isArray()) {
            sb.append("[");
            for (int i = 0; i < Array.getLength(object); i++) {
                if (i != 0) {
                    sb.append(",");
                }
                append(sb, Array.get(object, i));
            }
            sb.append("]");
        } else {
            sb.append(object);
        }
    }

    public interface Fields {
        ParseField USERNAME = new ParseField("username");
        ParseField PASSWORD = new ParseField("password");
        ParseField ROLES = new ParseField("roles");
        ParseField FULL_NAME = new ParseField("full_name");
        ParseField EMAIL = new ParseField("email");
        ParseField METADATA = new ParseField("metadata");
    }
}
