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
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

import java.nio.channels.AlreadyBoundException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;


public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    public final static int REQUEST_IMAGE_CAPTURE = 1;

    private ViewGroup mLoadView;

    private ViewGroup mScanView;

    private ViewGroup mAboutView;

    private Button mClearButton;

    private ProgressBar mProgressBar;

    private RequestPermissionsTool requestPermissionsTool;

    private String lastJPEGName;

    private ScanAssistant scanAssistant;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            if (item.getItemId() == R.id.navigation_scan) {
                doScan();
            }
            return setView(item.getItemId());
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
        mLoadView = (ViewGroup) findViewById(R.id.page_load);
        mScanView = (ViewGroup) findViewById(R.id.page_scan);
        mAboutView = (ViewGroup) findViewById(R.id.page_about);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        mClearButton = (Button) findViewById(R.id.btn_clear);
        mClearButton.setOnClickListener(this);
        mProgressBar = (ProgressBar) findViewById(R.id.ocr_progressbar);
    }

    private boolean setView(int id){
        boolean set = true;
        switch (id) {
            case R.id.navigation_picture:
                mLoadView.setVisibility(View.VISIBLE);
                mScanView.setVisibility(View.GONE);
                mAboutView.setVisibility(View.GONE);
                break;
            case R.id.navigation_scan:
                mLoadView.setVisibility(View.GONE);
                mScanView.setVisibility(View.VISIBLE);
                mAboutView.setVisibility(View.GONE);
                break;
            case R.id.navigation_about:
                mLoadView.setVisibility(View.GONE);
                mScanView.setVisibility(View.GONE);
                mAboutView.setVisibility(View.VISIBLE);
                break;
            default: set = false;
        }
        return set;
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
                Thread.sleep(5000);
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
                TextView mTextMessage = (TextView) findViewById(R.id.message_scan);
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

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_clear:
                // clear cache file
                CommonUtils.emptyDirectory(CommonUtils.APP_PATH);
                Toast.makeText(this, "已清空缓存文件", Toast.LENGTH_SHORT).show();
                break;
        }
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
