package com.toufuchew.cardocr.async;

import java.util.concurrent.Future;

public interface ProgressWork<V> {

    /**
     * the work should be take into this method
     * @return future work
     */
    Future<V> doInBackground();

    /**
     * the worker would get progress from this method
     */
    void updateProgress();

    /**
     * the caller should implement this method to get the result</br>
     * showing on the ui view
     * @param result
     * @param timeInterval time consuming of the work
     */
    void callBackResult(V result, long timeInterval);
}
