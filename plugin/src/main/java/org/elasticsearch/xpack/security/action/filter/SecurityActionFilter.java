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

package org.elasticsearch.xpack.security.action.filter;

import org.elasticsearch.Version;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.IndicesRequest;
import org.elasticsearch.action.admin.indices.close.CloseIndexAction;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexAction;
import org.elasticsearch.action.admin.indices.open.OpenIndexAction;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.action.support.ActionFilter;
import org.elasticsearch.action.support.ActionFilterChain;
import org.elasticsearch.action.support.ContextPreservingActionListener;
import org.elasticsearch.action.support.DestructiveOperations;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.license.LicenseUtils;
import org.elasticsearch.license.XPackLicenseState;
import org.elasticsearch.tasks.Task;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.XPackPlugin;
import org.elasticsearch.xpack.security.SecurityContext;
import org.elasticsearch.xpack.security.action.SecurityActionMapper;
import org.elasticsearch.xpack.security.action.interceptor.RequestInterceptor;
import org.elasticsearch.xpack.security.audit.AuditTrail;
import org.elasticsearch.xpack.security.audit.AuditTrailService;
import org.elasticsearch.xpack.security.authc.Authentication;
import org.elasticsearch.xpack.security.authc.AuthenticationService;
import org.elasticsearch.xpack.security.authz.AuthorizationService;
import org.elasticsearch.xpack.security.authz.AuthorizationUtils;
import org.elasticsearch.xpack.security.authz.privilege.HealthAndStatsPrivilege;
import org.elasticsearch.xpack.security.crypto.CryptoService;
import org.elasticsearch.xpack.security.support.Automatons;
import org.elasticsearch.xpack.security.user.SystemUser;
import org.elasticsearch.xpack.security.user.User;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.elasticsearch.xpack.security.support.Exceptions.authorizationError;

public class SecurityActionFilter extends AbstractComponent implements ActionFilter {

    private static final Predicate<String> LICENSE_EXPIRATION_ACTION_MATCHER = HealthAndStatsPrivilege.INSTANCE.predicate();
    private static final Predicate<String> SECURITY_ACTION_MATCHER = Automatons.predicate("cluster:admin/xpack/security*");

    private final AuthenticationService authcService;
    private final AuthorizationService authzService;
    private final CryptoService cryptoService;
    private final AuditTrail auditTrail;
    private final SecurityActionMapper actionMapper = new SecurityActionMapper();
    private final Set<RequestInterceptor> requestInterceptors;
    private final XPackLicenseState licenseState;
    private final ThreadContext threadContext;
    private final SecurityContext securityContext;
    private final DestructiveOperations destructiveOperations;
    private final ClusterService clusterService;

    @Inject
    public SecurityActionFilter(Settings settings, AuthenticationService authcService, AuthorizationService authzService,
                                CryptoService cryptoService, AuditTrailService auditTrail, XPackLicenseState licenseState,
                                Set<RequestInterceptor> requestInterceptors, ThreadPool threadPool,
                                SecurityContext securityContext, DestructiveOperations destructiveOperations,
                                ClusterService clusterService) {
        super(settings);
        this.authcService = authcService;
        this.authzService = authzService;
        this.cryptoService = cryptoService;
        this.auditTrail = auditTrail;
        this.licenseState = licenseState;
        this.requestInterceptors = requestInterceptors;
        this.threadContext = threadPool.getThreadContext();
        this.securityContext = securityContext;
        this.destructiveOperations = destructiveOperations;
        this.clusterService = clusterService;
    }

    @Override
    public void apply(Task task, String action, ActionRequest request, ActionListener listener, ActionFilterChain chain) {

        /*
         A functional requirement - when the license of security is disabled (invalid/expires), security will continue
         to operate normally, except all read operations will be blocked.
         */
        if (licenseState.isStatsAndHealthAllowed() == false && LICENSE_EXPIRATION_ACTION_MATCHER.test(action)) {
            logger.error("blocking [{}] operation due to expired license. Cluster health, cluster stats and indices stats \n" +
                    "operations are blocked on license expiration. All data operations (read and write) continue to work. \n" +
                    "If you have a new license, please update it. Otherwise, please reach out to your support contact.", action);
            throw LicenseUtils.newComplianceException(XPackPlugin.SECURITY);
        }

        if (licenseState.isAuthAllowed()) {
            final boolean useSystemUser = AuthorizationUtils.shouldReplaceUserWithSystem(threadContext, action);
            final Supplier<ThreadContext.StoredContext> toRestore = threadContext.newRestorableContext(true);
            final ActionListener<ActionResponse> signingListener = new ContextPreservingActionListener<>(toRestore,
                    ActionListener.wrap(r -> {
                        try {
                            listener.onResponse(sign(r));
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }, listener::onFailure));
            ActionListener<Void> authenticatedListener =
                    ActionListener.wrap((aVoid) -> chain.proceed(task, action, request, signingListener), signingListener::onFailure);
            try {
                if (useSystemUser) {
                    securityContext.executeAsUser(SystemUser.INSTANCE, (original) -> {
                        try {
                            applyInternal(action, request, authenticatedListener);
                        } catch (IOException e) {
                            listener.onFailure(e);
                        }
                    }, Version.CURRENT);
                } else {
                    try (ThreadContext.StoredContext ignore = threadContext.newStoredContext(true)) {
                        applyInternal(action, request, authenticatedListener);
                    }
                }
            } catch (Exception e) {
                listener.onFailure(e);
            }
        } else if (SECURITY_ACTION_MATCHER.test(action)) {
            listener.onFailure(LicenseUtils.newComplianceException(XPackPlugin.SECURITY));
        } else {
            chain.proceed(task, action, request, listener);
        }
    }

    @Override
    public int order() {
        return Integer.MIN_VALUE;
    }

    private void applyInternal(String action, final ActionRequest request, ActionListener<Void> listener) throws IOException {
        if (CloseIndexAction.NAME.equals(action) || OpenIndexAction.NAME.equals(action) || DeleteIndexAction.NAME.equals(action)) {
            IndicesRequest indicesRequest = (IndicesRequest) request;
            try {
                destructiveOperations.failDestructive(indicesRequest.indices());
            } catch(IllegalArgumentException e) {
                listener.onFailure(e);
                return;
            }
        }

        /*
         here we fallback on the system user. Internal system requests are requests that are triggered by
         the system itself (e.g. pings, update mappings, share relocation, etc...) and were not originated
         by user interaction. Since these requests are triggered by es core modules, they are security
         agnostic and therefore not associated with any user. When these requests execute locally, they
         are executed directly on their relevant action. Since there is no other way a request can make
         it to the action without an associated user (not via REST or transport - this is taken care of by
         the {@link Rest} filter and the {@link ServerTransport} filter respectively), it's safe to assume a system user
         here if a request is not associated with any other user.
         */
        final String securityAction = actionMapper.action(action, request);
        authcService.authenticate(securityAction, request, SystemUser.INSTANCE, Version.CURRENT,
                ActionListener.wrap((authc) -> authorizeRequest(authc, securityAction, request, listener), listener::onFailure));
    }

    void authorizeRequest(Authentication authentication, String securityAction, ActionRequest request, ActionListener listener) {
        if (authentication == null) {
            listener.onFailure(new IllegalArgumentException("authentication must be non null for authorization"));
        } else {
            final AuthorizationUtils.AsyncAuthorizer asyncAuthorizer = new AuthorizationUtils.AsyncAuthorizer(authentication, listener,
                    (userRoles, runAsRoles) -> {
                        authzService.authorize(authentication, securityAction, request, userRoles, runAsRoles);
                        final User user = authentication.getUser();
                        ActionRequest unsignedRequest = unsign(user, securityAction, request);

                        /*
                         * We use a separate concept for code that needs to be run after authentication and authorization that could
                         * affect the running of the action. This is done to make it more clear of the state of the request.
                         */
                        for (RequestInterceptor interceptor : requestInterceptors) {
                            if (interceptor.supports(unsignedRequest)) {
                                interceptor.intercept(unsignedRequest, user);
                            }
                        }
                        listener.onResponse(null);
                    });
            asyncAuthorizer.authorize(authzService);
        }
    }

    ActionRequest unsign(User user, String action, final ActionRequest request) {
        try {
            // In order to provide backwards compatibility with previous versions that always signed scroll ids
            // we sign the scroll requests and do not allow unsigned requests until all of the nodes in the cluster
            // have been upgraded to a version that does not sign scroll ids and instead relies improved scroll
            // authorization. It is important to note that older versions do not actually sign if the system key
            // does not exist so we need to take that into account as well.
            // TODO update to 5.5 on backport and remove any signing from master!
            final ClusterState state = clusterService.state();
            final boolean signingRequired = state.nodes().getMinNodeVersion().before(Version.V_5_5_0_UNRELEASED) &&
                    cryptoService.isSystemKeyPresent();
            if (request instanceof SearchScrollRequest) {
                SearchScrollRequest scrollRequest = (SearchScrollRequest) request;
                String scrollId = scrollRequest.scrollId();
                if (signingRequired) {
                    if (cryptoService.isSigned(scrollId)) {
                        scrollRequest.scrollId(cryptoService.unsignAndVerify(scrollId, null));
                    } else {
                        logger.error("scroll id [{}] is not signed but is expected to be. nodes [{}], minimum node version [{}]",
                                scrollId, state.nodes(), state.nodes().getMinNodeVersion());
                        // if we get a unsigned scroll request and not all nodes are up to date, then we cannot trust
                        // this scroll id and reject it
                        auditTrail.tamperedRequest(user, action, request);
                        throw authorizationError("invalid request");
                    }
                } else if (cryptoService.isSigned(scrollId)) {
                    // if signing isn't required we could still get a signed ID from an already running scroll or
                    // a node that hasn't received the current cluster state that shows signing isn't required
                    scrollRequest.scrollId(cryptoService.unsignAndVerify(scrollId, null));
                }
                // else the scroll id is fine on the request so don't do anything
            } else if (request instanceof ClearScrollRequest) {
                ClearScrollRequest clearScrollRequest = (ClearScrollRequest) request;
                final boolean isClearAllScrollRequest = clearScrollRequest.scrollIds().contains("_all");
                if (isClearAllScrollRequest == false) {
                    List<String> signedIds = clearScrollRequest.scrollIds();
                    List<String> unsignedIds = new ArrayList<>(signedIds.size());
                    for (String signedId : signedIds) {
                        if (signingRequired) {
                            if (cryptoService.isSigned(signedId)) {
                                unsignedIds.add(cryptoService.unsignAndVerify(signedId, null));
                            } else {
                                logger.error("scroll id [{}] is not signed but is expected to be. nodes [{}], minimum node version [{}]",
                                        signedId, state.nodes(), state.nodes().getMinNodeVersion());
                                // if we get a unsigned scroll request and not all nodes are up to date, then we cannot trust
                                // this scroll id and reject it
                                auditTrail.tamperedRequest(user, action, request);
                                throw authorizationError("invalid request");
                            }
                        } else if (cryptoService.isSigned(signedId)) {
                            // if signing isn't required we could still get a signed ID from an already running scroll or
                            // a node that hasn't received the current cluster state that shows signing isn't required
                            unsignedIds.add(cryptoService.unsignAndVerify(signedId, null));
                        } else {
                            // the id is not signed and we allow unsigned requests so just add it
                            unsignedIds.add(signedId);
                        }
                    }
                    clearScrollRequest.scrollIds(unsignedIds);
                }
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            // this can happen when we decode invalid base64 or get a invalid scroll id
            auditTrail.tamperedRequest(user, action, request);
            throw authorizationError("invalid request. {}", e.getMessage());
        }
        return request;
    }

    private <Response extends ActionResponse> Response sign(Response response) throws IOException {
        if (response instanceof SearchResponse) {
            // In order to provide backwards compatibility with previous versions that always signed scroll ids
            // we sign the scroll requests and do not allow unsigned requests until all of the nodes in the cluster
            // have been upgraded to a version that supports unsigned scroll ids
            final boolean sign = clusterService.state().nodes().getMinNodeVersion().before(Version.V_5_5_0_UNRELEASED);

            if (sign) {
                SearchResponse searchResponse = (SearchResponse) response;
                String scrollId = searchResponse.getScrollId();
                if (scrollId != null && !cryptoService.isSigned(scrollId)) {
                    searchResponse.scrollId(cryptoService.sign(scrollId, Version.CURRENT));
                }
            }
        }
        return response;
    }
}
