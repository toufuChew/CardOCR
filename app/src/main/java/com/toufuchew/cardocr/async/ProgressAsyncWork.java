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

    Handler postHandler;

    Future<V> task;

    private V result;

    public ProgressAsyncWork(ProgressWork progressWork, Handler postHandler) {
        this.progressWork = progressWork;
        this.postHandler = postHandler;
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
            Message msg = postHandler.obtainMessage();
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
}
