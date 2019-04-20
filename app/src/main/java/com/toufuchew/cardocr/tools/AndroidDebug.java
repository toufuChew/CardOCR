package com.toufuchew.cardocr.tools;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.util.Arrays;

import static com.toufuchew.cardocr.tools.CommonUtils.APP_PATH;
import static com.toufuchew.cardocr.tools.CommonUtils.info;
import static com.toufuchew.cardocr.tools.CommonUtils.objToString;
import static org.opencv.imgcodecs.Imgcodecs.imwrite;

public final class AndroidDebug {

    public static boolean DEBUG = true; // Debug mode

    // Write debug image
    public static void writeImage(String name, Mat origin) {
        if (!DEBUG) return;
        String appPath = APP_PATH;
        info("Writing " + appPath + name + "...");
        imwrite(appPath + name, origin);
    }

    /**
     * load image resource from ``APP_PATH`` external storage
     * @param name
     * @return
     */
    public static Mat readImage(String name) {
        BitmapFactory.Options options = new BitmapFactory.Options();
		Bitmap bitmap = BitmapFactory.decodeFile(APP_PATH + name, options);
		Mat mat = new Mat();
		Utils.bitmapToMat(bitmap, mat);
		return mat;
    }

    public static void log(String tag, Object obj) {
        tag += ": ";
        tag += objToString(obj);
        info(tag);
    }

}
