package com.toufuchew.cardocr.idcard.cv.override;

import com.toufuchew.cardocr.idcard.cv.imgutils.AbstractSimpleMathUtils;
import com.toufuchew.cardocr.idcard.cv.imgutils.CardFonts;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import smile.stat.distribution.KernelDensity;

public class CVCluster {

    public static final int GRAYSCALE = 256;
    protected static SimpleMathUtil helper = new SimpleMathUtil();
    /**
     * detect result of card id-font type
     * the default is black
     */
    public CardFonts.FontType type = CardFonts.FontType.UNKNOWN;

    private int binaryThreshold;

    static final class SimpleMathUtil extends AbstractSimpleMathUtils {

        public static int indexOfMaxValue(double[] array){
            double max = Double.MIN_VALUE;
            int index = -1;
            for (int i = 0; i < array.length; i++){
                if (max < array[i]){
                    index = i;
                    max = array[i];
                }
            }
            return index;
        }

        public double[] pixelsScale(Mat mat){
            int row = mat.rows();
            int col = mat.cols();
            double[] pixes = new double[row * col];
            int cur = 0;
            for (int i = 0; i < row; i++){
                for (int j = 0; j < col; j++){
                    pixes[cur++] = mat.get(i, j)[0];
                }
            }
            return pixes;
        }

        public double[] kernelDensity(double[] flow){
            double[] curve = new double[GRAYSCALE];
            KernelDensity kernelDensity = new KernelDensity(flow);
            for (int x = 0; x < curve.length; x++){
                curve[x] = kernelDensity.p(x);
            }
            return curve;
        }

        final int TOPGRAYS = 0;
        final int LOWGRAYS = 2;
        final int TOPVALUES = 1;
        final int LOWVALUES = 3;
    }


    /**
     * 灰度聚类
     * <p>note that: 如果图片过暗可能导致失败</p>
     * @param src 灰度图
     */
    public void cluster(Mat src){

        double []pixels = helper.pixelsScale(src);

        double []curve = helper.kernelDensity(pixels);

        this.type = CardFonts.FontType.BLACK_FONT;
        int reverse = Imgproc.THRESH_BINARY_INV;
        int threshold = properThreshold(curve);
        if (threshold > BACKGROUND){
            reverse = Imgproc.THRESH_BINARY;
            this.type = CardFonts.FontType.LIGHT_FONT;
        }
        this.binaryThreshold = threshold;
    }

    protected static final int BACKGROUND = 110;
    protected static final int FOREGROUND = 33;
    protected static final int OVERBRIGHT = 210;

    private static int properThreshold(double[] kdeCurve){
        Object[] plist = helper.pointsExt(kdeCurve);
        int threshold = -1;
        int maxIndex = helper.indexOfMaxValue((double[]) plist[helper.TOPVALUES]);
        maxIndex = ((int[]) plist[helper.TOPGRAYS])[maxIndex];
        int mask;

        if (maxIndex < BACKGROUND){
            mask = 125;
        }
        else {
            mask = maxIndex > OVERBRIGHT ? 75 : 50;
        }
        double[] minValues = (double[]) plist[helper.LOWVALUES];
        int[] minGrays = (int[]) plist[helper.LOWGRAYS];
        double[] scores = new double[minValues.length];
        final double v1 = 1;

        for (int i = 0; i < scores.length; i++){
            int symbol = -1;
            if (minGrays[i] - FOREGROUND < 0)
                symbol <<= ((FOREGROUND - minGrays[i]) >> 2);
            if (minGrays[i] - BACKGROUND > 0)
                symbol = 1;
            scores[i] = symbol * Math.abs(minGrays[i] - mask) / (kdeCurve[maxIndex] - v1 * minValues[i]);
        }

        int positiveIndex = -1, negativeIndex = -1;
        for (int i = 0; i < scores.length; i++){
            if ((positiveIndex == - 1 || scores[positiveIndex] > scores[i]) && scores[i] > 0)
                positiveIndex = i;
            if ((negativeIndex == - 1 || scores[negativeIndex] < scores[i]) && scores[i] < 0)
                negativeIndex = i;
        }
        threshold = maxIndex < BACKGROUND ? minGrays[positiveIndex] : minGrays[negativeIndex];

        return threshold;
    }

    public int getBinaryThreshold() {
        return binaryThreshold;
    }

}
