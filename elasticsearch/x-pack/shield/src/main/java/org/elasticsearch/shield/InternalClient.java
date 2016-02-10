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

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.Action;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.FilterClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.shield.authc.AuthenticationService;
import org.elasticsearch.shield.user.XPackUser;

import java.io.IOException;

/**
 *
 */
public abstract class InternalClient extends FilterClient {

    protected InternalClient(Client in) {
        super(in);
    }

    /**
     * An insecured internal client, baseically simply delegates to the normal ES client
     * without doing anything extra.
     */
    public static class Insecure extends InternalClient {

        @Inject
        public Insecure(Client in) {
            super(in);
        }
    }

    /**
     * A secured internal client that binds the internal XPack user to the current
     * execution context, before the action is executed.
     */
    public static class Secure extends InternalClient {

        private AuthenticationService authcService;

        @Inject
        public Secure(Client in, AuthenticationService authcService) {
            super(in);
            this.authcService = authcService;
        }

        @Override
        protected <Request extends ActionRequest<Request>, Response extends ActionResponse, RequestBuilder extends
                ActionRequestBuilder<Request, Response, RequestBuilder>> void doExecute(
                Action<Request, Response, RequestBuilder> action, Request request, ActionListener<Response> listener) {

            try (ThreadContext.StoredContext ctx = threadPool().getThreadContext().stashContext()) {
                try {
                    authcService.attachUserHeaderIfMissing(XPackUser.INSTANCE);
                } catch (IOException ioe) {
                    throw new ElasticsearchException("failed to attach internal user to request", ioe);
                }
                super.doExecute(action, request, listener);
            }
        }
    }
}
