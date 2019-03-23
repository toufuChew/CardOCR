package com.toufuchew.cardocr.idcard.ocr;

import android.util.Log;

import com.toufuchew.cardocr.idcard.cv.imgutils.CardFonts;
import com.toufuchew.cardocr.idcard.cv.override.CVDilate;
import com.toufuchew.cardocr.idcard.cv.override.CVGrayTransfer;
import com.toufuchew.cardocr.idcard.cv.override.CVRegion;
import com.toufuchew.cardocr.tools.AndroidDebug;
import com.toufuchew.cardocr.tools.CommonUtils;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.util.ArrayList;
import java.util.List;

import static com.toufuchew.cardocr.tools.AndroidDebug.log;

abstract public class Recognizer implements CardOCR{

    private Mat originMat;

    private TessBaseApi instance;

    protected String idNumbers;

    protected String validDate;

    public static final float aspectRation = 1.579f;

    public static final int standardWidth = 280;

    /**
     * load openCV lib only once
     */
    static {
        if (!OpenCVLoader.initDebug()) {
            log("Recognizer", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
        } else {
            log("Recognizer", "OpenCV library loaded!");
        }
    }

    static class Producer extends CVRegion {
        public Producer(Mat graySrc) {
            super(graySrc);
        }
    }

    public Recognizer(){
        this.instance = new TessBaseApi();
        this.idNumbers = "";
        this.validDate = "";
        this.originMat = null;
    }

    public void setOriginMat(Mat originMat) {
        this.originMat = originMat;
    }

    protected boolean doRecognize() {
        if (originMat == null) {
            Log.e(CommonUtils.TAG, "Recognizer error: originMat not set or is null.");
            return false;
        }
        Mat gray = CVGrayTransfer.grayTransferBeforeScale(originMat, false);

        Producer producer = new Producer(gray);

        Rect mainRect = findMainRect(producer);
        producer.setRectOfDigitRow(mainRect);

        List<Mat> normalizedDigits = new ArrayList<>();
        try {
            producer.digitSeparate();
            normalizedDigits = resizeDataSetImg(producer.getMatListOfDigit());
        } catch (Exception e) {
            e.printStackTrace();
        }

        Mat concat = new Mat();
        Core.vconcat(normalizedDigits, concat);
        AndroidDebug.writeImage(CardFonts.fontTypeToString(producer.getFontType()) + ".jpg", concat);

        idNumbers = "";
        String digit;
        for (Mat m : normalizedDigits) {
            digit = instance.doOCR(m);
            idNumbers += digit;
        }
        return true;
    }

    private List<Mat> resizeDataSetImg(List<Mat> set) {
        List<Mat> rstList = new ArrayList<>();
        for (Mat m : set) {
            rstList.add(CVGrayTransfer.resizeMat(m, standardWidth, (int)(standardWidth * aspectRation)));
        }
        return rstList;
    }

    private Rect findMainRect(Producer producer) {
        boolean findBright = false;
        Mat gray = producer.grayMat;
        Rect bestRect = new Rect();
        final float fullWidth = gray.cols() - Producer.border * 2;
        boolean chose;
        for ( ; ; findBright = true) {
            Mat dilate = CVDilate.fastDilate(gray, findBright);
            Rect idRect = null;
            chose = false;
            try {
                idRect = producer.digitRegion(dilate);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (idRect != null) {
                if (bestRect.width == 0)
                    chose = true;
                else if (idRect.width < fullWidth) {
                    if (bestRect.width == fullWidth || idRect.width > bestRect.width)
                        chose = true;
                }
                if (chose) {
                    bestRect = idRect;
                }
            }
            if (findBright) break;
        }
        if (bestRect.width == 0) {
            System.err.println("OCR Failed.");
            return null;
        }
        return bestRect;
    }

}
