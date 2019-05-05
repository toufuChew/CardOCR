package com.toufuchew.cardocr.idcard.cv.imgutils;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

/**
 * Created by chenqiu on 2/20/19.
 */
public interface RectFilter {
    /**
     * minimum height or size in roi
     */
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

    int IDRegionSimilarity(Mat m, Rect r, int rows, int cols);

    float FULL_AREA_RATIO = 0.8f;
    float FRAME_H_RATIO = 0.7f;
    void findMaxRect(Mat m, Rect r);

    boolean digitAssertFailed(int srcWidth, int srcHeight, int refWidth, int refHeight);
}
