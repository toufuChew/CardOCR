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

    @Override
    public boolean checkCardID() {
        /**
         * China UnionPay
         */
        if (idNumbers.charAt(1) == '2') {
            idNumbers = "6" + idNumbers.substring(1);
        }
        return true;
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
        boolean result = doRecognize();
        checkCardID();
        return result;
    }

    @Override
    protected char fixIllegalChars(String tessChars) {
        int len = tessChars.length();
        if (len == 1) return tessChars.charAt(0);
        if (len == 0) return '7';
        // digit '8' always would be recognized as '*3*'
        if (tessChars.contains("3")) {
            return '8';
        }
        if (tessChars.contains("11")) {
            return '4';
        }
        return tessChars.charAt(len - 1);
    }

    public int getProgress() {
        return this.progress;
    }
}
