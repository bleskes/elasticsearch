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

import java.io.IOException;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.Action;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.FilterClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.node.Node;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.security.authc.Authentication;
import org.elasticsearch.xpack.security.authc.AuthenticationService;
import org.elasticsearch.xpack.security.crypto.CryptoService;
import org.elasticsearch.xpack.security.user.XPackUser;

/**
 * A special filter client for internal node communication which adds the internal xpack user to the headers.
 * An optionally secured client for internal node communication.
 *
 * When secured, the XPack user is added to the execution context before each action is executed.
 */
public class InternalClient extends FilterClient {

    private final CryptoService cryptoService;
    private final boolean signUserHeader;
    private final String nodeName;

    /**
     * Constructs an InternalClient.
     * If {@code cryptoService} is non-null, the client is secure. Otherwise this client is a passthrough.
     */
    public InternalClient(Settings settings, ThreadPool threadPool, Client in, CryptoService cryptoService) {
        super(settings, threadPool, in);
        this.cryptoService = cryptoService;
        this.signUserHeader = AuthenticationService.SIGN_USER_HEADER.get(settings);
        this.nodeName = Node.NODE_NAME_SETTING.get(settings);
    }

    @Override
    protected <Request extends ActionRequest<Request>, Response extends ActionResponse, RequestBuilder extends
        ActionRequestBuilder<Request, Response, RequestBuilder>> void doExecute(
        Action<Request, Response, RequestBuilder> action, Request request, ActionListener<Response> listener) {

        if (cryptoService == null) {
            super.doExecute(action, request, listener);
            return;
        }

        try (ThreadContext.StoredContext ctx = threadPool().getThreadContext().stashContext()) {
            try {
                Authentication authentication = new Authentication(XPackUser.INSTANCE,
                    new Authentication.RealmRef("__attach", "__attach", nodeName), null);
                authentication.writeToContextIfMissing(threadPool().getThreadContext(), cryptoService, signUserHeader);
            } catch (IOException ioe) {
                throw new ElasticsearchException("failed to attach internal user to request", ioe);
            }
            super.doExecute(action, request, listener);
        }
    }
}
