package com.toufuchew.cardocr.idcard.cv.imgutils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by chenqiu on 3/9/19.
 */
public class SplitList {

    private List<Node> sList;

    private int standardWidth;

    public SplitList(){
        this.sList = new LinkedList<>();
        standardWidth = 0;
    }

    public SplitList(List<Integer> cutting, int standardWidth) {
        this.sList = new LinkedList<>();
        int id = 0;
        for (int i = 0; i < cutting.size(); i += 2) {
            Node n = new Node(id, cutting.get(i), cutting.get(i + 1));
            id += 20;
            sList.add(n);
        }
        this.standardWidth = standardWidth;
    }

    public SplitList(SplitList copy, int start, int end) {
        this.sList = new LinkedList<>();
        for (; start <= end; start++) {
            Node n = copy.get(start).clone();
            this.sList.add(n);
        }
        this.standardWidth = copy.standardWidth;
    }

    public int getStandardWidth() {
        return standardWidth;
    }

    public class Node {
        private int id;
        private int width;
        private int x1;
        private int x2;
        public Node(int id, int x1, int x2) {
            this.id = id;
            this.x1 = x1;
            this.x2 = x2;
            this.width = x2 - x1;
        }

        public int width() {
            return width;
        }

        public int getStartPointX() {
            return x1;
        }

        public int getEndPointX() {
            return x2;
        }

        public int id() {
            return id;
        }

        public Node clone() {
            return new Node(this.id, this.x1, this.x2);
        }
    }

    /**
     * division of itself
     * @param gap
     * @return
     */
    public List<SplitList> crack(int gap) {
        List<SplitList> rstSet = new ArrayList<>(30);
        Node cur, next = sList.get(0);
        int start = 0;
        for (int i = 0; i < sList.size() - 1; i++) {
            cur = next;
            next = sList.get(i + 1);
            if (next.x2 - cur.x1 > gap) {
                rstSet.add(rstSet.size(), new SplitList(this, start, i));
                start = i + 1;
            }
        }
        rstSet.add(rstSet.size(), new SplitList(this, start, sList.size() - 1));
        this.sList = null; // release this
        return rstSet;
    }

    /**
     * split the sticky digits character in one Node
     * @throws Exception
     */
    public void split(int index, int x){
        Node loc = sList.get(index);
        sList.add(index + 1, new Node(loc.id + 1, x + 1, loc.x2));
        loc.x2 = x;
        // update width
        loc.width = loc.x2 - loc.x1;
    }

    public void join(int si, int ei) {
        if (si >= ei)
            return;
        Node prev = sList.get(si);
        Node last = null;
        int count = ei - si;
        for (int i = 0; i < count; i++) {
            last = sList.remove(si + 1);
        }
        prev.x2 = last.x2;
        prev.width = prev.x2 - prev.x1;
    }

    public int size() {
        return sList.size();
    }

    public void sort() {
        Collections.sort(this.sList, new Comparator<Node>() {
            @Override
            public int compare(Node o1, Node o2) {
                return o1.id - o2.id;
            }
        });
    }

    public Node get(int index) {
        return sList.get(index);
    }

    public void addAll(List<Node> nodeList) {
        for (Node node : nodeList) {
            sList.add(node);
        }
    }

    public int dist(int si, int ei) {
        if (ei <= si)
            return 0;
        return sList.get(ei).x2 - sList.get(si).x1;
    }

    /**
     * remove Nodes have been merged, and clear fragments at the same time
     * @param window
     * @param lowerWidth the thresh whether it should be regarded as fragments
     * @return
     */
    public SplitList out(int window, int lowerWidth) {
        SplitList rstList = new SplitList();
        Node cur, next, prev = null;
        cur = sList.get(0);
        next = sList.get(1);
        int i;
        for (i = 1; i < sList.size() - 1; ) {
            prev = cur;
            cur = next;
            next = sList.get(i + 1);
            if (cur.x2 - prev.x1 > window && next.x2 - cur.x1 > window) {
                sList.remove(i);
                if (lowerWidth < cur.width())
                    rstList.sList.add(cur);
                continue;
            }
            i++;
        }
        if (cur.x2 - prev.x1 > window && next.x2 - cur.x1 > window) {
            // second Node to last
            sList.remove(i);
            if (lowerWidth < next.width())
                rstList.sList.add(next);
        }
        rstList.standardWidth = this.standardWidth;
        return rstList;
    }

    public List<Node> toNodeList() {
        return this.sList;
    }

    public List<Integer> toSimpleList() {
        List<Integer> rstList = new ArrayList<>();
        for (int i = 0; i < sList.size(); i++) {
            Node node = sList.get(i);
            rstList.add(node.x1);
            rstList.add(node.x2);
        }
        return rstList;
    }
}