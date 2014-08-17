package org.elasticsearch.alerting;

import org.elasticsearch.ElasticsearchIllegalArgumentException;
import org.elasticsearch.client.Client;

import java.util.Map;

/**
 * Created by brian on 8/17/14.
 */
public class IndexAlertActionFactory implements AlertActionFactory {



    @Override
    public AlertAction createAction(Object parameters) {
        try {
            if (parameters instanceof Map) {
                Map<String, Object> paramMap = (Map<String, Object>) parameters;
                String index = paramMap.get("index").toString();
                String type = paramMap.get("type").toString();
                return new IndexAlertAction(index, type);
            } else {
                throw new ElasticsearchIllegalArgumentException("Unable to parse [" + parameters + "] as an EmailAlertAction");
            }
        } catch (Throwable t){
            throw new ElasticsearchIllegalArgumentException("Unable to parse [" + parameters + "] as an EmailAlertAction");
        }
    }
}
