package com.toufuchew.cardocr.idcard.cv.override;

import com.toufuchew.cardocr.idcard.cv.imgutils.AbstractSimpleMathUtils;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import static org.opencv.imgproc.Imgproc.INTER_AREA;
import static org.opencv.imgproc.Imgproc.resize;

/**
 * Created by chenqiu on 11/26/18.
 */
public class CVGrayTransfer {

    private static GrayUtils grayUtils;

    static {
        grayUtils = new GrayUtils();
    }

    static final class GrayUtils extends AbstractSimpleMathUtils {
        /**
         * The bilinear interpolation
         * @param src
         * @param size
         * @return
         */
        public Mat scale(Mat src, Size size) {
            Mat scaleMat = new Mat();
            resize(src, scaleMat, size, 0, 0, INTER_AREA);
            return scaleMat;
        }
    }

    /**
     * transfer to gray image
     * @param src origin image file
     * @return
     */
    public static Mat grayTransfer(Mat src){
        return new CVGrayTransfer().rgbToGray(src);
    }

    public Mat rgbToGray(Mat src) {
        Mat dst = new Mat();
        Imgproc.cvtColor(src, dst, Imgproc.COLOR_BGR2GRAY);
        return dst;
    }

    /**
     * {@link CVGrayTransfer#grayTransfer(Mat)} after scale
     * @param src
     * @return
     */
    public static Mat grayTransferBeforeScale(Mat src, boolean pyr) {
        final int mw = src.width() > 1024 ? 1024 : src.width();
        return grayTransferBeforeScale(src, mw, pyr);
    }

    public static Mat grayTransferBeforeScale(Mat m, int resizeWidth, boolean pyr) {
        Mat resize;
        resize = resizeMat(m, resizeWidth, pyr);
        return new CVGrayTransfer().rgbToGray(resize);
    }

    public static Mat resizeNormalizedMat(Mat m, boolean pyr) {
        final int mw = m.width() > 1024 ? 1024 : m.width();
        return resizeMat(m, mw, pyr);
    }

    public static Mat resizeMat(Mat m, int resizeWidth, boolean pyr) {
        Mat resize;
        resize = pyr ? grayUtils.normalizeSize(m) :
                grayUtils.scale(m, new Size(resizeWidth, (float)m.height() / m.width() * resizeWidth));
        return resize;
    }

    public static Mat resizeMat(Mat m, int resizeWidth, int resizeHeight) {
        return grayUtils.scale(m, new Size(resizeWidth, resizeHeight));
    }
}