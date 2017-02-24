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
package org.elasticsearch.xpack;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.test.ESSingleNodeTestCase;
import org.elasticsearch.xpack.ml.MachineLearning;

/**
 * An extention to {@link ESSingleNodeTestCase} that adds node settings specifically needed for x-pack
 */
public abstract class XPackSingleNodeTestCase extends ESSingleNodeTestCase {

    @Override
    protected Settings nodeSettings()  {
        Settings.Builder newSettings = Settings.builder();
        // Disable native ML autodetect_process as the c++ controller won't be available
        newSettings.put(MachineLearning.AUTODETECT_PROCESS.getKey(), false);
        return newSettings.build();
    }
}
