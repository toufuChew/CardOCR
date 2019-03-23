package com.toufuchew.cardocr.camera.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.toufuchew.cardocr.tools.CommonUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import static com.toufuchew.cardocr.tools.CommonUtils.TAG;
import static com.toufuchew.cardocr.tools.CommonUtils.info;

public class CameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback, Camera.AutoFocusCallback{
    private Context mContext;

    private SurfaceHolder holder;

    private Camera mCamera;

    private int mScreenWidth;
    private int mScreenHeight;

    private String outputUri;

    private PictureView review;

    public CameraSurfaceView(Context context) {
        this(context, null);
    }

    public CameraSurfaceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        initView();
    }

    public void setPictureView(PictureView pictureView) {
        review = pictureView;
    }

    public void initView() {
        info("CameraSurfaceView init");
        getScreenSize();
        holder = getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    private void getScreenSize() {
        Display display = ((WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        mScreenWidth = size.x;
        mScreenHeight = size.y;
    }

    private void setCameraParams(int width, int height) {
        Camera.Parameters parameters = mCamera.getParameters();

        //TODO: preview size
        List<Camera.Size> previewSizeList = parameters.getSupportedPreviewSizes();
        if (previewSizeList == null || previewSizeList.size() == 0) {
            info("preview size is null");
            return;
        }
        Camera.Size preSize = getBestSize(previewSizeList, (float) width / height);
        if (preSize == null) {
            preSize = previewSizeList.get(0);
        }
        parameters.setPreviewSize(preSize.width, preSize.height);
        info("pic preview size: width=" + preSize.width + ", height=" + preSize.height);

        //TODO: picture size
        List<Camera.Size> pictureSizeList = parameters.getSupportedPictureSizes();
        Camera.Size picSize = getBestSize(pictureSizeList, (float) preSize.width / preSize.height);
        if (picSize == null) {
            info("picSize is null");
            picSize = parameters.getPictureSize();
        }
        info("pic size: width=" + picSize.width + ", height=" + picSize.height);
        parameters.setPictureSize(picSize.width, picSize.height);
        this.setLayoutParams(new RelativeLayout.LayoutParams((int) (height * ((float) picSize.width / picSize.height)), height));
        // set picture quality
        parameters.setJpegQuality(100);
        if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }
        mCamera.cancelAutoFocus();
        mCamera.setDisplayOrientation(90);
        mCamera.setParameters(parameters);
    }

    private Camera.Size getBestSize(List<Camera.Size> pics, float screenRatio) {
        Camera.Size result = null;
        for (Camera.Size size : pics) {
            float curRatio = (float)size.width / size.height;
            if (result == null) {
                result = size;
            }
            if (curRatio - screenRatio == 0) {
                result = size;
                break;
            }
        }
        return result;
    }

    /**
     * take picture
     */
    private Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
        @Override
        public void onShutter() {
            info("camera shutter");
        }
    };

    /**
     * picture raw data
     */
    private Camera.PictureCallback pictureCallback_RAW = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] bytes, Camera camera) {
            info("raw picture taken");
        }
    };

    private Camera.PictureCallback pictureCallback_JPEG = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] bytes, Camera camera) {
            //screenshot
            mCamera.stopPreview();
            Bitmap crop = screenshot(bytes);
            try {

                if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    FileOutputStream out = new FileOutputStream(new File(outputUri));
                    crop.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    out.close();
                    info("Image saved at " + outputUri);
                    review.startReview(crop);
                } else {
                    String warning = "Did not detect SD card in your phone";
                    Log.e(TAG, warning);
                    Toast.makeText(mContext, warning, Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                // set stop flag
                previewing = false;
            }

        }
    };

    private Bitmap screenshot(byte[] rawData) {
        Bitmap bmp = BitmapFactory.decodeByteArray(rawData, 0, rawData.length);
        int rawWidth = bmp.getWidth();
        int rawHeight = bmp.getHeight();
        // difference from ViewShadow x, y axis
        int x_center, y_center;
        int width, height;
        x_center = rawWidth >> 1;
        y_center = rawHeight >> 1;
        height = (int)(rawHeight * ViewShadow.SCALE_RATIO);
        width = (int)(height / ViewShadow.ASPECT_RATIO);

        Matrix matrix = new Matrix();
        //rotate image by center point
        matrix.postRotate(90, x_center, y_center);
        return Bitmap.createBitmap(bmp, x_center - width / 2, y_center - height / 2, width, height, matrix, false);
    }

    /**
     * external function called by CameraActivity button
     * @param outputUri
     */
    public void takePicture(String outputUri) {
        this.outputUri = outputUri;
        if (!previewing) {
            Log.e(TAG, "takePicture error: camera preview not started");
            return;
        }
        mCamera.takePicture(shutterCallback, pictureCallback_RAW, pictureCallback_JPEG);
    }

    /**
     * start preview after reviewing shot picture
     */
    public void resume() {
        if (!previewing) {
            if (mCamera == null) {
                initView();
            } else {
                mCamera.startPreview();
                previewing = true;
            }
        }
    }
    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        if (mCamera == null) {
            mCamera = Camera.open();
        }
        info("on previewing");
    }

    private boolean previewing = false;
    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        if (previewing) {
            mCamera.stopPreview();
            info("surface changed, preview restarting...");
            previewing = false;
        }
        if (mCamera != null) {
            setCameraParams(mScreenWidth, mScreenHeight);
            try {
                mCamera.setPreviewDisplay(holder);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mCamera.startPreview();
            previewing = true;
        }

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
        holder = null;
        previewing = false;
        info("surfaceDestroyed");
    }

    @Override
    public void onAutoFocus(boolean b, Camera camera) {
        if (b) {
            info("onAutoFocus success");
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mCamera.autoFocus(this);
        return super.onTouchEvent(event);
    }
}
