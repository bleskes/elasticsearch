/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2016     *
 *                                                          *
 *----------------------------------------------------------*
 *----------------------------------------------------------*
 * WARNING:                                                 *
 * THIS FILE CONTAINS UNPUBLISHED PROPRIETARY               *
 * SOURCE CODE WHICH IS THE PROPERTY OF PRELERT LTD AND     *
 * PARENT OR SUBSIDIARY COMPANIES.                          *
 * PLEASE READ THE FOLLOWING AND TAKE CAREFUL NOTE:         *
 *                                                          *
 * This source code is confidential and any person who      *
 * receives a copy of it, or believes that they are viewing *
 * it without permission is asked to notify Prelert Ltd     *
 * on +44 (0)20 3567 1249 or email to legal@prelert.com.    *
 * All intellectual property rights in this source code     *
 * are owned by Prelert Ltd.  No part of this source code   *
 * may be reproduced, adapted or transmitted in any form or *
 * by any means, electronic, mechanical, photocopying,      *
 * recording or otherwise.                                  *
 *                                                          *
 *----------------------------------------------------------*
 *                                                          *
 *                                                          *
 ************************************************************/

package com.prelert.rs.resources;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.Application;

import org.apache.log4j.Logger;

import com.prelert.job.alert.manager.AlertManager;
import com.prelert.job.logging.DefaultJobLoggerFactory;
import com.prelert.job.logging.JobLoggerFactory;
import com.prelert.job.manager.ActivityAudit;
import com.prelert.job.manager.JobManager;
import com.prelert.job.manager.actions.Action;
import com.prelert.job.manager.actions.ActionGuardian;
import com.prelert.job.manager.actions.LocalActionGuardian;
import com.prelert.job.manager.actions.NoneActionGuardian;
import com.prelert.job.manager.actions.ScheduledAction;
import com.prelert.job.manager.actions.zookeeper.ZooKeeperActionGuardian;
import com.prelert.job.messages.Messages;
import com.prelert.job.password.PasswordManager;
import com.prelert.job.persistence.JobProvider;
import com.prelert.job.persistence.OldDataRemover;
import com.prelert.job.process.ProcessCtrl;
import com.prelert.job.process.autodetect.ProcessFactory;
import com.prelert.job.process.autodetect.ProcessManager;
import com.prelert.job.reader.JobDataReader;
import com.prelert.rs.data.extraction.DataExtractorFactoryImpl;
import com.prelert.rs.persistence.ElasticsearchFactory;
import com.prelert.rs.persistence.ElasticsearchNodeClientFactory;
import com.prelert.rs.persistence.ElasticsearchTransportClientFactory;
import com.prelert.rs.provider.AcknowledgementWriter;
import com.prelert.rs.provider.AlertMessageBodyWriter;
import com.prelert.rs.provider.DataCountsWriter;
import com.prelert.rs.provider.DataUploadExceptionMapper;
import com.prelert.rs.provider.DetectorMessageBodyReader;
import com.prelert.rs.provider.ElasticsearchExceptionMapper;
import com.prelert.rs.provider.EngineStatusWriter;
import com.prelert.rs.provider.JobConfigurationMessageBodyReader;
import com.prelert.rs.provider.JobExceptionMapper;
import com.prelert.rs.provider.MultiDataPostResultWriter;
import com.prelert.rs.provider.NativeProcessRunExceptionMapper;
import com.prelert.rs.provider.PaginationWriter;
import com.prelert.rs.provider.SingleDocumentWriter;
import com.prelert.rs.provider.TransformConfigArrayMessageBodyReader;
import com.prelert.rs.provider.TransformConfigMessageBodyReader;
import com.prelert.server.info.ServerInfoFactory;
import com.prelert.server.info.ServerInfoWriter;
import com.prelert.settings.PrelertSettings;
import com.prelert.utils.scheduler.TaskScheduler;

/**
 * Web application class contains the singleton objects accessed by the
 * resource classes
 */

public class PrelertWebApp extends Application
{
    private static final Logger LOGGER = Logger.getLogger(PrelertWebApp.class);

    public static final String ES_CLUSTER_NAME_PROP = "es.cluster.name";
    public static final String DEFAULT_CLUSTER_NAME = "prelert";

    public static final String ES_TRANSPORT_PORT_RANGE = "es.transport.tcp.port";
    private static final String DEFAULT_ES_TRANSPORT_PORT_RANGE = "9300-9400";

    public static final String ES_NETWORK_PUBLISH_HOST_PROP = "es.network.publish_host";
    private static final String DEFAULT_NETWORK_PUBLISH_HOST = "127.0.0.1";

    private static final String ES_PROCESSORS_PROP = "es.processors";

    private static final String IGNORE_DOWNTIME_ON_STARTUP_PROP = "ignore.downtime.on.startup";
    private static final boolean DEFAULT_IGNORE_DOWNTIME_ON_STARTUP = true;

    private static final String ENCRYPTION_KEY_FILE = "aes.key";
    private static final String ENCRYPTION_TRANSFORMATION = "AES/CBC/PKCS5Padding";
    // The size of this array may need to be changed if the transformation on the line above is changed
    private static final byte[] DEV_KEY_BYTES = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };

    public static final String ZOOKEEPER_HOST_PROP = "zookeeper.host";
    public static final String ZOOKEEPER_PORT_PROP = "zookeeper.port";

    /**
     * This property specifies the client that should be used to connect
     * to the storage of the results.
     *
     * Available options:
     * <ul>
     * <li> <b>es-node</b> will connect to the es.host via a node client
     * <li> <b>es-transport</b> will create a transport client that is aware of the nodes specified in es.host
     * <li> <b>es-auto</b> will choose es-node if es.host is localhost or localhost6; otherwise es-transport
     * </ul>
     */
    private static final String RESULTS_STORAGE_CLIENT_PROP = "results.storage.client";
    private static final String ES_NODE = "es-node";
    private static final String ES_TRANSPORT = "es-transport";
    private static final String ES_AUTO = "es-auto";

    public static final String PERSIST_RECORDS = "persist.records";

    private static final String SERVER_INFO_FILE =  "server.json";

    private static final String ENGINE_API_DIR = "engine_api";

    /** Remove old results at 30 minutes past midnight */
    private static final long OLD_RESULTS_REMOVAL_PAST_MIDNIGHT_OFFSET_MINUTES = 30L;

    private Set<Class<?>> m_ResourceClasses;
    private Set<Object> m_Singletons;

    private JobManager m_JobManager;
    private AlertManager m_AlertManager;
    private ServerInfoFactory m_ServerInfo;
    private JobDataReader m_JobDataReader;


    private ScheduledExecutorService m_ServerStatsSchedule;
    private TaskScheduler m_OldDataRemoverSchedule;

    private final ShutdownThreadBuilder m_ShutdownThreadBuilder;

    private final ActivityAudit m_ActivityAudit;

    public PrelertWebApp()
    {
        m_ResourceClasses = new HashSet<>();
        addEndPoints();
        addMessageReaders();
        addMessageWriters();
        addExceptionMappers();
        m_ShutdownThreadBuilder = new ShutdownThreadBuilder();

        ElasticsearchFactory esFactory = createPersistenceFactory();
        JobProvider jobProvider = esFactory.newJobProvider();

        m_JobManager = createJobManager(jobProvider, esFactory,
                new DefaultJobLoggerFactory(ProcessCtrl.LOG_DIR));
        m_AlertManager = new AlertManager(jobProvider, m_JobManager);
        m_ServerInfo = esFactory.newServerInfoFactory();
        m_JobDataReader = new JobDataReader(jobProvider);

        writeServerInfoDailyStartingNow();
        scheduleOldDataRemovalAtMidnight(jobProvider, esFactory);
        restartJobManager();

        m_Singletons = new HashSet<>();
        m_Singletons.add(m_JobManager);
        m_Singletons.add(m_AlertManager);
        m_Singletons.add(m_ServerInfo);
        m_Singletons.add(m_JobDataReader);

        m_ShutdownThreadBuilder.addTask(m_JobManager);
        // The job provider must be the last shutdown task, as earlier shutdown
        // tasks may depend on it
        m_ShutdownThreadBuilder.addTask(jobProvider);
        Runtime.getRuntime().addShutdownHook(m_ShutdownThreadBuilder.build());

        m_JobManager.systemAudit().info(Messages.getMessage(Messages.SYSTEM_AUDIT_STARTED));

        m_ActivityAudit = new ActivityAudit(() -> m_JobManager.systemAudit(),
                                            () -> m_JobManager.getAllJobs(),
                                            () -> m_JobManager.getActiveJobs());
        m_ActivityAudit.scheduleNextAudit();
    }

    private ElasticsearchFactory createPersistenceFactory()
    {
        String esHost = PrelertSettings.getSettingOrDefault(ProcessCtrl.ES_HOST_PROP, ProcessCtrl.DEFAULT_ES_HOST);
        String clusterName = PrelertSettings.getSettingOrDefault(ES_CLUSTER_NAME_PROP, DEFAULT_CLUSTER_NAME);
        String portRange = PrelertSettings.getSettingOrDefault(ES_TRANSPORT_PORT_RANGE, DEFAULT_ES_TRANSPORT_PORT_RANGE);

        String resultsStorageClient = PrelertSettings.getSettingOrDefault(RESULTS_STORAGE_CLIENT_PROP, ES_AUTO);
        // Treat any unknown values as though they were es-auto
        if (!(resultsStorageClient.equals(ES_TRANSPORT) || resultsStorageClient.equals(ES_NODE)))
        {
            // We deliberately DON'T try to detect when es.host is set to the
            // hostname of the current machine, as this scenario is taken to
            // mean that Elasticsearch is running on the current host but is
            // being managed independently of the Engine API
            if ("localhost".equals(esHost) || "localhost6".equals(esHost))
            {
                resultsStorageClient = ES_NODE;
            }
            else
            {
                resultsStorageClient = ES_TRANSPORT;
            }
            LOGGER.info("Set " + RESULTS_STORAGE_CLIENT_PROP + " to " +
                    resultsStorageClient + " because " +
                    ProcessCtrl.ES_HOST_PROP + " is set to " + esHost);
        }

        if (resultsStorageClient.equals(ES_TRANSPORT))
        {
            String esHostAndPort = esHost + ":" + portRange.split("-", 2)[0];
            LOGGER.info("Connecting to Elasticsearch via transport client to " + esHostAndPort);
            return ElasticsearchTransportClientFactory.create(esHostAndPort, clusterName);
        }

        LOGGER.info("Connecting to Elasticsearch via node client");
        // The number of processors affects the size of ES thread pools, so it
        // can sometimes be desirable to frig it
        String numProcessors = PrelertSettings.getSettingOrDefault(ES_PROCESSORS_PROP, "");
        String networkPublishHost = PrelertSettings.getSettingOrDefault(
                ES_NETWORK_PUBLISH_HOST_PROP, DEFAULT_NETWORK_PUBLISH_HOST);
        return ElasticsearchNodeClientFactory.create(esHost, networkPublishHost, clusterName, portRange, numProcessors);
    }

    private JobManager createJobManager(JobProvider jobProvider, ElasticsearchFactory esFactory,
            JobLoggerFactory jobLoggerFactory)
    {
        ActionGuardian<Action> processActionGuardian = new LocalActionGuardian<>(Action.CLOSED);
        // we don't need an ActionGuardian for scheduled jobs if
        // not in a distributed environment
        ActionGuardian<ScheduledAction> schedulerActionGuardian =
                            new NoneActionGuardian<>(ScheduledAction.STOP);

        if (PrelertSettings.isSet(ZOOKEEPER_HOST_PROP))
        {
            String host = PrelertSettings.getSettingOrDefault(ZOOKEEPER_HOST_PROP, "localhost");
            int port = PrelertSettings.getSettingOrDefault(ZOOKEEPER_PORT_PROP, 2181);
            LOGGER.info("Using ZooKeeper server on " + host + ":" + port);

            processActionGuardian = new LocalActionGuardian<>(Action.CLOSED,
                       new ZooKeeperActionGuardian<>(Action.CLOSED, host, port));
            schedulerActionGuardian =
                       new ZooKeeperActionGuardian<>(ScheduledAction.STOP, host, port);
        }

        PasswordManager passwordManager = createPasswordManager();
        return new JobManager(jobProvider,
                createProcessManager(jobProvider, esFactory, jobLoggerFactory),
                new DataExtractorFactoryImpl(passwordManager), jobLoggerFactory,
                passwordManager, esFactory.newJobDataDeleterFactory(),
                processActionGuardian, schedulerActionGuardian);
    }

    private PasswordManager createPasswordManager()
    {
        PasswordManager passwordManager = null;
        try
        {
            File keyFile = new File(ProcessCtrl.CONFIG_DIR, ENCRYPTION_KEY_FILE);
            try
            {
                return new PasswordManager(ENCRYPTION_TRANSFORMATION, keyFile);
            }
            catch (IOException ioe)
            {
                LOGGER.error("Problem reading encryption key file " +
                        keyFile.getAbsolutePath(), ioe);
            }
            // The installer will always create an encryption key, so we only
            // get here when developers run the API outside of an installed
            // build OR if a customer deletes their encryption key file.
            passwordManager = new PasswordManager(ENCRYPTION_TRANSFORMATION,
                    DEV_KEY_BYTES);
            LOGGER.warn("Falling back to internal development encryption key - " +
                    "ENCRYPTED PASSWORDS ARE NOT SECURE!");
            LOGGER.warn("Create a " + DEV_KEY_BYTES.length + " byte file " +
                    keyFile.getAbsolutePath() + " with minimal read permissions " +
                    "to ensure your passwords are securely encrypted");
        }
        catch (NoSuchAlgorithmException nsae)
        {
            // This should never happen outside dev as the JVM spec says all
            // JVMs must support AES/CBC/PKCS5Padding
            LOGGER.error("Encryption algorithm " + ENCRYPTION_TRANSFORMATION +
                    " not supported", nsae);
        }

        return passwordManager;
    }

    private void restartJobManager()
    {
        if (PrelertSettings.getSettingOrDefault(IGNORE_DOWNTIME_ON_STARTUP_PROP,
                DEFAULT_IGNORE_DOWNTIME_ON_STARTUP))
        {
            m_JobManager.setIgnoreDowntimeToAllJobs();
        }
        m_JobManager.setupScheduledJobs();
    }

    private void addEndPoints()
    {
        m_ResourceClasses.add(ApiBase.class);
        m_ResourceClasses.add(AlertsLongPoll.class);
        m_ResourceClasses.add(Buckets.class);
        m_ResourceClasses.add(CategoryDefinitions.class);
        m_ResourceClasses.add(Schedulers.class);
        m_ResourceClasses.add(Data.class);
        m_ResourceClasses.add(DataLoad.class);
        m_ResourceClasses.add(Jobs.class);
        m_ResourceClasses.add(Influencers.class);
        m_ResourceClasses.add(Logs.class);
        m_ResourceClasses.add(ModelSnapshots.class);
        m_ResourceClasses.add(Preview.class);
        m_ResourceClasses.add(Records.class);
        m_ResourceClasses.add(Validate.class);
        m_ResourceClasses.add(Status.class);
        m_ResourceClasses.add(Support.class);
    }

    private void addMessageReaders()
    {
        m_ResourceClasses.add(DetectorMessageBodyReader.class);
        m_ResourceClasses.add(JobConfigurationMessageBodyReader.class);
        m_ResourceClasses.add(TransformConfigMessageBodyReader.class);
        m_ResourceClasses.add(TransformConfigArrayMessageBodyReader.class);
    }

    private void addMessageWriters()
    {
        m_ResourceClasses.add(AcknowledgementWriter.class);
        m_ResourceClasses.add(AlertMessageBodyWriter.class);
        m_ResourceClasses.add(DataCountsWriter.class);
        m_ResourceClasses.add(EngineStatusWriter.class);
        m_ResourceClasses.add(MultiDataPostResultWriter.class);
        m_ResourceClasses.add(PaginationWriter.class);
        m_ResourceClasses.add(SingleDocumentWriter.class);
    }

    private void addExceptionMappers()
    {
        m_ResourceClasses.add(ElasticsearchExceptionMapper.class);
        m_ResourceClasses.add(NativeProcessRunExceptionMapper.class);
        m_ResourceClasses.add(JobExceptionMapper.class);
        m_ResourceClasses.add(DataUploadExceptionMapper.class);
    }

    private static ProcessManager createProcessManager(JobProvider jobProvider,
            ElasticsearchFactory esFactory, JobLoggerFactory jobLoggerFactory)
    {
        ProcessFactory processFactory = new ProcessFactory(
                jobProvider,
                esFactory.newResultsReaderFactory(jobProvider),
                esFactory.newJobDataCountsPersisterFactory(),
                esFactory.newUsagePersisterFactory(),
                jobLoggerFactory);
        return new ProcessManager(jobProvider, processFactory, esFactory.newDataPersisterFactory(),
                jobLoggerFactory);
    }

    /**
     * Get the path to the main server log directory.
     * This defaults to $PRELERT_HOME/logs/engine_api, but
     * can be relocated.
     *
     * This method attempts to ensure that the directory exists before
     * returning, but if this proves impossible it will not throw an exception,
     * but will return the path to the non-existent directory.
     */
    public static Path getServerLogPath()
    {
        Path serverLogPath = Paths.get(ProcessCtrl.LOG_DIR, ENGINE_API_DIR);
        try
        {
            Files.createDirectories(serverLogPath);
        }
        catch (IOException e)
        {
            LOGGER.error("Error creating log file directory", e);
        }
        return serverLogPath;
    }

    private void writeServerInfoDailyStartingNow()
    {
        File serverInfoFile = new File(getServerLogPath().toString(), SERVER_INFO_FILE);

        ServerInfoWriter writer = new ServerInfoWriter(m_ServerInfo, serverInfoFile);
        writer.writeInfo();
        writer.writeStats();

        scheduleServerStatsDump(serverInfoFile);
    }

    /**
     * Starts a ScheduledExecutorService to write the server stats
     * every 24 hours.
     * To stop the scheduled executor keeping the JVM alive it has to
     * run in a daemon thread.
     *
     * @param file
     */
    private void scheduleServerStatsDump(File file)
    {
        // Create with a daemon thread factory
        m_ServerStatsSchedule = Executors.newSingleThreadScheduledExecutor(
                        (Runnable r) -> {
                            Thread thread = Executors.defaultThreadFactory().newThread(r);
                            thread.setDaemon(true);
                            thread.setName("Server-Stats-Writer-Thread");
                            return thread;
                        });

        ServerInfoWriter writer = new ServerInfoWriter(m_ServerInfo, file);

        LocalDate tomorrow = LocalDate.now(ZoneId.systemDefault()).plusDays(1L);
        ZonedDateTime tomorrorwMidnight = ZonedDateTime.of(tomorrow, LocalTime.MIDNIGHT,
                                                    ZoneId.systemDefault());

        ZonedDateTime now = ZonedDateTime.now();
        long delaySeconds = now.until(tomorrorwMidnight, ChronoUnit.SECONDS);

        m_ServerStatsSchedule.scheduleAtFixedRate(() -> writer.writeStats(),
                                                   delaySeconds, 3600L * 24L, TimeUnit.SECONDS);
    }

    private void scheduleOldDataRemovalAtMidnight(JobProvider jobProvider,
            ElasticsearchFactory esFactory)
    {
        OldDataRemover oldDataRemover = new OldDataRemover(jobProvider,
                esFactory.newJobDataDeleterFactory());
        m_OldDataRemoverSchedule = TaskScheduler
                .newMidnightTaskScheduler(() -> oldDataRemover.removeOldData(),
                        OLD_RESULTS_REMOVAL_PAST_MIDNIGHT_OFFSET_MINUTES);
        m_OldDataRemoverSchedule.start();
    }

    @Override
    public Set<Class<?>> getClasses()
    {
        return m_ResourceClasses;
    }

    @Override
    public Set<Object> getSingletons()
    {
        return m_Singletons;
    }
}
