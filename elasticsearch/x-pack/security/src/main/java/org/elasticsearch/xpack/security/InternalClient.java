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

package org.elasticsearch.xpack.security;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.Action;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.FilterClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.xpack.security.authc.AuthenticationService;
import org.elasticsearch.xpack.security.user.XPackUser;
import org.elasticsearch.threadpool.ThreadPool;

import java.io.IOException;

/**
 *
 */
public interface InternalClient extends Client {


    /**
     * An insecured internal client, baseically simply delegates to the normal ES client
     * without doing anything extra.
     */
    class Insecure extends FilterClient implements InternalClient {

        @Inject
        public Insecure(Settings settings, ThreadPool threadPool, Client in) {
            super(settings, threadPool, in);
        }
    }

    /**
     * A secured internal client that binds the internal XPack user to the current
     * execution context, before the action is executed.
     */
    class Secure extends FilterClient implements InternalClient {

        private final AuthenticationService authcService;

        @Inject
        public Secure(Settings settings, ThreadPool threadPool, Client in, AuthenticationService authcService) {
            super(settings, threadPool, in);
            this.authcService = authcService;
        }

        @Override
        protected <Request extends ActionRequest<Request>, Response extends ActionResponse, RequestBuilder extends
                ActionRequestBuilder<Request, Response, RequestBuilder>> void doExecute(
                Action<Request, Response, RequestBuilder> action, Request request, ActionListener<Response> listener) {

            try (ThreadContext.StoredContext ctx = threadPool().getThreadContext().stashContext()) {
                try {
                    authcService.attachUserIfMissing(XPackUser.INSTANCE);
                } catch (IOException ioe) {
                    throw new ElasticsearchException("failed to attach internal user to request", ioe);
                }
                super.doExecute(action, request, listener);
            }
        }
    }
}
