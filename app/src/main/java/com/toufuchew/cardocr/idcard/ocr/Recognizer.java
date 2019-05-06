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
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Filter;

import static com.toufuchew.cardocr.tools.AndroidDebug.log;

abstract public class Recognizer implements CardOCR{
    protected volatile int progress;

    protected Mat originMat;

    private TessBaseApi instance;

    protected String idNumbers;

    protected String validDate;

    public static final float aspectRation = 1.579f;

    public static final int standardWidth = 28;

    public static final String RESULT_VIEW = "DigitsView.jpg";

    public static final String MAIN_VIEW = "ROI.jpg";

    public static final String DEBUG_REGION_VIEW = "Pre-process.jpg";

    public static final String DEBUG_FONT_VIEW = "binDigits.jpg";
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
        /**
         * clone from rgbMat
         */
        Mat roiMat;

        float resizeRatio;

        private Mat resultView;

        public Producer(Mat graySrc, Mat originMat) {
            super(graySrc);
            rgbMat = CVGrayTransfer.resizeNormalizedMat(originMat, false);
            roiMat = rgbMat.clone();
            resizeRatio = (float)originMat.width() / rgbMat.width();
            resultView = null;
        }

        @Override
        public void drawROI(Rect r, boolean isAims, String msg) {
            Scalar scalar = new Scalar(0, 0, 255);
            if (isAims) scalar = new Scalar(0, 255, 0);
            Imgproc.rectangle(roiMat, new Point(r.x, r.y),
                    new Point(r.x + r.width, r.y + r.height), scalar, 2);
            if (msg != null) {
                Imgproc.putText(roiMat, msg,
                        new Point(r.x, r.y + r.height), Core.FONT_HERSHEY_SIMPLEX, 1, scalar, 2);
            }
        }

        /**
         * for the light-font type
         * @param cuttingList
         */
        @Override
        protected void paintDigits(List<Integer> cuttingList, int standardWidth) {
            super.paintDigits(cuttingList, standardWidth);
            Mat digits = new Mat(rgbMat, rectOfDigitRow);
            Rect cutter = new Rect(0, 0, 0, rectOfDigitRow.height);
            int standardHeight = digits.rows();
            Filter filter = new Filter(null);
            List<Mat> digitList = new ArrayList<>();
            for (int i = 1; i < cuttingList.size(); i++) {
                if ((i & 0x1) == 0)
                    continue;
                int x1 = cuttingList.get(i - 1);
                int x2 = cuttingList.get(i);
                cutter.x = x1; cutter.width = x2 - x1;
                Rect ofY = cutEdgeOfY(new Mat(getBinDigitRegion(), cutter));
                if (filter.digitAssertFailed(ofY.width, ofY.height, standardWidth, standardHeight))
                    continue;
                digitList.add(new Mat(digits, new Rect(x1, ofY.y, cutter.width, ofY.height)));
            }
            setResultView(digitList);
        }

        /**
         * for the black-font type
         * @param m binary image of id region
         * @throws Exception
         */
        @Override
        public void setSingleDigits(Mat m) throws Exception {
            super.setSingleDigits(m);
            AndroidDebug.writeImage(DEBUG_FONT_VIEW, m);
/**
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
 **/
            matListOfDigit.add(new Mat(grayMat, getRectOfDigitRow()).clone());
            resultView = matListOfDigit.get(0);
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
        AndroidDebug.writeImage(MAIN_VIEW, producer.roiMat);

        producer.setRectOfDigitRow(mainRect);
        updateProgress(20);

        List<Mat> normalizedDigits = new ArrayList<>();
        try {
            producer.digitSeparate();
            updateProgress(35);
            normalizedDigits = producer.getMatListOfDigit();
            if (producer.getFontType() == CardFonts.FontType.LIGHT_FONT) {
                normalizedDigits = resizeDataSetImg(normalizedDigits);
            }
            updateProgress(10);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Mat concat = new Mat();
        Core.vconcat(normalizedDigits, concat);
        AndroidDebug.writeImage("concat." + CardFonts.fontTypeToString(producer.getFontType()) + ".jpg", concat);
        AndroidDebug.writeImage(RESULT_VIEW, producer.resultView);
        String digit = "";
        String test = "";
        for (Mat m : normalizedDigits) {
            digit = instance.doOCR(m);
            test += "("+digit+")";
            if (producer.getFontType() != CardFonts.FontType.BLACK_FONT)
                digit = fixIllegalChars(digit) + "";
            idNumbers += digit;
            updateProgress(2);
        }
        AndroidDebug.log("number", test);
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
        // draw the last result region
        producer.drawROI(bestRect, true, null);
        try {
            AndroidDebug.writeImage(DEBUG_REGION_VIEW, writeDebug);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bestRect;
    }

    /**
     * repair wrong text chars Tesseract recognized.
     * @param tessChars chars of tesseract-ocr result
     * @return
     */
    abstract protected char fixIllegalChars(String tessChars);

    private void updateProgress(int val0) {
        progress += val0;
        if (progress > 100)
            progress = 100;
    }

    @Override
    public boolean isIDCard() {
        if (idNumbers.length() < 18)
            return false;
        String seq = idNumbers.substring(idNumbers.length() - 18, idNumbers.length()).replaceAll(" ", "");
        int year = Integer.decode(seq.substring(6, 8));
        if (year < 19 || year > 20) {
            return false;
        }
        int checksum = idNumbers.charAt(idNumbers.length() - 1) - '0';
        byte[] mul = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
        final byte div = 11;
        int sum = 0;
        for (int i = 0; i < seq.length() - 1; i++) {
            sum += mul[i] * (seq.charAt(i) - '0');
        }
        byte[] mod = {1, 0, -1, 9, 8, 7, 6, 5, 4, 3, 2};
        if (sum % div != 2 && checksum != mod[sum % div]) {
            return false;
        }
        if (sum % div == 2) {
            idNumbers = idNumbers.substring(0, idNumbers.length() - 1) + "X";
        }
        return true;
    }
}
