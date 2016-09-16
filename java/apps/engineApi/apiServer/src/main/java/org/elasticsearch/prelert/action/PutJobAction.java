package org.elasticsearch.prelert.action;

import org.elasticsearch.action.Action;
import org.elasticsearch.client.ElasticsearchClient;

public class PutJobAction extends Action<PutJobRequest, PutJobResponse, PutJobRequestBuilder> {

    public static final PutJobAction INSTANCE = new PutJobAction();
    public static final String NAME = "cluster:admin/prelert/job/put";

    public PutJobAction() {
        super(NAME);
    }

    @Override
    public PutJobRequestBuilder newRequestBuilder(ElasticsearchClient client) {
        return new PutJobRequestBuilder(client, this);
    }

    @Override
    public PutJobResponse newResponse() {
        return new PutJobResponse();
    }
}
