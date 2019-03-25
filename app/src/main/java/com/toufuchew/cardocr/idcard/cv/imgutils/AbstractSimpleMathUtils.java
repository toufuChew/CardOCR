package com.toufuchew.cardocr.idcard.cv.imgutils;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.Arrays;

/**
 * Created by chenqiu on 11/26/18.
 */
public abstract class AbstractSimpleMathUtils implements SimpleMath {
    
    public final int GRAYSCALE = 256;

    /**
     * find extremely value of 2D-curve
     * @param points
     * @return
     */
    public Object[] pointsExt(double[] points){
        // true 表示递增
        boolean state = true;
        int[] maxPoints = new int[GRAYSCALE];
        int[] minPoints = new int[GRAYSCALE];
        double[] maxVal = new double[GRAYSCALE];
        double[] minVal = new double[GRAYSCALE];

        int maxCur = -1;
        int minCur = -1;
        double pre = points[0];

        for (int i = 1; i < points.length; pre = points[i++]){
            double cp = points[i];
            // 极大值点
            if (pre > cp && state){
                state = false;
                maxPoints[++maxCur] = i - 1;
                maxVal[maxCur] = pre;
            }
            // 极小值点
            if (!state && pre < cp) {
                state = true;
                minPoints[++minCur] = i - 1;
                minVal[minCur] = pre;
            }
        }

        return new Object[]{
                Arrays.copyOf(maxPoints, maxCur + 1),
                Arrays.copyOf(maxVal, maxCur + 1),
                Arrays.copyOf(minPoints, minCur + 1),
                Arrays.copyOf(minVal, minCur + 1)
        };
    }

    /**
     * pyrDown method
     * @param src
     * @return
     */
    public Mat normalizeSize(Mat src) {
        final int s = 1200;
        int w = src.width();
        int h = src.height();
        Mat dst = src;
        while (w > s && h > s) {
            System.out.println(src.width() + ", " + src.height());
            Imgproc.pyrDown(src, dst, new Size(w >>= 1, h >>= 1));
            src = dst;
        }
        return dst;
    }

    @Override
    public double[] kernelDensity(double[] flow) {
        return new double[0];
    }

    public int safeLongToInt(long l) {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new IllegalArgumentException
                    (l + " cannot be cast to int without changing its value.");
        }
        return (int) l;
    }

}
