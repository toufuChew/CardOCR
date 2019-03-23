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
        setOriginMat(matToOCR);
    }

    @Override
    public String getIDString() {
        return this.idNumbers;
    }

    @Override
    public String getValidDateString() {
        return this.validDate;
    }

    public boolean scan() {
        return doRecognize();
    }

    public int getProgress() {
        return this.progress;
    }
}
