package com.toufuchew.cardocr;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.toufuchew.cardocr.async.ProgressAsyncWork;
import com.toufuchew.cardocr.async.ProgressWork;
import com.toufuchew.cardocr.camera.CameraActivity;
import com.toufuchew.cardocr.idcard.ocr.TessBaseApi;
import com.toufuchew.cardocr.tools.CommonUtils;
import com.toufuchew.cardocr.tools.RequestPermissionsAssistant;
import com.toufuchew.cardocr.tools.RequestPermissionsTool;
import com.toufuchew.cardocr.tools.ScanAssistant;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;


public class MainActivity extends AppCompatActivity {
    public final static int REQUEST_IMAGE_CAPTURE = 1;

    private TextView mTextMessage;

    private ProgressBar mProgressBar;

    private RequestPermissionsTool requestPermissionsTool;

    private String lastJPEGName;

    private ScanAssistant scanAssistant;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_picture:
                    mTextMessage.setText(R.string.picture);
                    return true;
                case R.id.navigation_scan:
                    mTextMessage.setText(R.string.ocr);
                    doScan();
                    return true;
                case R.id.navigation_about:
                    mTextMessage.setText(R.string.about);
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissionsTool = new RequestPermissionsAssistant();
            if (hasRequestedPermissions()) {
                CommonUtils.emptyDirectory(CommonUtils.APP_PATH);
            }
        }
        prepareTessData();
    }

    private void initView() {
        mTextMessage = (TextView) findViewById(R.id.message);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        mProgressBar = (ProgressBar) findViewById(R.id.ocr_progressbar);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void prepareTessData() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            new doTessPrepare().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            new doTessPrepare().execute();
        }
    }

    private void doScan() {
        Intent cameraIntent = new Intent(MainActivity.this, CameraActivity.class);
        lastJPEGName = CommonUtils.APP_PATH + System.currentTimeMillis() + ".jpg";
        cameraIntent.putExtra("shot", lastJPEGName);
        startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE);
        /**
        new Thread(new Runnable() {
            @Override
            public void run() {
                ScanAssistant scanAssistant = new ScanAssistant(null);
                scanAssistant.scan();
            }
        }).start();
         **/
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            // the image has been saved in storage after shooting
            BitmapFactory.Options options = new BitmapFactory.Options();
            Bitmap bitmap = BitmapFactory.decodeFile(lastJPEGName, options);

            if (bitmap == null) {
                Log.e(CommonUtils.TAG, "onActivityResult warning: bitmap is null");
                return;
            }
            workOCR(bitmap);
        }
    }

    private void workOCR(final Bitmap bitmapToOCR) {
        scanAssistant.setBitmapToScan(bitmapToOCR);

        final FutureTask<String> task;
        ProgressWork<String> progressWork;
        // set bar visible
        mProgressBar.setVisibility(View.VISIBLE);
        new Thread(task = new FutureTask<>(new Callable<String>() {
            @Override
            public String call() throws Exception {
                // start ocr
                boolean success = scanAssistant.scan();
                if (success) {
                    return scanAssistant.getIDString();
                }
                return "卡片无法识别，请重新拍摄";
            }
        })).start();

        progressWork = new ProgressWork<String>() {
            @Override
            public Future<String> doInBackground() {
                return task;
            }

            @Override
            public int updateProgress() {
                return scanAssistant.getProgress();
            }

            @Override
            public void callBackResult(String result) {
                mTextMessage.setText(result);
            }
        };
        // begin move the progressbar
        new ProgressAsyncWork<String>(progressWork, mProgressBar).work();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case RequestPermissionsTool.MULTIPLE_PERMISSIONS: {
                boolean grantedAll = true;
                for (int grantResult : grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        grantedAll = false;
                        break;
                    }
                }
                if (grantResults.length != permissions.length || !grantedAll) {
                    requestPermissionsTool.onPermissionDenied(this);
                    this.finish();
                    return;
                }
                break;
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * @see RequestPermissionsTool
     * @return
     */
    private boolean hasRequestedPermissions() {
        String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        return requestPermissionsTool.requestPermissions(this, permissions);
    }

    private class doTessPrepare extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {
            try {
                // prepare ocr working
                CommonUtils.prepareDirectory(CommonUtils.APP_PATH);
                TessBaseApi.copyTessDataFiles(getAssets());
                scanAssistant = new ScanAssistant();
            } catch (Exception e) {
                CommonUtils.info("doTessPrepare failed, cannot init data for OCR, message: " + e.getMessage());
            }
            return "";
        }

        @Override
        protected void onPostExecute(String s) {
        }
    }
}
