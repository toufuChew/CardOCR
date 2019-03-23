package com.toufuchew.cardocr.idcard.cv.imgutils;


import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.util.List;

/**
 * Created by chenqiu on 2/21/19.
 */
public interface RectSeparator {

    List<Rect> rectSeparate(Mat src, Rect region) throws Exception;

}
