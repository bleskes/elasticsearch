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

package org.elasticsearch.xpack.security.authc;

import org.elasticsearch.Version;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.xpack.XPackSettings;
import org.elasticsearch.xpack.security.crypto.CryptoService;
import org.elasticsearch.xpack.security.user.User;

import java.io.IOException;
import java.util.Base64;
import java.util.Objects;

public class Authentication {

    public static final String AUTHENTICATION_KEY = "_xpack_security_authentication";

    private final User user;
    private final RealmRef authenticatedBy;
    private final RealmRef lookedUpBy;
    private final Version version;

    public Authentication(User user, RealmRef authenticatedBy, RealmRef lookedUpBy) {
        this(user, authenticatedBy, lookedUpBy, Version.CURRENT);
    }

    public Authentication(User user, RealmRef authenticatedBy, RealmRef lookedUpBy, Version version) {
        this.user = Objects.requireNonNull(user);
        this.authenticatedBy = Objects.requireNonNull(authenticatedBy);
        this.lookedUpBy = lookedUpBy;
        this.version = version;
    }

    public Authentication(StreamInput in) throws IOException {
        this.user = User.readFrom(in);
        this.authenticatedBy = new RealmRef(in);
        if (in.readBoolean()) {
            this.lookedUpBy = new RealmRef(in);
        } else {
            this.lookedUpBy = null;
        }
        this.version = in.getVersion();
    }

    public User getUser() {
        return user;
    }

    // TODO remove run as from the User object...
    public User getRunAsUser() {
        if (user.runAs() != null) {
            return user.runAs();
        }
        return user;
    }

    /**
     * returns true if this authentication represents a authentication object with a authenticated user that is different than the user the
     * request should be run as
     */
    public boolean isRunAs() {
        return getUser().equals(getRunAsUser()) == false;
    }

    public RealmRef getAuthenticatedBy() {
        return authenticatedBy;
    }

    public RealmRef getLookedUpBy() {
        return lookedUpBy;
    }

    public Version getVersion() {
        return version;
    }

    /**
     * Reads the Authentication from the context. If the Authentication was created and placed as a transient in the current
     * {@link ThreadContext} then we can read the transient directly and return it. If not, we read the authentication header and
     * deserialize it using the {@link Version} to ensure we deserialize it properly
     */
    public static Authentication readFromContext(ThreadContext ctx, CryptoService cryptoService, Settings settings, Version version,
                                                 boolean signingEnabled) throws IOException, IllegalArgumentException {
        Authentication authentication = ctx.getTransient(AUTHENTICATION_KEY);
        if (authentication != null) {
            assert ctx.getHeader(AUTHENTICATION_KEY) != null;
            return authentication;
        }

        String authenticationHeader = ctx.getHeader(AUTHENTICATION_KEY);
        if (authenticationHeader == null) {
            return null;
        }
        final boolean shouldSign = shouldSign(settings, version, signingEnabled);
        return deserializeHeaderAndPutInContext(authenticationHeader, ctx, cryptoService, shouldSign, version);
    }

    public static Authentication getAuthentication(ThreadContext context) {
        return context.getTransient(Authentication.AUTHENTICATION_KEY);
    }

    /**
     * Reads the Authentication object from the header and places the deserialized object in the ThreadContext. The header may be signed if
     * the message is sent from a node without ssl or a version before 5.4.0. The version is used by the CryptoService to properly read
     * the signed string.
     *
     * While we serialize the version out in the bytes of this object, we do not have this information prior to verifying the data so we
     * need the version passed in.
     */
    private static Authentication deserializeHeaderAndPutInContext(String header, ThreadContext ctx, CryptoService cryptoService,
                                                                   boolean sign, Version version)
                                                                   throws IOException, IllegalArgumentException {
        assert ctx.getTransient(AUTHENTICATION_KEY) == null;
        if (sign) {
            header = cryptoService.unsignAndVerify(header, version);
        }

        byte[] bytes = Base64.getDecoder().decode(header);
        StreamInput input = StreamInput.wrap(bytes);
        final Version streamVersion = Version.readVersion(input);
        if (streamVersion.equals(version) == false) {
            throw new IllegalStateException("version mismatch. expected [" + version + "] but got [" + streamVersion + "]");
        }
        input.setVersion(version);
        Authentication authentication = new Authentication(input);
        ctx.putTransient(AUTHENTICATION_KEY, authentication);
        return authentication;
    }

    void writeToContextIfMissing(ThreadContext context, CryptoService cryptoService, Settings settings, Version version,
                                 boolean signingEnabled) throws IOException, IllegalArgumentException {
        if (context.getTransient(AUTHENTICATION_KEY) != null) {
            if (context.getHeader(AUTHENTICATION_KEY) == null) {
                throw new IllegalStateException("authentication present as a transient but not a header");
            }
            return;
        }

        if (context.getHeader(AUTHENTICATION_KEY) != null) {
            final boolean shouldSign = shouldSign(settings, version, signingEnabled);
            deserializeHeaderAndPutInContext(context.getHeader(AUTHENTICATION_KEY), context, cryptoService, shouldSign, version);
        } else {
            writeToContext(context, cryptoService, settings, version, signingEnabled);
        }
    }

    public static boolean shouldSign(Settings settings, Version version, boolean signingEnabled) {
        return signingEnabled && XPackSettings.TRANSPORT_SSL_ENABLED.get(settings) == false && version.before(Version.V_5_4_0_UNRELEASED);
    }

    /**
     * Writes the authentication to the context. There must not be an existing authentication in the context and if there is an
     * {@link IllegalStateException} will be thrown
     */
    public void writeToContext(ThreadContext ctx, CryptoService cryptoService, Settings settings, Version version,
                               boolean signingEnabled) throws IOException, IllegalArgumentException {
        ensureContextDoesNotContainAuthentication(ctx);
        String header = encode();
        if (shouldSign(settings, version, signingEnabled)) {
            header = cryptoService.sign(header, version);
        }
        ctx.putTransient(AUTHENTICATION_KEY, this);
        ctx.putHeader(AUTHENTICATION_KEY, header);
    }

    void ensureContextDoesNotContainAuthentication(ThreadContext ctx) {
        if (ctx.getTransient(AUTHENTICATION_KEY) != null) {
            if (ctx.getHeader(AUTHENTICATION_KEY) == null) {
                throw new IllegalStateException("authentication present as a transient but not a header");
            }
            throw new IllegalStateException("authentication is already present in the context");
        }
    }

    String encode() throws IOException {
        BytesStreamOutput output = new BytesStreamOutput();
        Version.writeVersion(version, output);
        writeTo(output);
        return Base64.getEncoder().encodeToString(BytesReference.toBytes(output.bytes()));
    }

    void writeTo(StreamOutput out) throws IOException {
        User.writeTo(user, out);
        authenticatedBy.writeTo(out);
        if (lookedUpBy != null) {
            out.writeBoolean(true);
            lookedUpBy.writeTo(out);
        } else {
            out.writeBoolean(false);
        }
    }

    public static class RealmRef {

        private final String nodeName;
        private final String name;
        private final String type;

        public RealmRef(String name, String type, String nodeName) {
            this.nodeName = nodeName;
            this.name = name;
            this.type = type;
        }

        public RealmRef(StreamInput in) throws IOException {
            this.nodeName = in.readString();
            this.name = in.readString();
            this.type = in.readString();
        }

        void writeTo(StreamOutput out) throws IOException {
            out.writeString(nodeName);
            out.writeString(name);
            out.writeString(type);
        }

        public String getNodeName() {
            return nodeName;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }
    }
}

