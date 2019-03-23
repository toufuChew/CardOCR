package com.toufuchew.cardocr.idcard.cv.imgutils;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

/**
 * Created by chenqiu on 2/20/19.
 */
public interface RectFilter {
    int MIN_AREA = 10;
    float MIN_HEIGHT_RATE = 0.038f;
    float MAX_HEIGHT_RATE = 0.15f;
    float MIN_WIDTH_RATE = 0.12f;
    /**
     * filter out irrelevant areas of the credit card
     * @param rect
     * @return
     */
    boolean isDigitRegion(Rect rect, int srcWidth, int srcHeight);

    int HEIGHT_SCORE = 6;
    int WIDTH_SCORE = 3;
    int Y_POS_SCORE = 1;
    int IDRegionSimilarity(Rect roi, int rows, int cols);

    Rect findMaxRect(Mat m);

}
