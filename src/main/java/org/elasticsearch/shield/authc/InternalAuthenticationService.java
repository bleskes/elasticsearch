package org.elasticsearch.shield.authc;

import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.internal.Nullable;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.shield.User;
import org.elasticsearch.shield.audit.AuditTrail;
import org.elasticsearch.transport.TransportRequest;

/**
 * An authentication service that delegates the authentication process to its configured {@link Realm realms}.
 */
public class InternalAuthenticationService extends AbstractComponent implements AuthenticationService {

    private final Realm[] realms;
    private final AuditTrail auditTrail;

    @Inject
    public InternalAuthenticationService(Settings settings, Realms realms, @Nullable AuditTrail auditTrail) {
        super(settings);
        this.realms = realms.realms();
        this.auditTrail = auditTrail;
    }

    /**
     * Authenticates the user associated with the given request by delegating the authentication to
     * the configured realms. Each realm will be asked to authenticate the request, the first realm that
     * successfully authenticates will "win" and its authenticated user will be returned. If none of the
     * configured realms successfully authenticates the request, an {@link AuthenticationException} will
     * be thrown.
     *
     * The order by which the realms are ran is based on the order by which they were set in the
     * constructor.
     *
     * @param request   The executed request
     * @return          The authenticated user
     * @throws AuthenticationException If none of the configured realms successfully authenticated the
     *                                 request
     */
    @Override
    public User authenticate(String action, TransportRequest request) throws AuthenticationException {
        for (Realm realm : realms) {
            AuthenticationToken token = realm.token(request);
            if (token != null) {
                User user = realm.authenticate(token);
                if (user != null) {
                    return user;
                } else if (auditTrail != null) {
                    auditTrail.authenticationFailed(realm.type(), token, action, request);
                }
            }
        }
        if (auditTrail != null) {
            auditTrail.anonymousAccess(action, request);
        }
        throw new AuthenticationException("Unable to authenticate user for request");
    }
}
