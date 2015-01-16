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

package org.elasticsearch.shield.authc;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.shield.User;
import org.elasticsearch.transport.TransportMessage;

/**
 * An authentication mechanism to which the default authentication {@link org.elasticsearch.shield.authc.AuthenticationService service}
 * delegates the authentication process. Different realms may be defined, each may be based on different
 * authentication mechanism supporting its own specific authentication token type.
 */
public abstract class Realm<T extends AuthenticationToken> implements Comparable<Realm> {

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
     * Attempts to extract an authentication token from the given rest request. If an appropriate token
     * is found it's returned, otherwise {@code null} is returned.
     *
     * @param request   The rest request
     * @return          The authentication token or {@code null} if not found
     */
    public abstract T token(RestRequest request);

    /**
     * Attempts to extract an authentication token from the given transport message. If an appropriate token
     * is found it's returned, otherwise {@code null} is returned.
     *
     * @param message   The transport message
     * @return          The authentication token or {@code null} if not found
     */
    public abstract T token(TransportMessage<?> message);

    /**
     * Authenticates the given token. A successful authentication will return the User associated
     * with the given token. An unsuccessful authentication returns {@code null}.
     *
     * @param token The authentication token
     * @return      The authenticated user or {@code null} if authentication failed.
     */
    public abstract User authenticate(T token);

    @Override
    public String toString() {
        return type + "/" + config.name;
    }

    /**
     * A factory for a specific realm type. Knows how to create a new realm given the appropriate
     * settings
     */
    public static abstract class Factory<R extends Realm> {

        private final String type;
        private final boolean internal;

        public Factory(String type, boolean internal) {
            this.type = type;
            this.internal = internal;
        }

        /**
         * @return  The type of the ream this factory creates
         */
        public String type() {
            return type;
        }

        public boolean internal() {
            return internal;
        }

        /**
         * Creates a new realm based on the given settigns.
         *
         * @param config    The configuration for the realm
         * @return          The new realm (this method never returns {@code null}).
         */
        public abstract R create(RealmConfig config);

        /**
         * Creates a default realm, one that has no custom settings. Some realms might require minimal
         * settings, in which case, this method will return {@code null}.
         */
        public abstract R createDefault(String name);
    }

}
