package com.toufuchew.cardocr.idcard.ocr;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.toufuchew.cardocr.tools.CommonUtils;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.io.IOException;

import static com.toufuchew.cardocr.tools.CommonUtils.APP_PATH;
import static com.toufuchew.cardocr.tools.CommonUtils.TAG;
import static com.toufuchew.cardocr.tools.CommonUtils.copyAssets;
import static com.toufuchew.cardocr.tools.CommonUtils.info;

public class TessBaseApi {

    private TessBaseAPI tessBaseAPI;

    private static final String lang = "card";

    private static final String TESSDATA = "tessdata";

    public TessBaseApi() {
        try {
            tessBaseAPI = new TessBaseAPI();
        } catch (Exception e) {
            Log.e(TAG,"Tesseract started failed, TessBaseAPI instance got null: " + e.getMessage());
        }
        try {
            tessBaseAPI.init(APP_PATH, lang);
        } catch (Exception e) {
            Log.e(TAG,"Tesseract initialized error: " + e.getMessage());
        }
    }

    public String doOCR(Mat im0) {
        Bitmap bitmap = Bitmap.createBitmap(im0.width(), im0.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(im0, bitmap);
        tessBaseAPI.setImage(bitmap);
        return tessBaseAPI.getUTF8Text();
    }

    public static void copyTessDataFiles(AssetManager assetManager) {
        // create tessdata directory
        CommonUtils.prepareDirectory(APP_PATH + TESSDATA);
        try {
            String fileList[] = assetManager.list(TESSDATA);

            for (String fileName : fileList) {
                String pathToStorageData = APP_PATH + TESSDATA + "/" + fileName;
                if (!(new File(pathToStorageData)).exists()) {
                    String pathToAssetsData = TESSDATA + "/" + fileName;
                    copyAssets(assetManager.open(pathToAssetsData), pathToStorageData);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
