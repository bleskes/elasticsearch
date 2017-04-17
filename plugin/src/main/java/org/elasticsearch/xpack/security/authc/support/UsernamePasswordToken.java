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

package org.elasticsearch.xpack.security.authc.support;

import org.elasticsearch.common.settings.SecureString;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.xpack.security.authc.AuthenticationToken;

import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

import static org.elasticsearch.xpack.security.support.Exceptions.authenticationError;

/**
 *
 */
public class UsernamePasswordToken implements AuthenticationToken {

    public static final String BASIC_AUTH_HEADER = "Authorization";
    private static final String BASIC_AUTH_PREFIX = "Basic ";

    private final String username;
    private final SecureString password;

    public UsernamePasswordToken(String username, SecureString password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public String principal() {
        return username;
    }

    @Override
    public SecureString credentials() {
        return password;
    }

    @Override
    public void clearCredentials() {
        password.close();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UsernamePasswordToken that = (UsernamePasswordToken) o;

        return Objects.equals(password, that.password) &&
                Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, password.hashCode());
    }

    public static UsernamePasswordToken extractToken(ThreadContext context) {
        String authStr = context.getHeader(BASIC_AUTH_HEADER);
        if (authStr == null) {
            return null;
        }

        return extractToken(authStr);
    }

    private static UsernamePasswordToken extractToken(String headerValue) {
        if (headerValue.startsWith(BASIC_AUTH_PREFIX) == false) {
            // the header does not start with 'Basic ' so we cannot use it, but it may be valid for another realm
            return null;
        }

        // if there is nothing after the prefix, the header is bad
        if (headerValue.length() == BASIC_AUTH_PREFIX.length()) {
            throw authenticationError("invalid basic authentication header value");
        }

        char[] userpasswd;
        try {
            userpasswd = CharArrays.utf8BytesToChars(Base64.getDecoder().decode(headerValue.substring(BASIC_AUTH_PREFIX.length()).trim()));
        } catch (IllegalArgumentException e) {
            throw authenticationError("invalid basic authentication header encoding", e);
        }

        int i = CharArrays.indexOf(userpasswd, ':');
        if (i < 0) {
            throw authenticationError("invalid basic authentication header value");
        }

        return new UsernamePasswordToken(
                new String(Arrays.copyOfRange(userpasswd, 0, i)),
                new SecureString(Arrays.copyOfRange(userpasswd, i + 1, userpasswd.length)));
    }

    public static void putTokenHeader(ThreadContext context, UsernamePasswordToken token) {
        context.putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue(token.username, token.password));
    }

    public static String basicAuthHeaderValue(String username, SecureString passwd) {
        CharBuffer chars = CharBuffer.allocate(username.length() + passwd.length() + 1);
        byte[] charBytes = null;
        try {
            chars.put(username).put(':').put(passwd.getChars());
            charBytes = CharArrays.toUtf8Bytes(chars.array());

            //TODO we still have passwords in Strings in headers. Maybe we can look into using a CharSequence?
            String basicToken = Base64.getEncoder().encodeToString(charBytes);
            return "Basic " + basicToken;
        } finally {
            Arrays.fill(chars.array(), (char) 0);
            if (charBytes != null) {
                Arrays.fill(charBytes, (byte) 0);
            }
        }
    }
}
