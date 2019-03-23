package com.toufuchew.cardocr.camera;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.toufuchew.cardocr.R;
import com.toufuchew.cardocr.camera.view.CameraSurfaceView;
import com.toufuchew.cardocr.camera.view.PictureView;

import java.io.File;

public class CameraActivity extends AppCompatActivity implements View.OnClickListener, PictureView{
    static final int REQUEST_IMAGE_CAPTURE = 1;

    private Button mShootButton;

    private Button mCancelButton;

    private CameraSurfaceView cameraSurfaceView;

    private Button mOCRButton;

    private ImageView mImageView;

    private TextView mTextView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // set land orientation
//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        // set full screen mode
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_camera);

        initView();
    }

    private void initView() {
        cameraSurfaceView = (CameraSurfaceView) findViewById(R.id.surface_view);
        cameraSurfaceView.setPictureView(this);

        mShootButton = (Button) findViewById(R.id.btn_shoot);
        mCancelButton = (Button) findViewById(R.id.btn_cancle);
        mOCRButton = (Button) findViewById(R.id.btn_ocr);
        mImageView = (ImageView) findViewById(R.id.image_view);
        mTextView = (TextView) findViewById(R.id.focus_hint);
        mShootButton.setOnClickListener(this);
        mCancelButton.setOnClickListener(this);
        mOCRButton.setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_shoot:
                takePicture();
                break;
            case R.id.btn_cancle:
                endReview();
                break;
            case R.id.btn_ocr:
                processPicture();
                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // restart preview
        cameraSurfaceView.resume();
    }

    private void takePicture() {
        final String JPEG = getShotResource();
        cameraSurfaceView.takePicture(JPEG);
    }

    private void addToGallery() {
        final String JPEG = getShotResource();
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri uri = Uri.fromFile(new File(JPEG));
        intent.setData(uri);
        sendBroadcast(intent);
    }

    private String getShotResource() {
        Bundle b = getIntent().getExtras();
        return b.getString("shot");
    }

    private void processPicture() {
        setResult(Activity.RESULT_OK);
        addToGallery();
        finishActivity(REQUEST_IMAGE_CAPTURE);
        finish();
    }

    @Override
    public void startReview(Bitmap bmp) {
//        mImageView.setImageBitmap(bmp);
        // set control tools visible
//        mImageView.setVisibility(View.VISIBLE);

        mOCRButton.setVisibility(View.VISIBLE);
        mCancelButton.setVisibility(View.VISIBLE);
        // hide surface view
//        cameraSurfaceView.setVisibility(View.INVISIBLE);
        mShootButton.setVisibility(View.INVISIBLE);
        mTextView.setVisibility(View.GONE);
    }

    @Override
    public void endReview() {
//        mImageView.setVisibility(View.GONE);
//        mImageView.setImageResource(0);
        mCancelButton.setVisibility(View.GONE);
        mOCRButton.setVisibility(View.GONE);

//        cameraSurfaceView.setVisibility(View.VISIBLE);
        mShootButton.setVisibility(View.VISIBLE);
        mTextView.setVisibility(View.VISIBLE);

        cameraSurfaceView.resume();
    }
}
