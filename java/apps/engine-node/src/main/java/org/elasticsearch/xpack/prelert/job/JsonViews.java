
package org.elasticsearch.xpack.prelert.job;

/**
 * Classes used to configure Jackson's JsonView functionality.  The
 * nested static classes are effectively labels.  They are used in
 * Jackson annotations to specify that certain fields are only
 * serialised/deserialised when a particular "view" of the object is
 * desired.
 */
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
