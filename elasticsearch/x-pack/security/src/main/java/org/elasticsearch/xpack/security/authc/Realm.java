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

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.xpack.security.user.User;

import java.util.HashMap;
import java.util.Map;

/**
 * An authentication mechanism to which the default authentication {@link org.elasticsearch.xpack.security.authc.AuthenticationService
 * service } delegates the authentication process. Different realms may be defined, each may be based on different
 * authentication mechanism supporting its own specific authentication token type.
 */
public abstract class Realm implements Comparable<Realm> {

    protected final ESLogger logger;
    protected final String type;
    protected RealmConfig config;

    public Realm(String type, RealmConfig config) {
        this.type = type;
        this.config = config;
        this.logger = config.logger(getClass());
    }

    /**
     * @return  The type of this realm
     */
    public String type() {
        return type;
    }

    /**
     * @return  The name of this realm.
     */
    public String name() {
        return config.name;
    }

    /**
     * @return  The order of this realm within the executing realm chain.
     */
    public int order() {
        return config.order;
    }

    @Override
    public int compareTo(Realm other) {
        return Integer.compare(config.order, other.config.order);
    }

    /**
     * @return  {@code true} if this realm supports the given authentication token, {@code false} otherwise.
     */
    public abstract boolean supports(AuthenticationToken token);

    /**
     * Attempts to extract an authentication token from the given context. If an appropriate token
     * is found it's returned, otherwise {@code null} is returned.
     *
     * @param context   The context that will provide information about the incoming request
     * @return          The authentication token or {@code null} if not found
     */
    public abstract AuthenticationToken token(ThreadContext context);

    /**
     * Authenticates the given token. A successful authentication will return the User associated
     * with the given token. An unsuccessful authentication returns {@code null}.
     *
     * @param token The authentication token
     * @return      The authenticated user or {@code null} if authentication failed.
     */
    public abstract User authenticate(AuthenticationToken token);

    /**
     * Looks up the user identified the String identifier. A successful lookup will return the {@link User} identified
     * by the username. An unsuccessful lookup returns {@code null}.
     *
     * @param username the String identifier for the user
     * @return         the {@link User} or {@code null} if lookup failed
     */
    public abstract User lookupUser(String username);

    public Map<String, Object> usageStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("name", name());
        stats.put("order", order());
        return stats;
    }

    /**
     * Indicates whether this realm supports user lookup.
     * @return true if the realm supports user lookup
     */
    public abstract boolean userLookupSupported();

    @Override
    public String toString() {
        return type + "/" + config.name;
    }

    /**
     * A factory interface to construct a security realm.
     */
    public interface Factory {

        /**
         * Constructs a realm which will be used for authentication.
         * @param config The configuration for the realm
         */
        Realm create(RealmConfig config);
    }
}
