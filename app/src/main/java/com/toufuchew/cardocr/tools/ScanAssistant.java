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
        char first = idNumbers.charAt(0);
        if (first < '3' || first > '6') {
            idNumbers = '6' + idNumbers.substring(1);
        }
        /**
         * China UnionPay
         */
        if (idNumbers.charAt(1) == '2') {
            idNumbers = "6" + idNumbers.substring(1);
        }

        /**
         * ID card
         */
        if (isIDCard()) {
            idNumbers = "身份证: " + idNumbers.substring(idNumbers.length() - 18, idNumbers.length());
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
        return tessChars.charAt(0);
    }

    public int getProgress() {
        return this.progress;
    }
}
