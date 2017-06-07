/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2017 Elasticsearch BV. All Rights Reserved.
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.ml.action;

import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.ml.MlMetadata;
import org.elasticsearch.xpack.ml.support.BaseMlIntegTestCase;

import java.util.Date;

public class DeleteJobActionTests extends ESTestCase {

    public void testJobIsDeletedFromState() {
        MlMetadata mlMetadata = MlMetadata.EMPTY_METADATA;

        ClusterState clusterState = ClusterState.builder(new ClusterName("_name"))
                .metaData(new MetaData.Builder().putCustom(MlMetadata.TYPE, mlMetadata))
                .build();

        assertTrue(DeleteJobAction.TransportAction.jobIsDeletedFromState("job_id_1", clusterState));

        MlMetadata.Builder mlBuilder = new MlMetadata.Builder();
        mlBuilder.putJob(BaseMlIntegTestCase.createScheduledJob("job_id_1").build(new Date()), false);
        mlMetadata = mlBuilder.build();
        clusterState = ClusterState.builder(new ClusterName("_name"))
                .metaData(new MetaData.Builder().putCustom(MlMetadata.TYPE, mlMetadata))
                .build();

        assertFalse(DeleteJobAction.TransportAction.jobIsDeletedFromState("job_id_1", clusterState));
    }
}
