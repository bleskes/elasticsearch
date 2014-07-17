package org.elasticsearch.shield.authz;

import org.elasticsearch.shield.User;
import org.elasticsearch.transport.TransportRequest;

/**
 *
 */
public interface AuthorizationService {

    void authorize(User user, String action, TransportRequest request) throws AuthorizationException;

}
