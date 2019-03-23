package com.toufuchew.cardocr.idcard.cv.imgutils;


import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by chenqiu on 2/20/19.
 */
public class ImgFilter implements RectFilter {
    @Override
    public boolean isDigitRegion(Rect rect, int srcWidth, int srcHeight) {
        if (rect.width * rect.height < this.MIN_AREA) {
            return false;
        }
        if (srcHeight * this.MIN_HEIGHT_RATE > rect.height ||
                srcHeight * this.MAX_HEIGHT_RATE < rect.height) {
            return false;
        }
        if (srcWidth * this.MIN_WIDTH_RATE > rect.width) {
            return false;
        }
        return true;
    }

    @Override
    public int IDRegionSimilarity(Rect roi, int rows, int cols) {
        int origin = 0;
        if (roi.y < this.MIN_HEIGHT_RATE * rows)
            return origin;
        if (roi.y > (1 - this.MIN_HEIGHT_RATE) * rows)
            return origin;
        origin += roi.width * this.WIDTH_SCORE + roi.height * this.HEIGHT_SCORE;
        origin += roi.y * this.Y_POS_SCORE;
        return origin;
    }

    @Override
    public Rect findMaxRect(Mat m) {
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(m, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        Rect out = new Rect();
        for (MatOfPoint contour : contours) {
            Rect rect = Imgproc.boundingRect(contour);
            if (rect.width * rect.height > out.width * out.height) {
                out = rect;
            }
        }
        return out;
    }

}
