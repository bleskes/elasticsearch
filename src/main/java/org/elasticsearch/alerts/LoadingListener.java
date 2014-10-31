package org.elasticsearch.alerts;

/**
 */
public interface LoadingListener {

    void onSuccess();

    void onFailure();

}
