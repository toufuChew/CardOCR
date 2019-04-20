package com.toufuchew.cardocr.idcard.cv.imgutils;

import com.toufuchew.cardocr.idcard.cv.override.CVGrayTransfer;
import com.toufuchew.cardocr.tools.AndroidDebug;
import com.toufuchew.cardocr.tools.CommonUtils;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by chenqiu on 2/21/19.
 */
public abstract class ImgSeparator implements RectSeparator, DigitSeparator{
    public Mat grayMat;

    protected List<Mat> matListOfDigit;

    protected Rect rectOfDigitRow;

    public ImgSeparator(Mat graySrc) {
        this.grayMat = graySrc;
        matListOfDigit = new ArrayList<>();
        rectOfDigitRow = null;
    }

    @Override
    public List<Rect> rectSeparate(Mat src, Rect region) throws Exception {
        if (src.channels() != 1) {
            throw new Exception("error: image.channels() != 1 in function 'rectSeparate(Mat m,Rect r)'");
        }
        // fist step, remove abnormal height area, fill with 0
        int cols = src.cols();
        int rows = src.rows();
        byte buff[] = new byte[cols * rows];
        src.get(0, 0, buff);
        List<Rect> stack = new LinkedList<>();
        List<Rect> separates = new ArrayList<>();
        stack.add(region);
        while (!stack.isEmpty()) {
            Rect ret = new Rect();
            Rect head;
            Rect scan = findEdge(buff, cols, rows, head = stack.remove(0), ret);
            if (ret.x > 0 && ret.y > 0) {
                separates.add(ret);
            }
            // separate region
            int upper = scan.y - head.y;
            int lower = head.y + head.height - scan.y - scan.height;
            if (upper > 0) {
                stack.add(new Rect(head.x, head.y, head.width, upper));
            }
            if (lower > 0) {
                stack.add(new Rect(head.x, scan.y + scan.height, head.width,  lower));
            }

        }
        return separates;
//        drawLine(buff, edge.y * cols, cols);
//        drawLine(buff, (edge.y + edge.height) * cols, cols);
//        byte out[] = new byte[src.cols() * src.rows()];
//        System.arraycopy(buff, edge.y * cols, out, edge.y * cols, edge.height * cols);
//        Mat m = Mat.zeros(src.size(), src.type());
//        m.put(0, 0, out);
//        return m;
    }

    /**
     * return rect scanned bounding, remove to avoid scanning overtimes
     * if find failed, out.x = out.y = -1
     * @param buff
     * @param cols
     * @param rows
     * @param region
     * @param out
     * @return
     */
    private Rect findEdge(byte buff[], int cols, int rows, Rect region, Rect out) {
        // thresh of `thin`
        final int thinH = (int)(RectFilter.MIN_HEIGHT_RATE * rows);
        out.x = out.y = -1;
        if (region.height < thinH) {
            return region.clone();
        }
        int w = region.x + region.width;
        int h = region.y + region.height;
        int pivot[] = new int[3]; // the longest continuous line
        int len = 0; // length of the line
        for (int i = region.y; i < h; i++) {
            int tLen = 0;
            int start = 0;
            int gap = 0;
            for (int j = 0; j < cols; j++) {
                int index = i * cols + j;
                if (buff[index] != 0) {
                    if (tLen++ == 0)
                        start = j;
                    if (tLen > len) {
                        len = tLen;
                        pivot[0] = start; // start x-pos
                        pivot[1] = i;
                        pivot[2] = j; // end x-pos
                    }
                    gap = 0;
                } else if (++gap > RectFilter.MIN_WIDTH_RATE * cols) {
                    tLen = 0;
                }
            }
        }
        int line = pivot[2] - pivot[0];
//        CommonUtils.info("line: " + pivot[2] + ", thresh: " + (cols * (RectFilter.MIN_WIDTH_RATE * 3)));
        if (len < cols * (RectFilter.MIN_WIDTH_RATE * 3)) { // too short
            return region.clone();
        }

        int upperY, lowerY, cnt;
        upperY = lowerY = cnt = 0;
        int []ha = new int[line];
        for (int i = 0; i < line; i++) {
            ha[i] = extendHeight(buff, cols,i + pivot[0], pivot[1]);
        }

        final int normalH = (int)(RectFilter.MAX_HEIGHT_RATE * rows);
        // when continuous thin area is too long, assert fail
        final int thinW = (int)(RectFilter.MIN_WIDTH_RATE * len);
        final int normalW = (int)(0.1 * len);
        int cw = 0; // continuous width that fitted normal height
        int ctl = 0; // continuous thin len
        int y2[][] = new int[2][line];
        byte next = -1;
        for (int c = 0; c < line; c++) {
            int []ey2 = extendY(buff, cols, c + pivot[0], pivot[1]);
            if (ha[c] < normalH) {
                if (ha[c] < thinH) {
                    ++ctl;
                    if (ctl > thinW) {
                        next = 0; // cannot be changed
                    }
                } else {
                    ctl = 0;

                    cw ++;
                    upperY += ey2[0];
                    lowerY += ey2[1];
                    cnt++;
                    if (cw > normalW && next != 0) {
                        next = 1;
                    }

                }
            } else {
                cw = 0;
            }
            y2[0][c] = ey2[0];
            y2[1][c] = ey2[1];
        }
        // find median
        Arrays.sort(y2[0]);
        Arrays.sort(y2[1]);
        int my1, my2, b = y2[0].length >> 1;
        my1 = y2[0][b];
        my2 = y2[1][b];
        if ((y2[0].length & 0x1) == 0) {
            my1 = (y2[0][b] + y2[0][b - 1]) >> 1;
            my2 = (y2[1][b] + y2[1][b - 1]) >> 1;
        }

        Rect scanRect = new Rect(region.x, my1, region.width, my2 - my1 + 1);
        if (next < 1) {
            return scanRect;
        }
        upperY /= cnt;
        lowerY /= cnt;
        out.x = pivot[0];
        out.y = upperY;
        out.width = line;
        out.height = lowerY - upperY + 1;
        return scanRect;
    }

    private int[] extendY(byte buff[], int cols, int x, int y) {
        int u, d;
        u = d = 0;
        while (y - u > 0 && buff[(y - u) * cols + x] != 0) {
            ++u;
        }
        int rows = buff.length / cols;
        while (y + d < rows && buff[(y + d) * cols + x] != 0) {
            ++d;
        }
        return new int[]{y - u, y + d};
    }

    protected int extendHeight(byte buff[], int cols, int x, int y) {
        int[] y2 = extendY(buff, cols, x, y);
        return y2[1] - y2[0];
    }

    /**
     * descending sort
     * @param a array with index and value
     */
    private void sortMap(int [][]a) {
        for (int i = 0; i < a[1].length - 1; i++) {
            int k = i;
            for (int j = i + 1; j < a[1].length; j++) {
                if (a[1][k] < a[1][j]) {
                    k = j;
                }
            }
            if (k != i) {
                swap(a[0], i, k);
                swap(a[1], i, k);
            }
        }
    }

    private void swap(int []a, int i, int j) {
        a[i] = a[i] + a[j];
        a[j] = a[i] - a[j];
        a[i] = a[i] - a[j];
    }

    @SuppressWarnings("unused")
    private void drawLine(byte buff[], int xy, int len) {
        int e = len + xy;
        for (int i = xy; i < e; i++) {
            buff[xy] = 0;
        }
    }

    /**
     * @deprecated as solution space too large
     * @param cutting
     */
    private void localCombine(List<Integer> cutting) {
        if ((cutting.size() & 0x1) == 1) {
            System.err.println("ImgSeparator error: cutting.size() cannot be odd number in function combine(List<Integer> c)");
            System.exit(1);
        }
        int cnt = cutting.size() >> 1;
        int []boxes = {16, 17, 18, 19};
        final int cap = 3;
        for (int box : boxes) {
            int []intervals = new int[box];
            // all are 0 except the first box
            if ((cnt - box) % cap != 0)
                intervals[(cnt - box) / cap] = (cnt - box) % cap;
            for (int i = (cnt - box) / cap - 1; i >= 0; i--)
                intervals[i] = cap;

            int saving = 0;
            while (intervals[0] > 0) {
                int mv;
                // scan from right to left
                for (mv = box - 1; mv > 0; mv--)
                    if (intervals[mv] != 0)
                        break;
                --intervals[mv];
                if (mv == box - 1) {
                    saving += intervals[mv];
                    intervals[mv] = 0;
                    for (mv = box - 2; mv > 0; mv--)
                        if (intervals[mv] != 0)
                            break;
                    --intervals[mv];
                    ++saving;
                }
                for (saving++, mv++; mv < box; mv++) {
                    if (intervals[mv] >= cap)
                        continue;
                    if (saving <= cap - intervals[mv]) {
                        intervals[mv] += saving;
                        saving = 0;
                        break;
                    }
                    saving -= (cap - intervals[mv]);
                    intervals[mv] = cap;
                }
                if (saving > 0) {
                    for (mv = box - 1; mv > 0 && intervals[mv] != 0; mv--) {
                        saving += intervals[mv];
                        intervals[mv] = 0;
                    }
                    mv = box;
                    while (mv > 0 && intervals[--mv] == 0);
                    intervals[mv] -= 1;
                    for (saving++, mv++; saving > 0; mv++) {
                        intervals[mv] = Math.min(saving, cap);
                        saving -= cap;
                    }
                }
                saving = 0;
            }
            return;
        }
    }

    private void simpleCombine(List<Integer> cutting, List<Integer> cntArea, int refWidth) {
        int wid[] = new int[cutting.size() >> 1];
        int []symbol = new int[wid.length];
        for (int i = 1; i < cutting.size(); i+=2) {
            wid[(i - 1) >> 1] = cutting.get(i) - cutting.get(i - 1);
        }
        for (int i = 0; i < symbol.length; i++)
            symbol[i] = i;

        final float exp = 1.2f * refWidth;

        boolean combineLeft = true;
        for (int c = 1; c < wid.length - 1; c++) {
            int lg = cutting.get(c << 1) - cutting.get((c << 1) - 1);
            int rg = -cutting.get(c * 2 + 1) + cutting.get((c + 1) << 1);
            int left = wid[c - 1] + wid[c] + lg;
            int right = wid[c] + wid[c + 1] + rg;
            if (left > exp && right > exp)
                continue;
            if (left < exp && right < exp) {
                if (lg > rg) {
                    // combine right
                    combineLeft = false;
                }
            }
            else if (right < exp) {
                combineLeft = false;
            }
            if (combineLeft) {
                // combine left
                symbol[c] = symbol[c - 1];
                wid[c] = left;
            } else {
                // combine right
                symbol[c + 1] = symbol[c];
                wid[c] = right;
                wid[c + 1] = right;
                ++c; // skip next
            }
            combineLeft = true;
        }
        // output combine
        for (int i = 1; i < symbol.length; i++) {
            if (symbol[i] == symbol[i - 1]) {
                cutting.set(2 * i - 1, -1); // for tag to remove
            }
        }
        for (int i = 0; i < cutting.size();) {
            if (cutting.get(i) == -1) {
                cutting.remove(i);
                cutting.remove(i);
                continue;
            }
            i++;
        }
        AndroidDebug.log("cntArea", cntArea);
        AndroidDebug.log("symbol", symbol);
    }

    /**
     * get the average width of id region digits
     * @param cutting
     * @return
     */
    protected int getDigitWidth(List<Integer> cutting) throws Exception {
        if ((cutting.size() & 0x1) == 1) {
            CommonUtils.info(true, "ImgSeparator error: cutting.size() cannot be odd number in function getDigitWidth(List<Integer> c).");
            cutting.remove(cutting.size() - 1);
        }
        final int window = 5;
        int [][]width = new int[2][cutting.size() >> 1];
        if (width[0].length <= window) {
            return -1;
        }
        for (int i = 1, j = 0; i < cutting.size(); i+= 2, j++) {
            width[1][j] = cutting.get(i) - cutting.get(i - 1);
            width[0][j] = j;
        }
        sortMap(width);
        int ms = -1;
        float m = Float.MAX_VALUE;
        int sum = 0;
        for (int i = 0; i < window; i++)
            sum += width[1][i];
        for (int i = window; i < width[0].length; i++) {
            float diff = 0;
            if (i > window)
                sum += (- width[1][i - window - 1] + width[1][i]);
            float avg = sum / window;
            for (int j = 0; j < window; j++) {
                diff += Math.pow(width[1][i - j] - avg, 2);
            }
            // get the min square difference
            if (diff < m) {
                ms = i - window;
                m = diff;
            }
        }
        int corrWidth = 0;
        for (int i = window; i > 0; i--)
            corrWidth += width[1][ms + i - 1];
        return corrWidth / window;
    }

    public int[] calcHistOfXY(Mat m, boolean axisX) {
        int []calc;
        int rows = m.rows();
        int cols = m.cols();
        byte buff[] = new byte[rows * cols];
        m.get(0, 0, buff);
        if (axisX) {
            calc = new int[cols];
            for (int i = 0; i < cols; i++) {
                for (int j = 0; j < rows; j++)
                    calc[i] += (buff[i + j * cols] & 0x1);
            }
        } else {
            calc = new int[rows];
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++)
                    calc[i] += (buff[i * cols + j] & 0x1);
            }
        }
        return calc;
    }

    abstract public void setSingleDigits() throws Exception;

    /**
     * spilt each digit of id region
     * @param m binary image of id region
     * @throws Exception
     */
    public void setSingleDigits(Mat m) throws Exception {
        if (m.type() != CvType.CV_8UC1) {
            throw new Exception("ImgSeparator error: Mat m.type is not CvType.CV_8UC1 which is " + CvType.typeToString(m.type()) + " in function setSingleDigits(Mat m).");
        }
    }

    abstract public void split(SplitList splitList);

    abstract public void merge(SplitList splitList) throws Exception;

    protected void paintDigits(List<Integer> cuttingList) {
        for (int i = 1; i < cuttingList.size(); i++) {
            if ((i & 0x1) == 0)
                continue;
            int x1 = cuttingList.get(i - 1);
            int x2 = cuttingList.get(i);
            Mat crop = new Mat(grayMat, new Rect(x1 + rectOfDigitRow.x, rectOfDigitRow.y, x2 - x1, rectOfDigitRow.height));
            byte buff[] = new byte[crop.rows() * crop.cols()];
            crop.get(0, 0, buff);
            Mat dst = Mat.zeros(new Size(rectOfDigitRow.width, rectOfDigitRow.height), grayMat.type());
            byte out[] = new byte[dst.cols() * dst.rows()];
            for (int j = 0; j < crop.rows(); j++)
                System.arraycopy(buff, j * crop.cols(), out, j * dst.cols(), crop.cols());
            dst.put(0, 0, out);
            dst = CVGrayTransfer.resizeMat(dst, 380, false);
            matListOfDigit.add(dst);
        }
        Mat dst = new Mat();
        Core.vconcat(matListOfDigit, dst);
    }

    abstract protected void cutEdgeOfX(Rect rect);

    protected int findNext(int v[], int index) {
        int i;
        boolean get = false;
        if (index < v.length && v[index] != 0)
            get = true;
        for (i = index; i < v.length; i++) {
            if (get && v[i] == 0)
                break;
            if (!get && v[i] != 0)
                break;
        }
        return i;
    }

    abstract protected Rect cutEdgeOfY(Mat binSingleDigit);

    abstract public void drawROI(Rect rect, boolean isAims, String msg);

    @Override
    public void digitSeparate() throws Exception {
        if (grayMat.type() != CvType.CV_8UC1) {
            throw new Exception("ImgSeparator error: digitSeparate supports only CV_8UC1 images when mode == " + CvType.typeToString(grayMat.type()));
        }
        if (this.rectOfDigitRow.width == 0 || this.rectOfDigitRow.height == 0) {
            throw new Exception("ImgSeparator error: digitSeparate need to set this.rectOfDigitRow whose width or height is 0.");
        }
//        this.matListOfDigit = Arrays.asList(bin);
    }

    public List<Mat> getMatListOfDigit() {
        return matListOfDigit;
    }

    public void setRectOfDigitRow(Rect rectOfDigitRow) {
        this.rectOfDigitRow = rectOfDigitRow;
    }

    public Rect getRectOfDigitRow() {
        return rectOfDigitRow;
    }}
