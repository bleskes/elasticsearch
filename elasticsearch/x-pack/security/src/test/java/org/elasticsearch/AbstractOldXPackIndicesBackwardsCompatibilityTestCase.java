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

package org.elasticsearch;

import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.TestUtil;
import org.elasticsearch.cluster.routing.allocation.decider.ThrottlingAllocationDecider;
import org.elasticsearch.common.io.FileSystemUtils;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.NodeEnvironment;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.test.SecurityIntegTestCase;
import org.elasticsearch.test.VersionUtils;
import org.junit.Before;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.elasticsearch.test.OldIndexUtils.copyIndex;
import static org.elasticsearch.test.OldIndexUtils.loadDataFilesList;

/**
 * Base class for tests against clusters coming from old versions of xpack and Elasticsearch.
 */
@ESIntegTestCase.ClusterScope(scope = ESIntegTestCase.Scope.TEST, numDataNodes = 0, numClientNodes = 0) // We'll start the nodes manually
public abstract class AbstractOldXPackIndicesBackwardsCompatibilityTestCase extends SecurityIntegTestCase {
    /**
     * Set to true when it is ok to start a node. We don't want to start nodes at unexpected times.
     */
    private boolean okToStartNode = false;
    private List<String> dataFiles;

    @Override
    protected final boolean ignoreExternalCluster() {
        return true;
    }

    @Override
    protected boolean shouldAssertXPackIsInstalled() {
        return false; // Skip asserting that the xpack is installed because it tries to start the cluter.
    }

    @Override
    protected void ensureClusterSizeConsistency() {
        // We manage our nodes ourselves. At this point no node should be running anyway and this would start a new one!
    }

    @Override
    protected void ensureClusterStateConsistency() throws IOException {
        // We manage our nodes ourselves. At this point no node should be running anyway and this would start a new one!
    }

    @Before
    public final void initIndexesList() throws Exception {
        dataFiles = loadDataFilesList("x-pack", getBwcIndicesPath());
    }

    @Override
    public Settings nodeSettings(int ord) {
        if (false == okToStartNode) {
            throw new IllegalStateException("Starting nodes must only happen in setupCluster");
        }
        // speed up recoveries
        return Settings.builder()
                .put(super.nodeSettings(ord))
                .put(ThrottlingAllocationDecider
                        .CLUSTER_ROUTING_ALLOCATION_NODE_CONCURRENT_INCOMING_RECOVERIES_SETTING.getKey(), 30)
                .put(ThrottlingAllocationDecider
                        .CLUSTER_ROUTING_ALLOCATION_NODE_CONCURRENT_OUTGOING_RECOVERIES_SETTING.getKey(), 30)
                .build();
    }

    @Override
    protected int maxNumberOfNodes() {
        try {
            return SecurityIntegTestCase.defaultMaxNumberOfNodes() + loadDataFilesList("x-pack", getBwcIndicesPath()).size();
        } catch (IOException e) {
            throw new RuntimeException("couldn't enumerate bwc indices", e);
        }
    }

    public void testAllVersionsTested() throws Exception {
        SortedSet<String> expectedVersions = new TreeSet<>();
        for (Version v : VersionUtils.allVersions()) {
            if (false == shouldTestVersion(v)) continue;
            if (v.equals(Version.CURRENT)) continue; // the current version is always compatible with itself
            if (v.isBeta() == true || v.isAlpha() == true || v.isRC() == true) continue; // don't check alphas etc
            expectedVersions.add("x-pack-" + v.toString() + ".zip");
        }
        expectedVersions.removeAll(dataFiles);
        if (expectedVersions.isEmpty() == false) {
            StringBuilder msg = new StringBuilder("Old index tests are missing indexes:");
            for (String expected : expectedVersions) {
                msg.append("\n" + expected);
            }
            fail(msg.toString());
        }
    }

    public void testOldIndexes() throws Exception {
        Collections.shuffle(dataFiles, random());
        for (String dataFile : dataFiles) {
            Version version = Version.fromString(dataFile.replace("x-pack-", "").replace(".zip", ""));
            if (false == shouldTestVersion(version)) continue;
            long clusterStartTime = System.nanoTime();
            setupCluster(dataFile);
            ensureYellow();
            long testStartTime = System.nanoTime();
            try {
                checkVersion(version);
            } catch (Throwable t) {
                throw new AssertionError("Failed while checking [" + version + "]", t);
            }
            logger.info("--> Done testing [{}]. Setting up cluster took [{}] millis and testing took [{}] millis", version,
                    TimeUnit.NANOSECONDS.toMillis(testStartTime - clusterStartTime),
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - testStartTime));
        }
    }

    /**
     * Should we test this version at all? Called before loading the data directory. Return false to skip it entirely.
     */
    protected boolean shouldTestVersion(Version version) {
        return true;
    }

    /**
     * Actually test this version.
     */
    protected abstract void checkVersion(Version version) throws Exception;

    private void setupCluster(String pathToZipFile) throws Exception {
        // shutdown any nodes from previous zip files
        while (internalCluster().size() > 0) {
            internalCluster().stopRandomNode(s -> true);
        }
        // first create the data directory and unzip the data there
        // we put the whole cluster state and indexes because if we only copy indexes and import them as dangling then
        // the native realm services will start because there is no security index and nothing is recovering
        // but we want them to not start!
        Path dataPath = createTempDir();
        Settings.Builder nodeSettings = Settings.builder()
                .put("path.data", dataPath.toAbsolutePath());
        // unzip data
        Path backwardsIndex = getBwcIndicesPath().resolve(pathToZipFile);
        // decompress the index
        try (InputStream stream = Files.newInputStream(backwardsIndex)) {
            logger.info("unzipping {}", backwardsIndex.toString());
            TestUtil.unzip(stream, dataPath);
            // now we need to copy the whole thing so that it looks like an actual data path
            try (Stream<Path> unzippedFiles = Files.list(dataPath.resolve("data"))) {
                Path dataDir = unzippedFiles.findFirst().get();
                // this is not actually an index but the copy does the job anyway
                copyIndex(logger, dataDir.resolve("nodes"), "nodes", dataPath);
                // remove the original unzipped directory
            }
            IOUtils.rm(dataPath.resolve("data"));
        }

        // check it is unique
        assertTrue(Files.exists(dataPath));
        Path[] list = FileSystemUtils.files(dataPath);
        if (list.length != 1) {
            throw new IllegalStateException("Backwards index must contain exactly one node");
        }

        // start the node
        logger.info("--> Data path for importing node: {}", dataPath);
        okToStartNode = true;
        String importingNodeName = internalCluster().startNode(nodeSettings.build());
        okToStartNode = false;
        Path[] nodePaths = internalCluster().getInstance(NodeEnvironment.class, importingNodeName).nodeDataPaths();
        assertEquals(1, nodePaths.length);
    }
}
