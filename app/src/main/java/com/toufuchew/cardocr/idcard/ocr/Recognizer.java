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
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import static com.toufuchew.cardocr.tools.AndroidDebug.log;

abstract public class Recognizer implements CardOCR{
    protected volatile int progress;

    protected Mat originMat;

    private TessBaseApi instance;

    protected String idNumbers;

    protected String validDate;

    public static final float aspectRation = 1.579f;

    public static final int standardWidth = 280;

    public static final String RESULT_VIEW = "DigitsView.jpg";

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
        /**
         * for display result
         */
        Mat rgbMat;

        float resizeRatio;

        private Mat resultView;

        public Producer(Mat graySrc, Mat originMat) {
            super(graySrc);
            rgbMat = CVGrayTransfer.resizeNormalizedMat(originMat, false);
            resizeRatio = (float)originMat.width() / rgbMat.width();
            resultView = null;
        }

        @Override
        protected void paintDigits(List<Integer> cuttingList) {
            super.paintDigits(cuttingList);
            Mat digits = new Mat(rgbMat, rectOfDigitRow);
            Rect cutter = new Rect(0, 0, 0, rectOfDigitRow.height);
            List<Mat> digitList = new ArrayList<>();
            for (int i = 1; i < cuttingList.size(); i++) {
                if ((i & 0x1) == 0)
                    continue;
                int x1 = cuttingList.get(i - 1);
                int x2 = cuttingList.get(i);
                cutter.x = x1; cutter.width = x2 - x1;
                Rect ofY = cutEdgeOfY(new Mat(getBinDigitRegion(), cutter));
                digitList.add(new Mat(digits, new Rect(x1, ofY.y, cutter.width, ofY.height)));
            }
            setResultView(digitList);
        }

        @Override
        public void setSingleDigits(Mat m) throws Exception {
            super.setSingleDigits(m);

            Mat digits = new Mat(rgbMat, rectOfDigitRow);
            List<Mat> digitList = new ArrayList<>();
            List<Integer> axisX = new ArrayList<>();
            final float minHeight = 0.5f * m.rows();
            final float aspectRatio = 7;
            List<MatOfPoint> contours = new ArrayList<>();
            Imgproc.findContours(m, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
            for (MatOfPoint matOfPoint : contours) {
                Rect digitRect = Imgproc.boundingRect(matOfPoint);
                if (digitRect.height > minHeight) {
                    if (aspectRatio * digitRect.width > digitRect.height) {
                        // to sort the contours
                        int index;
                        for (index = 0; index < axisX.size(); index++) {
                            if (axisX.get(index) > digitRect.x)
                                break;
                        }
                        matListOfDigit.add(index, new Mat(m, digitRect));
                        axisX.add(index, digitRect.x);
                        // for display
                        digitList.add(index, new Mat(digits, digitRect));
                    }
                }
            }
            setResultView(digitList);
        }

        private void setResultView(List<Mat> digitList) {
            int viewHeight = 0;
            int viewWidth = 0;
            final int marginRight = 5;
            for (Mat m : digitList) {
                viewHeight = Math.max(viewHeight, m.rows());
                viewWidth += m.cols() + marginRight;
            }
            Mat view = new Mat(viewHeight, viewWidth, rgbMat.type(), Scalar.all(255));
            int start = 0;
            int channels = rgbMat.channels();
            byte[] dst = new byte[viewWidth * viewHeight * channels];
            view.get(0, 0, dst);
            for (int i = 0; i < digitList.size(); i++) {
                Mat m = digitList.get(i);
                int width = m.cols();
                int height = m.rows();
                byte[] buff = new byte[width * height * channels];
                m.get(0, 0, buff);
                for (int j = 0; j < height; j ++) {
                    System.arraycopy(buff, j * width * channels, dst, (start + j * viewWidth) * channels, width * channels);
                }
                start += marginRight + width;
            }
            view.put(0, 0, dst);
            resultView = CVGrayTransfer.resizeMat(view, (int)(view.width() * resizeRatio) << 1, false);
        }
    }

    public Recognizer(){
        this.instance = new TessBaseApi();
        this.originMat = null;
        resetVal();
    }

    public void setOriginMat(Mat originMat) {
        this.originMat = originMat;
    }

    private void resetVal() {
        this.idNumbers = "";
        this.validDate = "";
        this.progress = 0;
    }

    protected boolean doRecognize() {
        if (originMat == null) {
            Log.e(CommonUtils.TAG, "Recognizer error: originMat not set or is null.");
            return false;
        }
        resetVal();

        Mat gray = CVGrayTransfer.grayTransferBeforeScale(originMat, false);
        updateProgress(10);

        Producer producer = new Producer(gray, originMat);

        Rect mainRect = findMainRect(producer);
        producer.setRectOfDigitRow(mainRect);
        updateProgress(20);

        List<Mat> normalizedDigits = new ArrayList<>();
        try {
            producer.digitSeparate();
            updateProgress(35);

            normalizedDigits = resizeDataSetImg(producer.getMatListOfDigit());
            updateProgress(10);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Mat concat = new Mat();
        Core.vconcat(normalizedDigits, concat);
        AndroidDebug.writeImage(CardFonts.fontTypeToString(producer.getFontType()) + ".jpg", concat);
        AndroidDebug.writeImage(RESULT_VIEW, producer.resultView);

        String digit;
        for (Mat m : normalizedDigits) {
            digit = instance.doOCR(m);
            idNumbers += digit;
            updateProgress(2);
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
        Mat writeDebug = null;
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
                    writeDebug = dilate;
                }
            }
            if (findBright) break;
        }
        if (bestRect.width == 0) {
            System.err.println("OCR Failed.");
            return null;
        }
        try {
            AndroidDebug.writeImage("Preprocess.jpg", writeDebug);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bestRect;
    }

    private void updateProgress(int val0) {
        progress += val0;
        if (progress > 100)
            progress = 100;
    }

}
