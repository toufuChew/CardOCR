package com.toufuchew.cardocr.idcard.cv.override;

import com.toufuchew.cardocr.idcard.cv.imgutils.CardFonts;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import static org.opencv.imgproc.Imgproc.THRESH_BINARY;

/**
 * Created by chenqiu on 3/6/19.
 */
public class CVDilate {

    /**
     * It needs cluster proc before, such as :
     *  <p>**CVCluster cvCluster = new CVCluster();</p>
     *  <p>**cvCluster.cluster();</p>
     * <p>Then cvCluster will generate the result as its parameter,</p>
     * if ClusterType == LIGHT_FONT, param `thresh` will be ignore
     * @param gray0
     * @param type
     * @param thresh
     * @return
     */
    public static Mat dilate(Mat gray0, CardFonts.FontType type, int thresh) {
        if (gray0.type() != CvType.CV_8U) {
            System.err.println("CVDilate error: image gray0 is not gray scale image in function dilate(Mat gray0, type)");
            System.exit(1);
        }
        Mat dst;
        if (type == CardFonts.FontType.LIGHT_FONT) {
            dst = dilateBrightRegion(gray0);
        }
        else {
            dst = new Mat();
            Imgproc.threshold(gray0, dst, thresh, 255, Imgproc.THRESH_BINARY_INV);
        }
        Size heavy = new Size(35, 5);
        // again
        Imgproc.dilate(dst, dst, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, heavy));
        return dst;
    }

    public static Mat fastDilate(Mat gray0, boolean findBright) {
        if (gray0.type() != CvType.CV_8U) {
            System.err.println("CVDilate error: image gray0 is not gray scale image in function fastDilate(Mat gray0, boolean fb)");
            System.exit(1);
        }
        Mat dst;
        if (findBright)
            dst = dilateBrightRegion(gray0);
        else dst = dilateDarkRegion(gray0);
        return dst;
    }

    public static Mat dilateBrightRegion(Mat gray0) {
        Mat dst = new Mat();
        // top-hat enhance contrast
        Imgproc.morphologyEx(gray0, dst, Imgproc.MORPH_TOPHAT, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(9, 3)));
        Imgproc.GaussianBlur(dst, dst, new Size(13, 13), 0);
        Imgproc.Canny(dst, dst, 300, 600, 5, true);
        Imgproc.dilate(dst, dst, new Mat(), new Point(-1, -1), 5);
        Size heavy = new Size(35, 5);
        // apply a second dilate operation to the binary image
        Imgproc.dilate(dst, dst, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, heavy));
        return dst;
    }

    public static Mat dilateDarkRegion(Mat gray0) {
        Mat dst = new Mat();
        // enhance black area by black-hat
        Imgproc.morphologyEx(gray0, dst, Imgproc.MORPH_BLACKHAT, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(18, 10)));
        Imgproc.GaussianBlur(dst, dst, new Size(13, 13), 0);
        Imgproc.Canny(dst, dst, 300, 600, 5, true);
        Size heavy = new Size(35, 3);
        Imgproc.dilate(dst, dst, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, heavy));
        return dst;
    }
}
