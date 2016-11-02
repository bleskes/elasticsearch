/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
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
package org.elasticsearch.xpack.prelert.job;

/**
 * Classes used to configure Jackson's JsonView functionality.  The
 * nested static classes are effectively labels.  They are used in
 * Jackson annotations to specify that certain fields are only
 * serialised/deserialised when a particular "view" of the object is
 * desired.
 */
// NORELEASE remove this when Jackson is gone
public class JsonViews
{
    /**
     * Neither this class nor its nested classes are ever meant to be
     * instantiated.
     */
    private JsonViews()
    {
    }

    /**
     * View used when serialising objects for the REST API.
     */
    public static class RestApiView
    {
    }

    /**
     * View used when serialising objects for the data store.
     */
    public static class DatastoreView
    {
    }
}
