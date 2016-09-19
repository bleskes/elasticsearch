package org.elasticsearch.xpack.prelert.action;

import org.elasticsearch.action.support.master.MasterNodeOperationRequestBuilder;
import org.elasticsearch.client.ElasticsearchClient;

public class PutJobRequestBuilder extends MasterNodeOperationRequestBuilder<PutJobRequest, PutJobResponse, PutJobRequestBuilder> {

    public PutJobRequestBuilder(ElasticsearchClient client, PutJobAction action) {
        super(client, action, new PutJobRequest());
    }
}
