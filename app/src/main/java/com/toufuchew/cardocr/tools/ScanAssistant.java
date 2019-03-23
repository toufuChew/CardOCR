package com.toufuchew.cardocr.tools;

import com.toufuchew.cardocr.idcard.ocr.Recognizer;

import org.opencv.core.Mat;

public class ScanAssistant extends Recognizer {

    public ScanAssistant(Mat matToScan) {
        super();
        setOriginMat(matToScan);
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
}
