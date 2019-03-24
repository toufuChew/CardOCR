package com.toufuchew.cardocr.async;

import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ProgressBar;

import com.toufuchew.cardocr.tools.CommonUtils;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class ProgressAsyncWork<V> {

    ProgressWork progressWork;

    Handler postHandler;

    Runnable uiTask;

    Future<V> task;

    ProgressBar mProgressBar;

    private V result;

    public ProgressAsyncWork(ProgressWork progressWork, ProgressBar progressBar) {
        this.progressWork = progressWork;
        mProgressBar = progressBar;
    }

    public void work() {
        // get the work thread doing in background
        task = progressWork.doInBackground();

        postHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                mProgressBar.setProgress(msg.arg1);
                if (msg.arg1 >= 100 || task.isDone()) {
                    mProgressBar.setVisibility(View.GONE);
                }
                this.post(uiTask);
            }
        };

        uiTask = new Runnable() {
            @Override
            public void run() {
                do {
                    Message msg = postHandler.obtainMessage();
                    if (task.isDone()) {
                        postHandler.removeCallbacks(uiTask);
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
                    msg.arg1 = progressWork.updateProgress();
                    postHandler.sendMessage(msg);
                    CommonUtils.info("progress: " + msg.arg1);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } while (true);
            }
        };
        // start post
        postHandler.post(uiTask);
    }

    public V getResult() {
        return result;
    }
}
