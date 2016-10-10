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

package org.elasticsearch.xpack.prelert;

import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.bootstrap.JarHell;
import org.elasticsearch.cli.Terminal;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.internal.InternalSettingsPreparer;
import org.elasticsearch.plugins.ActionPlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.rest.RestHandler;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.search.SearchRequestParsers;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.Netty4Plugin;
import org.elasticsearch.watcher.ResourceWatcherService;
import org.elasticsearch.xpack.prelert.action.*;
import org.elasticsearch.xpack.prelert.job.metadata.JobAllocator;
import org.elasticsearch.xpack.prelert.job.metadata.JobLifeCycleService;
import org.elasticsearch.xpack.prelert.job.metadata.PrelertMetadata;
import org.elasticsearch.xpack.prelert.rest.RestClearPrelertAction;
import org.elasticsearch.xpack.prelert.rest.buckets.RestGetBucketAction;
import org.elasticsearch.xpack.prelert.rest.buckets.RestGetBucketsAction;
import org.elasticsearch.xpack.prelert.rest.data.RestPostDataAction;
import org.elasticsearch.xpack.prelert.rest.data.RestPostDataCloseAction;
import org.elasticsearch.xpack.prelert.rest.data.RestPostDataFlushAction;
import org.elasticsearch.xpack.prelert.rest.job.RestGetJobAction;
import org.elasticsearch.xpack.prelert.rest.job.RestGetJobsAction;
import org.elasticsearch.xpack.prelert.rest.job.RestPutJobsAction;
import org.elasticsearch.xpack.prelert.rest.validate.RestValidateDetectorAction;
import org.elasticsearch.xpack.prelert.rest.list.RestCreateListAction;
import org.elasticsearch.xpack.prelert.rest.list.RestGetListAction;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class ServerBootstrap {

	private static final String JETTY_PORT_PROPERTY = "jetty.port";
	private static final String JETTY_HOME_PROPERTY = "jetty.home";
	private static final String DEFAULT_JETTY_HOME = "cots/jetty";

	public static final int JETTY_PORT = 8080;

	public static void main(String[] args) throws Exception {
		JarHell.checkJarHell();
		Settings.Builder settings = Settings.builder();
		settings.put("path.home", System.getProperty(JETTY_HOME_PROPERTY, DEFAULT_JETTY_HOME));
		settings.put("http.port", System.getProperty(JETTY_PORT_PROPERTY, Integer.toString(JETTY_PORT)));
		settings.put("cluster.name", "prelert");

		CountDownLatch latch = new CountDownLatch(1);
		Node node = new PrelertNode(settings.build());
		Runtime.getRuntime().addShutdownHook(new Thread() {

			@Override
			public void run() {
				try {
					node.close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				} finally {
					latch.countDown();
				}
			}
		});
		node.start();
		latch.await();
	}

	static class PrelertNode extends Node {

		public PrelertNode(Settings settings) {
			super(
					InternalSettingsPreparer.prepareEnvironment(settings, Terminal.DEFAULT),
					Arrays.asList(PrelertPlugin.class, Netty4Plugin.class)
					);
		}
	}

	public static class PrelertPlugin extends Plugin implements ActionPlugin {

        private final Settings settings;

        static {
            MetaData.registerPrototype(PrelertMetadata.TYPE, PrelertMetadata.PROTO);
        }

        public PrelertPlugin(Settings settings) {
            this.settings = settings;
        }

		@Override
        public Collection<Object> createComponents(Client client, ClusterService clusterService, ThreadPool threadPool,
                                                   ResourceWatcherService resourceWatcherService, ScriptService scriptService,
                                                   SearchRequestParsers searchRequestParsers) {
            return Arrays.asList(
                    new PrelertServices(client, clusterService),
                    new JobAllocator(settings, clusterService, threadPool),
                    new JobLifeCycleService(settings, clusterService)
            );
		}

		@Override
		public List<Class<? extends RestHandler>> getRestHandlers() {
			return Arrays.asList(
					RestGetJobAction.class,
					RestGetJobsAction.class,
					RestPutJobsAction.class,
					RestGetListAction.class,
					RestCreateListAction.class,
					RestGetBucketsAction.class,
					RestGetBucketAction.class,
					RestPostDataAction.class,
					RestPostDataCloseAction.class,
					RestPostDataFlushAction.class,
					RestValidateDetectorAction.class,
                    RestClearPrelertAction.class);
		}

		@Override
		public List<ActionHandler<? extends ActionRequest<?>, ? extends ActionResponse>> getActions() {
			return Arrays.asList(
					new ActionHandler<>(GetJobAction.INSTANCE, GetJobAction.TransportAction.class),
					new ActionHandler<>(GetJobsAction.INSTANCE, GetJobsAction.TransportAction.class),
					new ActionHandler<>(PutJobAction.INSTANCE, PutJobAction.TransportAction.class),
					new ActionHandler<>(GetListAction.INSTANCE, GetListAction.TransportAction.class),
					new ActionHandler<>(CreateListAction.INSTANCE, CreateListAction.TransportAction.class),
					new ActionHandler<>(GetBucketsAction.INSTANCE, GetBucketsAction.TransportAction.class),
					new ActionHandler<>(GetBucketAction.INSTANCE, GetBucketAction.TransportAction.class),
					new ActionHandler<>(PostDataAction.INSTANCE, PostDataAction.TransportAction.class),
					new ActionHandler<>(PostDataCloseAction.INSTANCE, PostDataCloseAction.TransportAction.class),
					new ActionHandler<>(PostDataFlushAction.INSTANCE, PostDataFlushAction.TransportAction.class),
					new ActionHandler<>(ValidateDetectorAction.INSTANCE, ValidateDetectorAction.TransportAction.class),
                    new ActionHandler<>(ClearPrelertAction.INSTANCE, ClearPrelertAction.TransportAction.class));
		}
	}

}
