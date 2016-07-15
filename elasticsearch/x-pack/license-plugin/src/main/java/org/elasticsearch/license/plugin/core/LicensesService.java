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
package org.elasticsearch.license.plugin.core;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.cluster.AckedClusterStateUpdateTask;
import org.elasticsearch.cluster.ClusterChangedEvent;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.ClusterStateListener;
import org.elasticsearch.cluster.ClusterStateUpdateTask;
import org.elasticsearch.cluster.ack.ClusterStateUpdateResponse;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.component.Lifecycle;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.joda.FormatDateTimeFormatter;
import org.elasticsearch.common.joda.Joda;
import org.elasticsearch.common.logging.LoggerMessageFormat;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.gateway.GatewayService;
import org.elasticsearch.license.core.License;
import org.elasticsearch.license.core.LicenseVerifier;
import org.elasticsearch.license.plugin.action.delete.DeleteLicenseRequest;
import org.elasticsearch.license.plugin.action.put.PutLicenseRequest;
import org.elasticsearch.license.plugin.action.put.PutLicenseResponse;
import org.elasticsearch.xpack.scheduler.SchedulerEngine;
import org.elasticsearch.xpack.support.clock.Clock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Service responsible for managing {@link LicensesMetaData}
 * Interfaces through which this is exposed are:
 * - LicensesClientService - responsible for listener registration of consumer plugin(s)
 * <p>
 * Notification Scheme:
 * <p>
 * All registered listeners are notified of the current license upon registration or when a new license is installed in the cluster state.
 * When a new license is notified as enabled to the registered listener, a notification is scheduled at the time of license expiry.
 * Registered listeners are notified using {@link #onUpdate(LicensesMetaData)}
 */
public class LicensesService extends AbstractLifecycleComponent implements ClusterStateListener, SchedulerEngine.Listener {

    // pkg private for tests
    static final TimeValue TRIAL_LICENSE_DURATION = TimeValue.timeValueHours(30 * 24);

    private final ClusterService clusterService;

    /**
     * Currently active consumers to notify to
     */
    private final List<InternalLicensee> registeredLicensees;

    /**
     * Currently active license
     */
    private final AtomicReference<License> currentLicense = new AtomicReference<>();
    private SchedulerEngine scheduler;
    private final Clock clock;

    /**
     * Callbacks to notify relative to license expiry
     */
    private List<ExpirationCallback> expirationCallbacks = new ArrayList<>();

    /**
     * Max number of nodes licensed by generated trial license
     */
    private int trialLicenseMaxNodes = 1000;

    /**
     * Duration of grace period after a license has expired
     */
    public static final TimeValue GRACE_PERIOD_DURATION = days(7);

    private static final String LICENSE_JOB = "licenseJob";

    private static final FormatDateTimeFormatter DATE_FORMATTER = Joda.forPattern("EEEE, MMMMM dd, yyyy", Locale.ROOT);

    private static final String ACKNOWLEDGEMENT_HEADER = "This license update requires acknowledgement. To acknowledge the license, " +
            "please read the following messages and update the license again, this time with the \"acknowledge=true\" parameter:";

    public LicensesService(Settings settings, ClusterService clusterService, Clock clock,
                           List<Licensee> registeredLicensees) {
        super(settings);
        this.clusterService = clusterService;
        populateExpirationCallbacks();
        this.clock = clock;
        this.scheduler = new SchedulerEngine(clock);
        this.scheduler.register(this);
        this.registeredLicensees = registeredLicensees.stream().map(InternalLicensee::new).collect(Collectors.toList());
    }

    private void populateExpirationCallbacks() {
        expirationCallbacks.add(new ExpirationCallback.Pre(days(7), days(25), days(1)) {
                                    @Override
                                    public void on(License license) {
                                        String general = LoggerMessageFormat.format(null, "\n" +
                                                "#\n" +
                                                "# License will expire on [{}]. If you have a new license, please update it.\n" +
                                                "# Otherwise, please reach out to your support contact.\n" +
                                                "# ", DATE_FORMATTER.printer().print(license.expiryDate()));
                                        if (!registeredLicensees.isEmpty()) {
                                            StringBuilder builder = new StringBuilder(general);
                                            builder.append(System.lineSeparator());
                                            builder.append("# Commercial plugins operate with reduced functionality on license " +
                                                    "expiration:");
                                            for (InternalLicensee licensee : registeredLicensees) {
                                                if (licensee.expirationMessages().length > 0) {
                                                    builder.append(System.lineSeparator());
                                                    builder.append("# - ");
                                                    builder.append(licensee.id());
                                                    for (String message : licensee.expirationMessages()) {
                                                        builder.append(System.lineSeparator());
                                                        builder.append("#  - ");
                                                        builder.append(message);
                                                    }
                                                }
                                            }
                                            logger.warn("{}", builder);
                                        } else {
                                            logger.warn("{}", general);
                                        }
                                    }
                                }
        );
        expirationCallbacks.add(new ExpirationCallback.Pre(days(0), days(7), TimeValue.timeValueMinutes(10)) {
                                    @Override
                                    public void on(License license) {
                                        String general = LoggerMessageFormat.format(null, "\n" +
                                                "#\n" +
                                                "# License will expire on [{}]. If you have a new license, please update it.\n" +
                                                "# Otherwise, please reach out to your support contact.\n" +
                                                "# ", DATE_FORMATTER.printer().print(license.expiryDate()));
                                        if (!registeredLicensees.isEmpty()) {
                                            StringBuilder builder = new StringBuilder(general);
                                            builder.append(System.lineSeparator());
                                            builder.append("# Commercial plugins operate with reduced functionality on license " +
                                                    "expiration:");
                                            for (InternalLicensee licensee : registeredLicensees) {
                                                if (licensee.expirationMessages().length > 0) {
                                                    builder.append(System.lineSeparator());
                                                    builder.append("# - ");
                                                    builder.append(licensee.id());
                                                    for (String message : licensee.expirationMessages()) {
                                                        builder.append(System.lineSeparator());
                                                        builder.append("#  - ");
                                                        builder.append(message);
                                                    }
                                                }
                                            }
                                            logger.warn("{}", builder.toString());
                                        } else {
                                            logger.warn("{}", general);
                                        }
                                    }
                                }
        );
        expirationCallbacks.add(new ExpirationCallback.Post(days(0), null, TimeValue.timeValueMinutes(10)) {
                                    @Override
                                    public void on(License license) {
                                        // logged when grace period begins
                                        String general = LoggerMessageFormat.format(null, "\n" +
                                                "#\n" +
                                                "# LICENSE EXPIRED ON [{}]. IF YOU HAVE A NEW LICENSE, PLEASE\n" +
                                                "# UPDATE IT. OTHERWISE, PLEASE REACH OUT TO YOUR SUPPORT CONTACT.\n" +
                                                "# ", DATE_FORMATTER.printer().print(license.expiryDate()));
                                        if (!registeredLicensees.isEmpty()) {
                                            StringBuilder builder = new StringBuilder(general);
                                            builder.append(System.lineSeparator());
                                            builder.append("# COMMERCIAL PLUGINS OPERATING WITH REDUCED FUNCTIONALITY");
                                            for (InternalLicensee licensee : registeredLicensees) {
                                                if (licensee.expirationMessages().length > 0) {
                                                    builder.append(System.lineSeparator());
                                                    builder.append("# - ");
                                                    builder.append(licensee.id());
                                                    for (String message : licensee.expirationMessages()) {
                                                        builder.append(System.lineSeparator());
                                                        builder.append("#  - ");
                                                        builder.append(message);
                                                    }
                                                }
                                            }
                                            logger.warn("{}", builder.toString());
                                        } else {
                                            logger.warn("{}", general);
                                        }
                                    }
                                }
        );
    }

    /**
     * Registers new license in the cluster
     * Master only operation. Installs a new license on the master provided it is VALID
     */
    public void registerLicense(final PutLicenseRequest request, final ActionListener<PutLicenseResponse> listener) {
        final License newLicense = request.license();
        final long now = clock.millis();
        if (!LicenseVerifier.verifyLicense(newLicense) || newLicense.issueDate() > now) {
            listener.onResponse(new PutLicenseResponse(true, LicensesStatus.INVALID));
        } else if (newLicense.expiryDate() < now) {
            listener.onResponse(new PutLicenseResponse(true, LicensesStatus.EXPIRED));
        } else {
            if (!request.acknowledged()) {
                final License currentLicense = getLicense();
                if (currentLicense != null) {
                    Map<String, String[]> acknowledgeMessages = new HashMap<>(registeredLicensees.size() + 1);
                    if (!License.isAutoGeneratedLicense(currentLicense.signature()) // current license is not auto-generated
                            && currentLicense.issueDate() > newLicense.issueDate()) { // and has a later issue date
                        acknowledgeMessages.put("license",
                                new String[]{"The new license is older than the currently installed license. Are you sure you want to " +
                                        "override the current license?"});
                    }
                    for (InternalLicensee licensee : registeredLicensees) {
                        String[] listenerAcknowledgeMessages = licensee.acknowledgmentMessages(currentLicense, newLicense);
                        if (listenerAcknowledgeMessages.length > 0) {
                            acknowledgeMessages.put(licensee.id(), listenerAcknowledgeMessages);
                        }
                    }
                    if (!acknowledgeMessages.isEmpty()) {
                        // needs acknowledgement
                        listener.onResponse(new PutLicenseResponse(false, LicensesStatus.VALID, ACKNOWLEDGEMENT_HEADER,
                                acknowledgeMessages));
                        return;
                    }
                }
            }
            clusterService.submitStateUpdateTask("register license [" + newLicense.uid() + "]", new
                    AckedClusterStateUpdateTask<PutLicenseResponse>(request, listener) {
                        @Override
                        protected PutLicenseResponse newResponse(boolean acknowledged) {
                            return new PutLicenseResponse(acknowledged, LicensesStatus.VALID);
                        }

                        @Override
                        public ClusterState execute(ClusterState currentState) throws Exception {
                            MetaData.Builder mdBuilder = MetaData.builder(currentState.metaData());
                            mdBuilder.putCustom(LicensesMetaData.TYPE, new LicensesMetaData(newLicense));
                            return ClusterState.builder(currentState).metaData(mdBuilder).build();
                        }
                    });
        }
    }


    static TimeValue days(int days) {
        return TimeValue.timeValueHours(days * 24);
    }

    @Override
    public void triggered(SchedulerEngine.Event event) {
        final LicensesMetaData licensesMetaData = clusterService.state().metaData().custom(LicensesMetaData.TYPE);
        if (licensesMetaData != null) {
            final License license = licensesMetaData.getLicense();
            if (event.getJobName().equals(LICENSE_JOB)) {
                notifyLicensees(license);
            } else if (event.getJobName().startsWith(ExpirationCallback.EXPIRATION_JOB_PREFIX)) {
                expirationCallbacks.stream()
                        .filter(expirationCallback -> expirationCallback.getId().equals(event.getJobName()))
                        .forEach(expirationCallback -> expirationCallback.on(license));
            }
        }
    }

    /**
     * Remove license from the cluster state metadata
     */
    public void removeLicense(final DeleteLicenseRequest request, final ActionListener<ClusterStateUpdateResponse> listener) {
        clusterService.submitStateUpdateTask("delete license",
                new AckedClusterStateUpdateTask<ClusterStateUpdateResponse>(request, listener) {
                    @Override
                    protected ClusterStateUpdateResponse newResponse(boolean acknowledged) {
                        return new ClusterStateUpdateResponse(acknowledged);
                    }

                    @Override
                    public ClusterState execute(ClusterState currentState) throws Exception {
                        MetaData metaData = currentState.metaData();
                        final LicensesMetaData currentLicenses = metaData.custom(LicensesMetaData.TYPE);
                        if (currentLicenses.getLicense() != LicensesMetaData.LICENSE_TOMBSTONE) {
                            MetaData.Builder mdBuilder = MetaData.builder(currentState.metaData());
                            mdBuilder.putCustom(LicensesMetaData.TYPE, new LicensesMetaData(LicensesMetaData.LICENSE_TOMBSTONE));
                            return ClusterState.builder(currentState).metaData(mdBuilder).build();
                        } else {
                            return currentState;
                        }
                    }
                });
    }

    public LicenseState licenseState() {
        if (registeredLicensees.size() > 0) {
            return registeredLicensees.get(0).currentLicenseState;
        } else {
            final License license = getLicense(clusterService.state().metaData().custom(LicensesMetaData.TYPE));
            return getLicenseState(license, clock.millis());
        }
    }

    public License getLicense() {
        final License license = getLicense(clusterService.state().metaData().custom(LicensesMetaData.TYPE));
        return license == LicensesMetaData.LICENSE_TOMBSTONE ? null : license;
    }

    /**
     * Master-only operation to generate a one-time global trial license.
     * The trial license is only generated and stored if the current cluster state metaData
     * has no signed/trial license
     */
    private void registerTrialLicense() {
        clusterService.submitStateUpdateTask("generate trial license for [" + TRIAL_LICENSE_DURATION + "]", new ClusterStateUpdateTask() {
            @Override
            public void clusterStateProcessed(String source, ClusterState oldState, ClusterState newState) {
                LicensesMetaData licensesMetaData = newState.metaData().custom(LicensesMetaData.TYPE);
                if (logger.isDebugEnabled()) {
                    logger.debug("registered trial license: {}", licensesMetaData);
                }
            }

            @Override
            public ClusterState execute(ClusterState currentState) throws Exception {
                final MetaData metaData = currentState.metaData();
                final LicensesMetaData currentLicensesMetaData = metaData.custom(LicensesMetaData.TYPE);
                MetaData.Builder mdBuilder = MetaData.builder(currentState.metaData());
                // do not generate a trial license if any license is present
                if (currentLicensesMetaData == null) {
                    long issueDate = clock.millis();
                    License.Builder specBuilder = License.builder()
                            .uid(UUID.randomUUID().toString())
                            .issuedTo(clusterService.getClusterName().value())
                            .maxNodes(trialLicenseMaxNodes)
                            .issueDate(issueDate)
                            .expiryDate(issueDate + TRIAL_LICENSE_DURATION.getMillis());
                    License trialLicense = TrialLicense.create(specBuilder);
                    mdBuilder.putCustom(LicensesMetaData.TYPE, new LicensesMetaData(trialLicense));
                    return ClusterState.builder(currentState).metaData(mdBuilder).build();
                }
                return currentState;
            }

            @Override
            public void onFailure(String source, @Nullable Exception e) {
                logger.error("unexpected failure during [{}]", e, source);
            }

        });
    }

    @Override
    protected void doStart() throws ElasticsearchException {
        clusterService.add(this);
        scheduler.start(Collections.emptyList());
        registeredLicensees.forEach(x -> initLicensee(x.licensee));
    }

    @Override
    protected void doStop() throws ElasticsearchException {
        clusterService.remove(this);
        scheduler.stop();
        // clear all handlers
        registeredLicensees.clear();

        // clear current license
        currentLicense.set(null);
    }

    @Override
    protected void doClose() throws ElasticsearchException {
    }

    /**
     * When there is no global block on {@link org.elasticsearch.gateway.GatewayService#STATE_NOT_RECOVERED_BLOCK}
     * notify licensees and issue auto-generated license if no license has been installed/issued yet.
     */
    @Override
    public void clusterChanged(ClusterChangedEvent event) {
        final ClusterState previousClusterState = event.previousState();
        final ClusterState currentClusterState = event.state();
        if (!currentClusterState.blocks().hasGlobalBlock(GatewayService.STATE_NOT_RECOVERED_BLOCK)) {
            final LicensesMetaData prevLicensesMetaData = previousClusterState.getMetaData().custom(LicensesMetaData.TYPE);
            final LicensesMetaData currentLicensesMetaData = currentClusterState.getMetaData().custom(LicensesMetaData.TYPE);
            if (logger.isDebugEnabled()) {
                logger.debug("previous [{}]", prevLicensesMetaData);
                logger.debug("current [{}]", currentLicensesMetaData);
            }
            // notify all interested plugins
            if (previousClusterState.blocks().hasGlobalBlock(GatewayService.STATE_NOT_RECOVERED_BLOCK)
                    || prevLicensesMetaData == null) {
                if (currentLicensesMetaData != null) {
                    onUpdate(currentLicensesMetaData);
                }
            } else if (!prevLicensesMetaData.equals(currentLicensesMetaData)) {
                onUpdate(currentLicensesMetaData);
            }
            // auto-generate license if no licenses ever existed
            // this will trigger a subsequent cluster changed event
            if (currentClusterState.getNodes().isLocalNodeElectedMaster() &&
                prevLicensesMetaData == null &&
                (currentLicensesMetaData == null || currentLicensesMetaData.getLicense() == null)) {
                registerTrialLicense();
            }
        } else if (logger.isDebugEnabled()) {
            logger.debug("skipped license notifications reason: [{}]", GatewayService.STATE_NOT_RECOVERED_BLOCK);
        }
    }

    private void notifyLicensees(final License license) {
        if (license == LicensesMetaData.LICENSE_TOMBSTONE) {
            // implies license has been explicitly deleted
            // update licensee states
            registeredLicensees.forEach(InternalLicensee::onRemove);
            return;
        }
        if (license != null) {
            logger.debug("notifying [{}] listeners", registeredLicensees.size());
            switch (getLicenseState(license, clock.millis())) {
                case ENABLED:
                    for (InternalLicensee licensee : registeredLicensees) {
                        licensee.onChange(license, LicenseState.ENABLED);
                    }
                    logger.debug("license [{}] - valid", license.uid());
                    break;
                case GRACE_PERIOD:
                    for (InternalLicensee licensee : registeredLicensees) {
                        licensee.onChange(license, LicenseState.GRACE_PERIOD);
                    }
                    logger.warn("license [{}] - grace", license.uid());
                    break;
                case DISABLED:
                    for (InternalLicensee licensee : registeredLicensees) {
                        licensee.onChange(license, LicenseState.DISABLED);
                    }
                    logger.warn("license [{}] - expired", license.uid());
                    break;
            }
        }
    }

    static LicenseState getLicenseState(final License license, long time) {
        if (license == null) {
            return LicenseState.DISABLED;
        }
        if (license.issueDate() > time) {
            return LicenseState.DISABLED;
        }
        if (license.expiryDate() > time) {
            return LicenseState.ENABLED;
        }
        if ((license.expiryDate() + GRACE_PERIOD_DURATION.getMillis()) > time) {
            return LicenseState.GRACE_PERIOD;
        }
        return LicenseState.DISABLED;
    }

    /**
     * Notifies registered licensees of license state change and/or new active license
     * based on the license in <code>currentLicensesMetaData</code>.
     * Additionally schedules license expiry notifications and event callbacks
     * relative to the current license's expiry
     */
    void onUpdate(final LicensesMetaData currentLicensesMetaData) {
        final License license = getLicense(currentLicensesMetaData);
        // license can be null if the trial license is yet to be auto-generated
        // in this case, it is a no-op
        if (license != null) {
            notifyLicensees(license);
            if (license.equals(currentLicense.get()) == false) {
                currentLicense.set(license);
                scheduler.add(new SchedulerEngine.Job(LICENSE_JOB, new LicenseSchedule(license)));
                for (ExpirationCallback expirationCallback : expirationCallbacks) {
                    scheduler.add(new SchedulerEngine.Job(expirationCallback.getId(),
                            (startTime, now) ->
                                    expirationCallback.nextScheduledTimeForExpiry(license.expiryDate(), startTime, now)));
                }
            }
        }
    }

    private void initLicensee(Licensee licensee) {
        logger.debug("initializing licensee [{}]", licensee.id());
        final ClusterState clusterState = clusterService.state();
        if (clusterService.lifecycleState() == Lifecycle.State.STARTED
                && clusterState.blocks().hasGlobalBlock(GatewayService.STATE_NOT_RECOVERED_BLOCK) == false
                && clusterState.nodes().getMasterNode() != null) {
            final LicensesMetaData currentMetaData = clusterState.metaData().custom(LicensesMetaData.TYPE);
            if (clusterState.getNodes().isLocalNodeElectedMaster() &&
                (currentMetaData == null || currentMetaData.getLicense() == null)) {
                // triggers a cluster changed event
                // eventually notifying the current licensee
                registerTrialLicense();
            } else {
                notifyLicensees(currentMetaData.getLicense());
            }
        }
    }

    License getLicense(final LicensesMetaData metaData) {
        if (metaData != null) {
            License license = metaData.getLicense();
            if (license == LicensesMetaData.LICENSE_TOMBSTONE) {
                return license;
            } else {
                boolean autoGeneratedLicense = License.isAutoGeneratedLicense(license.signature());
                if ((autoGeneratedLicense && TrialLicense.verify(license))
                        || (!autoGeneratedLicense && LicenseVerifier.verifyLicense(license))) {
                    return license;
                }
            }
        }
        return null;
    }

    /**
     * Stores acknowledgement, expiration and license notification callbacks
     * for a registered listener
     */
    private class InternalLicensee {
        volatile License currentLicense = null;
        volatile LicenseState currentLicenseState = LicenseState.DISABLED;
        private final Licensee licensee;

        private InternalLicensee(Licensee licensee) {
            this.licensee = licensee;
        }

        @Override
        public String toString() {
            return "(listener: " + licensee.id() + ", state: " + currentLicenseState.name() + ")";
        }

        public String id() {
            return licensee.id();
        }

        public String[] expirationMessages() {
            return licensee.expirationMessages();
        }

        public String[] acknowledgmentMessages(License currentLicense, License newLicense) {
            return licensee.acknowledgmentMessages(currentLicense, newLicense);
        }

        public void onChange(License license, LicenseState state) {
            synchronized (this) {
                if (currentLicense == null // not yet initialized
                        || !currentLicense.equals(license)  // current license has changed
                        || currentLicenseState != state) { // same license but state has changed
                    logger.debug("licensee [{}] notified", licensee.id());
                    licensee.onChange(new Licensee.Status(license.operationMode(), state));
                    currentLicense = license;
                    currentLicenseState = state;
                }
            }
        }

        public void onRemove() {
            synchronized (this) {
                if (currentLicense != null || currentLicenseState != LicenseState.DISABLED) {
                    currentLicense = null;
                    currentLicenseState = LicenseState.DISABLED;
                    licensee.onChange(Licensee.Status.MISSING);
                }
            }
        }
    }
}