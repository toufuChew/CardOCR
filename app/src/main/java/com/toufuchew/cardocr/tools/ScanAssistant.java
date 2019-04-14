package com.toufuchew.cardocr.tools;

import android.graphics.Bitmap;

import com.toufuchew.cardocr.idcard.ocr.Recognizer;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

public class ScanAssistant extends Recognizer {

    public ScanAssistant() {
        /**
         * initialize openCV lib
         */
        super();
    }

    public void setBitmapToScan(Bitmap bitmapToScan) {
        Mat matToOCR = new Mat();
        Utils.bitmapToMat(bitmapToScan, matToOCR);
//        matToOCR = AndroidDebug.readImage("tessdata/again.jpg");
        setOriginMat(matToOCR);
    }

    @Override
    public String getIDString() {
        if (idNumbers.length() > 19)
            return idNumbers.substring(0, 19);
        return idNumbers;
    }

    @Override
    public String getValidDateString() {
        return this.validDate;
    }

    public String getResultView() {
        return CommonUtils.APP_PATH + RESULT_VIEW;
    }

    public String getMainView() {
        return CommonUtils.APP_PATH + MAIN_VIEW;
    }

    public String getDebugRegionView() {
        return CommonUtils.APP_PATH + DEBUG_REGION_VIEW;
    }

    public String getDebugFontView() {
        return CommonUtils.APP_PATH + DEBUG_FONT_VIEW;
    }

    public boolean scan() {
        return doRecognize();
    }

    public int getProgress() {
        return this.progress;
    }
}
