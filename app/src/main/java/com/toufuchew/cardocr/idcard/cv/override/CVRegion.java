package com.toufuchew.cardocr.idcard.cv.override;


import com.toufuchew.cardocr.idcard.cv.imgutils.CardFonts;
import com.toufuchew.cardocr.idcard.cv.imgutils.ImgFilter;
import com.toufuchew.cardocr.idcard.cv.imgutils.ImgSeparator;
import com.toufuchew.cardocr.idcard.cv.imgutils.RectFilter;
import com.toufuchew.cardocr.idcard.cv.imgutils.SplitList;
import com.toufuchew.cardocr.tools.AndroidDebug;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by chenqiu on 2/20/19.
 */
public class CVRegion extends ImgSeparator {

    public static final int border = 10;

    public Mat getBinDigitRegion() {
        return binDigitRegion;
    }

    private Mat binDigitRegion;

    private CardFonts.FontType fontType;

    public CVRegion(Mat graySrc) {
        super(graySrc);
        binDigitRegion = null;
        fontType = CardFonts.FontType.UNKNOWN;
    }

    @Override
    public void split(SplitList splitList) {
        int rows = binDigitRegion.rows();
        int cols = binDigitRegion.cols();
        byte buff[] = new byte[cols * rows];
        binDigitRegion.get(0, 0, buff);
        int upperWidth = (int)(1.38f * splitList.getStandardWidth());
        int lowerWidth = (int)(0.8f * splitList.getStandardWidth());
        int window = upperWidth - lowerWidth;
        for (int i = 0; i < splitList.size(); i++) {
            SplitList.Node node = splitList.get(i);
            if (node.width() > upperWidth) {
                int x = node.getStartPointX() + lowerWidth;
                int spx = splitX(buff, x, x + window);
                if (spx > 0) {
                    splitList.split(i, spx);
                }
            }
        }
    }

    @Override
    public void merge(SplitList splitList) throws Exception {
        int min = Integer.MAX_VALUE;
        String solution = "";
        System.err.println("merge size: " + splitList.size());
        if (splitList.size() > 10) {
            throw new Exception("CVRegion error: splitList.size() is too large and over time limit to merge in function merge(SplitList spl).");
        }
        List<String> box = new ArrayList<>();
        permutations(splitList.size(), 0, "", box);
        for (int i = 0; i < box.size(); i++) {
            String s = box.get(i);
            int splIndex = 0;
            int score = 0;
            for (int j = 0; j < s.length(); j++) {
                int val = s.charAt(j) - '0';
                int distance = splitList.dist(splIndex, splIndex + val - 1);
                splIndex += val;
                score += Math.abs(distance - splitList.getStandardWidth());
            }
            if (score < min) {
                min = score;
                solution = s;
            }
        }
        for (int c = 0, spl = 0; c < solution.length(); c++) {
            int val = solution.charAt(c) - '0';
            splitList.join(spl, spl + val - 1);
            spl++;
        }
    }

    private void permutations(int total, int n, String solution, List<String> box) {
        if (total < n)
            return;
        if (total == n) {
            box.add(solution.substring(1) + n);
            return;
        }
        solution += n;
        permutations(total - n, 3, solution, box);
        permutations(total - n, 2, solution, box);
        permutations(total - n, 1, solution, box);
    }

    private int splitX(byte []buff, int si, int ei) {
        int max = 0;
        int index = 0;
        int rows = binDigitRegion.rows();
        int cols = binDigitRegion.cols();
        for (int x = si; x <= ei; x++) {
            int len = 0;
            for (int y = 0; y < rows; y++)
                if (buff[y * cols + x] == 0)
                    len++;
            if (max < len) {
                max = len;
                index = x;
            }
        }
        return index;
    }

    @Override
    public void setSingleDigits() throws Exception {
        if (fontType == CardFonts.FontType.BLACK_FONT) {
            setSingleDigits(binDigitRegion);
            return;
        }
        int []x = calcHistOfXY(binDigitRegion, true);
        int cur = 0;
        List<Integer> cutting = new LinkedList<>();
        if (x[cur] > 0)
            cutting.add(cur);
        while (true) {
            int next = findNext(x, cur);
            if (next >= x.length)
                break;
            cutting.add(next);
            cur = next;
        }

        int ref = getDigitWidth(cutting);
        if (ref < 0)
            return;

        SplitList splitList = new SplitList(cutting, ref);
        split(splitList);
        final int upperWidth = (int)(1.2f * ref);
        final int lowerWidth = (int)(0.6f * ref);
        SplitList output = splitList.out(upperWidth, lowerWidth);
        List<SplitList> buckets = splitList.crack(upperWidth);
        for (SplitList elem : buckets) {
            merge(elem);
            output.addAll(elem.toNodeList());
        }
        output.sort();
        paintDigits(output.toSimpleList());
    }

    public CardFonts.FontType getFontType() {
        return fontType;
    }

    final static class Filter extends ImgFilter {
        private Mat src;
        public Filter(Mat src) {
            this.src = src;
        }

        private void sortMap(int [][]a) {
            for (int i = 0; i < a[1].length - 1; i++) {
                int k = i;
                for (int j = i + 1; j < a[1].length; j++) {
                    if (a[1][k] > a[1][j]) {
                        k = j;
                    }
                }
                if (k != i) {
                    a[1][k] = a[1][k] + a[1][i];
                    a[1][i] = a[1][k] - a[1][i];
                    a[1][k] = a[1][k] - a[1][i];
                    a[0][i] = a[0][k] + a[0][i];
                    a[0][k] = a[0][i] - a[0][k];
                    a[0][i] = a[0][i] - a[0][k];
                }
            }
        }
        /**
         * get rect area of id numbers
         * @param contours
         * @return null if rect of id area not found
         */
        public Rect boundingIdRect(List<MatOfPoint> contours) {
            Rect rect;
            List<Rect> rectSet = new ArrayList<>();
            for (int i = 0; i < contours.size(); i++) {
                rect = Imgproc.boundingRect(contours.get(i));
                rectSet.add(rect);
            }
            rect = rectSet.get(0);
            int dist[][] = new int[2][rectSet.size()];
            for (int i = 0; i < rectSet.size(); i++) {
                dist[0][i] = i;
                dist[1][i] = rectSet.get(i).y - rect.y;
            }
            sortMap(dist);
            final int verBias = 15;
            for (int i = 0; i < dist[1].length - 2; i++) {
                if (dist[1][i + 2] - dist[1][i] < verBias) {
                    int k;
                    /**
                     * Upper left and lower right corners
                     */
                    int sx = src.width();
                    int sy = src.height();
                    int mx = -1;
                    int my = -1;
                    // max width between these id-digit area
                    int mw = 0;
                    int sw = src.width();
                    for (k = 0; k < 3; k ++) {
                        rect = rectSet.get(dist[0][k + i]);
                        if (!isDigitRegion(rect, src.width(), src.height())) {
                            break;
                        }
                        sx = Math.min(rect.x, sx);
                        sy = Math.min(rect.y, sy);
                        mx = Math.max(rect.x + rect.width, mx);
                        my = Math.max(rect.y + rect.height, my);
                        mw = Math.max(rect.width, mw);
                        sw = Math.min(rect.width, sw);
                    }
                    // less than 3 area, find next
                    if (k < 3) {
                        continue;
                    }

                    if (i < dist[1].length - 3) {
                        if (dist[1][i + 3] - dist[1][i] < verBias &&
                                isDigitRegion(rect = rectSet.get(dist[0][i + 3]), src.width(), src.height())) {
                            sx = Math.min(sx, rect.x);
                            sy = Math.min(sy, rect.y);
                            mx = Math.max(rect.x + rect.width, mx);
                            my = Math.max(rect.y + rect.height, my);
                            // finding out all 4 digit area
                            return new Rect(sx, sy, mx - sx, my - sy);
                        }
                    }
                    // completing 4th digit area
                    int mg;
                    // in order to make the gap largest, avoiding losing digit message
                    int gap = (mx - sx - sw * 3) >> 1;
                    Rect rt;
                    if (sx < (mg = src.width() - mx - gap)) {
                        rt = mg > mw ? new Rect(sx, sy, mx + mw + gap - sx , my - sy) :
                                new Rect(10, sy, src.width() - 20, my - sy);
                    }
                    else {
                        mg = sx - gap;
                        rt = mg > mw ? new Rect(sx - mw - gap, sy, mx - sx + mw, my - sy) :
                                new Rect(10, sy, src.width() - 20, my - sy);
                    }
                    return rt;
                }
            }
            return null;
        }
    }

    /**
     * for debug
     * getting mat with contours
     * @param src
     * @return
     */
    public static Mat getContours(Mat src) {
        List<MatOfPoint> contours = new ArrayList<>();
        /**
         * TODO:
         * We do findContours for white objects on black background.
         * While your binary image is black chars on white background, you should threshold it with flag THRESH_BINARY_INV to get white on black. Then do findContours.
         */
        Imgproc.findContours(src, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        Mat out = Mat.zeros(src.size(), src.type());
        Imgproc.drawContours(out, contours, -1, new Scalar(255, 255, 255), 1);
        return out;
    }

    /**
     * for debug
     * return mat with rectangle drawn
     * @param src
     * @param filter if is null, using CVRegion.Filter
     * @return
     */
    public static Mat drawRectangle(Mat src, RectFilter filter) {
        Mat out = Mat.zeros(src.size(), src.type());
        List<MatOfPoint> contours = new ArrayList<>();
        /**
         * TODO:
         * We do findContours for white objects on black background.
         * While your binary image is black chars on white background, you should threshold it with flag THRESH_BINARY_INV to get white on black. Then do findContours.
         */
        Imgproc.findContours(src, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        MatOfPoint contour = null;
        Rect rect = null;
        if (filter == null) {
            filter = new Filter(src);
        }
        for (int i = 0; i < contours.size(); i++) {
            contour = contours.get(i);
            if (filter.isDigitRegion(rect = Imgproc.boundingRect(contour), src.width(), src.height())) {

                Imgproc.rectangle(out, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(255, 255, 255), 1);
            }
        }
        return out;
    }

    /**
     * loc the digit area
     * @param src mat proc by binary, top-hat, dilate and closed opr
     * @return
     */
    public Rect digitRegion(Mat src) throws Exception {
        if (src.cols() < 20 || src.rows() < 20) {
            throw new Exception("error: image.cols() < 20 || image.rows() < 20 in function 'digitRegion(Mat m)'");
        }
        fillBorder(src);
        Filter filter = new Filter(src);
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(src, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        Rect rect;
        rect = filter.boundingIdRect(contours);
        if (rect == null) {
            // if cannot bounding digit area, start separating rect areas which are large
            Collections.sort(contours, new Comparator<MatOfPoint>() {
                @Override
                public int compare(MatOfPoint o1, MatOfPoint o2) {
                    return -((int) (Imgproc.contourArea(o1) - Imgproc.contourArea(o2))); // decrease
                }
            });
            final int detectDepth = Math.min(5, contours.size());
            int maxScore = 0;
            for (int t = 0; t < detectDepth; t++) {
                Rect br = Imgproc.boundingRect(contours.get(t));
                AndroidDebug.writeImage("br" + t + ".jpg", new Mat(src, br));
                List<Rect> separates = this.rectSeparate(src, br);
                for (Rect r : separates) {
                    Mat roi = drawRectRegion(src, r);
                    filter.findMaxRect(roi, r);
                    int score = filter.IDRegionSimilarity(roi, r, src.rows(), src.cols());
                    if (score > maxScore) {
                        maxScore = score;
                        rect = r;
                    }
                }
            }
        }
        if (rect == null)
            return null;
//        Mat m =drawRectRegion(src, rect); //debug
        cutEdgeOfX(rect);

        return rect;
    }

    public static Mat drawRectRegion(Mat src, Rect roi) {
        byte buff[] = new byte[src.rows() * src.cols()];
        src.get(0, 0, buff);

        Mat m = Mat.zeros(src.size(), src.type());
        byte out[] = new byte[buff.length];
        int row = roi.y + roi.height;
        for (int i = roi.y; i < row; i++) {
            System.arraycopy(buff, i * src.cols() + roi.x, out, i * src.cols() + roi.x, roi.width);
        }
        m.put(0, 0, out);

        return m;
    }

    /**
     * fill image border with black pix
     * @param m
     */
    public static void fillBorder(Mat m) {
        int cols = m.cols();
        int rows = m.rows();
        byte buff[] = new byte[cols * rows];
        m.get(0, 0, buff);
        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < rows; j++) {
                if ((i > border && j > border) && (i < cols - border && j < rows - border))
                    continue;
                buff[j * cols + i] = 0;
            }
        }
        m.put(0, 0, buff);
    }

    @Override
    public void digitSeparate() throws Exception {
        super.digitSeparate();
        Mat binDigits = new Mat(grayMat, getRectOfDigitRow()).clone();
        CardFonts fonts = CVFontType.getFontType(binDigits);
        CardFonts.FontType type = fonts.getType();
        if (type == CardFonts.FontType.LIGHT_FONT) {
            Mat sqKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));

            Mat dst0 = new Mat();
            Imgproc.morphologyEx(binDigits, dst0, Imgproc.MORPH_TOPHAT, sqKernel);
            Imgproc.morphologyEx(dst0, dst0, Imgproc.MORPH_GRADIENT, sqKernel);
            Imgproc.threshold(dst0, dst0, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
            Imgproc.medianBlur(dst0, dst0, 3);

            Mat dst1 = new Mat();
            Imgproc.morphologyEx(binDigits, dst1, Imgproc.MORPH_BLACKHAT, sqKernel);
            Imgproc.morphologyEx(dst1, dst1, Imgproc.MORPH_GRADIENT, sqKernel);
            Imgproc.medianBlur(dst1, dst1, 3);
            Imgproc.threshold(dst1, dst1, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
            Core.bitwise_or(dst0, dst1, dst1);
            Imgproc.morphologyEx(dst1, dst1, Imgproc.MORPH_CLOSE, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3)));
            Imgproc.dilate(dst1, binDigits, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(1, 5)));
        }
        if (type == CardFonts.FontType.BLACK_FONT || type == CardFonts.FontType.UNKNOWN) {
            binDigits = fonts.getFonts();
        }
        AndroidDebug.writeImage("binDigits.jpg", binDigits);
        this.binDigitRegion = binDigits;
        this.fontType = type;
        setSingleDigits();
    }

    /**
     * remove left and right edge of id region
     * @param rect
     */
    protected void cutEdgeOfX(Rect rect) {
        Mat dst = new Mat();
        Imgproc.GaussianBlur(grayMat, dst, new Size(13, 13), 0);
        Imgproc.Canny(dst, dst, 300, 600, 5, true);
        Imgproc.dilate(dst, dst, new Mat(), new Point(-1, -1), 1);
        Mat m = new Mat(dst, rect);
        byte buff[] = new byte[m.rows() * m.cols()];
        m.get(0, 0, buff);
        int rows = rect.height;
        int cols = rect.width;
        int left = rect.x;
        int right = rect.x + rect.width;
        int w = 0;
        for (int i = 0; i < (cols >> 1); i++) {
            int h = 0;
            for (int j = 0; j < rows; j++) {
                int at = j * cols + i;
                if (buff[at] == 0 && w == 0) {
                    break;
                }
                if (buff[at] != 0) ++h;
            }
            if (w > 0 && h == 0) break;
            if (h == rows) ++w;
            if (w > 0)
                left = rect.x + i;
        }

        byte b[] = new byte[dst.cols() * dst.rows()];
        dst.get(0, 0 ,b);
        if (w > 0) {
            int max = 0;
            for (int i = 0; i < w; i++) {
                int h = extendHeight(b, dst.cols(), left - i, rect.y);
                max = Math.max(max, h);
            }
            // reset
            if (max < rect.height * 1.5)
                left = rect.x;
        }
        // right edge
        w = 0;
        for (int i = cols - 1; i > cols - (cols >> 3); i--) {
            int h = 0;
            for (int j = 0; j < rows; j++) {
                int at = j * cols + i;
                if (buff[at] == 0)
                    break;
                ++h;
            }
            if (h < rows && w > 0)
                break;
            if (h == rows) {
                w++;
                right = rect.x + i;
            }
        }
        if (w > 0) {
            int max = 0;
            for (int i = 0; i < w; i++) {
                int h = extendHeight(b, dst.cols(), right + i, rect.y);
                max = Math.max(max, h);
            }
            if (max < rect.height * 1.5)
                right = rect.x + rect.width;
        }
        rect.x = left;
        rect.width = right - left;
    }


    @Override
    protected Rect cutEdgeOfY(Mat binDigitRegion) {
        int cols = binDigitRegion.cols();
        int rows = binDigitRegion.rows();
        byte origin[] = new byte[rows * cols];
        binDigitRegion.get(0, 0, origin);
        int upperY = 0, lowerY = 0;
        boolean white = false;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (origin[i * cols + j] != 0) {
                    white = true;
                    break;
                }
            }
            if (white) {
                upperY = i;
                break;
            }
        }
        white = false;
        for (int i = rows - 1; i >= 0; i--) {
            for (int j = 0; j < cols; j++) {
                if (origin[i * cols + j] != 0) {
                    white = true;
                    break;
                }
            }
            if (white) {
                lowerY = i;
                break;
            }
        }
        return new Rect(0, upperY, cols, lowerY - upperY);
    }

    @Override
    protected void paintDigits(List<Integer> cuttingList) {
        Mat digits;
        if (getFontType() == CardFonts.FontType.BLACK_FONT)
            digits = getBinDigitRegion();
        else digits = new Mat(grayMat, rectOfDigitRow);
        Rect cutter = new Rect(0, 0, 0, rectOfDigitRow.height);
        for (int i = 1; i < cuttingList.size(); i++) {
            if ((i & 0x1) == 0)
                continue;
            int x1 = cuttingList.get(i - 1);
            int x2 = cuttingList.get(i);
            cutter.x = x1; cutter.width = x2 - x1;
            Rect ofY = cutEdgeOfY(new Mat(getBinDigitRegion(), cutter));
            matListOfDigit.add(new Mat(digits, new Rect(x1, ofY.y, cutter.width, ofY.height)));
        }
    }
}
