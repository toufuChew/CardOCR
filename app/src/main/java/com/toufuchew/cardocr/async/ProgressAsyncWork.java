package com.toufuchew.cardocr.async;

import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ProgressBar;

import com.toufuchew.cardocr.tools.CommonUtils;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class ProgressAsyncWork<V> extends Thread{

    ProgressWork progressWork;

    /**
     * offer by the caller
     * need to overwrite doInBackground() function
     */
    Future<V> task;

    /**
     * the result returning from FutureTask
     */
    private V result;

    public ProgressAsyncWork(ProgressWork progressWork) {
        this.progressWork = progressWork;
    }

    public V getResult() {
        return result;
    }

    @Override
    public void run() {
        // get the work thread doing in background
        task = progressWork.doInBackground();
        work();
    }

    private void work() {
        do {
            if (task.isDone()) {
                // set result
                try {
                    result = task.get();
                    // callback when the work done
                    progressWork.callBackResult(result);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    break;
                }
            }
            progressWork.updateProgress();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } while (true);
    }
}
