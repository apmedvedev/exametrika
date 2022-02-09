/**
 * Copyright 2015 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.forecast;

import java.util.Arrays;
import java.util.Comparator;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.SimpleIntDeque;

class Dtw extends TempState {
    private final float INF = Float.MAX_VALUE;
    private final int maxElementCount;
    private final float[] query;
    private final float[] qo;
    private final float[] uo;
    private final float[] lo;
    private final int[] order;
    private final Index[] Q_tmp;
    private final float[] u;
    private final float[] l;
    private final float[] cb;
    private final float[] cb1;
    private final float[] cb2;
    private final float[] t;
    private final float[] tz;
    private final float[] data;
    private final float[] u_buff;
    private final float[] l_buff;
    private final Comparator<Index> comparator;
    private float[] cost;
    private float[] cost_prev;
    private final int m;
    private final int r;
    private int dataLength;
    private float bsf = INF;
    private int best;

    public Dtw(int windowSize, float warpingBand, int maxElementCount) {
        this.m = windowSize;
        if (warpingBand <= 1)
            r = (int) Math.floor(warpingBand * windowSize);
        else
            r = (int) Math.floor(warpingBand);

        query = new float[windowSize];
        qo = new float[windowSize];
        uo = new float[windowSize];
        lo = new float[windowSize];
        order = new int[windowSize];
        Q_tmp = new Index[windowSize];
        u = new float[windowSize];
        l = new float[windowSize];
        cb = new float[windowSize];
        cb1 = new float[windowSize];
        cb2 = new float[windowSize];
        t = new float[2 * windowSize];
        tz = new float[windowSize];
        for (int i = 0; i < windowSize; i++)
            Q_tmp[i] = new Index();

        cost = new float[2 * r + 1];
        cost_prev = new float[2 * r + 1];

        this.maxElementCount = maxElementCount;
        data = new float[maxElementCount];
        u_buff = new float[maxElementCount];
        l_buff = new float[maxElementCount];

        comparator = new Comparator<Index>() {
            @Override
            public int compare(Index o1, Index o2) {
                float v2 = Math.abs(o2.value);
                float v1 = Math.abs(o1.value);
                return Float.compare(v2, v1);
            }
        };
    }

    public float[] getQuery() {
        return query;
    }

    public float[] getData() {
        return data;
    }

    public int getDataLength() {
        return dataLength;
    }

    public void setDataLength(int length) {
        Assert.isTrue(length < maxElementCount);
        this.dataLength = length;
    }

    public float getDistance() {
        return bsf;
    }

    public int getIndex() {
        return best;
    }

    public static void reverse(float[] a, int length) {
        for (int i = 0; i < length / 2; i++) {
            float f = a[i];
            a[i] = a[length - 1 - i];
            a[length - 1 - i] = f;
        }
    }

    public void znorm(float[] values, int length, float mean, float stddev) {
        for (int i = 0; i < length; i++)
            values[i] = (values[i] - mean) / stddev;
    }

    public void derivative(float[] x, int length) {
        float[] y = l_buff;
        for (int i = 1; i < length - 1; i++)
            y[i] = (x[i] - x[i - 1] + (x[i + 1] - x[i - 1]) / 2) / 2;

        y[0] = y[1];
        y[length - 1] = y[length - 2];
        System.arraycopy(y, 0, x, 0, length);
    }

    public void compute(float distanceThreshold) {
        bsf = INF;
        best = -1;

        lower_upper_lemire(query, m, r, l, u);

        for (int i = 0; i < m; i++) {
            Q_tmp[i].value = query[i];
            Q_tmp[i].index = i;
        }

        Arrays.sort(Q_tmp, comparator);

        for (int i = 0; i < m; i++) {
            int o = Q_tmp[i].index;
            order[i] = o;
            qo[i] = query[o];
            uo[i] = u[o];
            lo[i] = l[o];
        }

        for (int i = 0; i < m; i++) {
            cb[i] = 0;
            cb1[i] = 0;
            cb2[i] = 0;
        }

        lower_upper_lemire(data, dataLength, r, l_buff, u_buff);

        for (int i = 0; i < dataLength; i++) {
            float d = data[i];

            t[i % m] = d;
            t[(i % m) + m] = d;

            if (i >= m - 1) {
                int j = (i + 1) % m;
                int offset = i - (m - 1);

                float lb_kim = lb_kim_hierarchy(t, query, j, m, bsf);

                if (lb_kim < bsf) {
                    float lb_k = lb_keogh_cumulative(order, t, uo, lo, cb1, j, m, bsf);
                    if (lb_k < bsf) {
                        for (int k = 0; k < m; k++)
                            tz[k] = t[(k + j)];

                        float lb_k2 = lb_keogh_data_cumulative(order, tz, qo, cb2, l_buff, u_buff, offset, m, bsf);
                        if (lb_k2 < bsf) {
                            if (lb_k > lb_k2) {
                                cb[m - 1] = cb1[m - 1];
                                for (int k = m - 2; k >= 0; k--)
                                    cb[k] = cb[k + 1] + cb1[k];
                            } else {
                                cb[m - 1] = cb2[m - 1];
                                for (int k = m - 2; k >= 0; k--)
                                    cb[k] = cb[k + 1] + cb2[k];
                            }

                            float dist = dtw(tz, query, cb, bsf, true);

                            if (dist < bsf) {
                                bsf = dist;
                                best = i;

                                if (bsf <= distanceThreshold)
                                    break;
                            }
                        }
                    }
                }
            }
        }

        bsf = (float) Math.sqrt(bsf);
    }

    public float dtw(float[] A, float[] B, float[] cb, float bsf, boolean checkBounds) {
        Arrays.fill(cost, INF);
        Arrays.fill(cost_prev, INF);

        int k = 0;
        for (int i = 0; i < m; i++) {
            k = Math.max(0, r - i);
            float min_cost = INF;

            for (int j = Math.max(0, i - r); j <= Math.min(m - 1, i + r); j++, k++) {
                if ((i == 0) && (j == 0)) {
                    cost[k] = dist(A[0], B[0]);
                    min_cost = cost[k];
                    continue;
                }

                float x, y, z;
                if ((j - 1 < 0) || (k - 1 < 0))
                    y = INF;
                else
                    y = cost[k - 1];
                if ((i - 1 < 0) || (k + 1 > 2 * r))
                    x = INF;
                else
                    x = cost_prev[k + 1];
                if ((i - 1 < 0) || (j - 1 < 0))
                    z = INF;
                else
                    z = cost_prev[k];

                cost[k] = Math.min(Math.min(x, y), z) + dist(A[i], B[j]);

                if (cost[k] < min_cost)
                    min_cost = cost[k];
            }

            if (checkBounds && (i + r < m - 1 && min_cost + cb[i + r + 1] >= bsf))
                return min_cost + cb[i + r + 1];

            float[] cost_tmp = cost;
            cost = cost_prev;
            cost_prev = cost_tmp;
        }

        return cost_prev[k - 1];
    }

    private void lower_upper_lemire(float[] t, int len, int r, float[] l, float[] u) {
        SimpleIntDeque du = new SimpleIntDeque(2 * r + 2);
        SimpleIntDeque dl = new SimpleIntDeque(2 * r + 2);

        du.addLast(0);
        dl.addLast(0);

        for (int i = 1; i < len; i++) {
            if (i > r) {
                u[i - r - 1] = t[du.getFirst()];
                l[i - r - 1] = t[dl.getFirst()];
            }
            if (t[i] > t[i - 1]) {
                du.removeLast();
                while (!du.isEmpty() && t[i] > t[du.getLast()])
                    du.removeLast();
            } else {
                dl.removeLast();
                while (!dl.isEmpty() && t[i] < t[dl.getLast()])
                    dl.removeLast();
            }
            du.addLast(i);
            dl.addLast(i);
            if (i == 2 * r + 1 + du.getFirst())
                du.removeFirst();
            else if (i == 2 * r + 1 + dl.getFirst())
                dl.removeFirst();
        }
        for (int i = len; i < len + r + 1; i++) {
            u[i - r - 1] = t[du.getFirst()];
            l[i - r - 1] = t[dl.getFirst()];
            if (i - du.getFirst() >= 2 * r + 1)
                du.removeFirst();
            if (i - dl.getFirst() >= 2 * r + 1)
                dl.removeFirst();
        }
    }

    private float lb_kim_hierarchy(float[] t, float[] q, int j, int len, float bsf) {
        float x0 = t[j];
        float y0 = t[(len - 1 + j)];
        float lb = dist(x0, q[0]) + dist(y0, q[len - 1]);
        if (lb >= bsf)
            return lb;

        float x1 = t[(j + 1)];
        float d = Math.min(dist(x1, q[0]), dist(x0, q[1]));
        d = Math.min(d, dist(x1, q[1]));
        lb += d;
        if (lb >= bsf)
            return lb;

        float y1 = t[(len - 2 + j)];
        d = Math.min(dist(y1, q[len - 1]), dist(y0, q[len - 2]));
        d = Math.min(d, dist(y1, q[len - 2]));
        lb += d;
        if (lb >= bsf)
            return lb;

        float x2 = t[(j + 2)];
        d = Math.min(dist(x0, q[2]), dist(x1, q[2]));
        d = Math.min(d, dist(x2, q[2]));
        d = Math.min(d, dist(x2, q[1]));
        d = Math.min(d, dist(x2, q[0]));
        lb += d;
        if (lb >= bsf)
            return lb;

        float y2 = t[(len - 3 + j)];
        d = Math.min(dist(y0, q[len - 3]), dist(y1, q[len - 3]));
        d = Math.min(d, dist(y2, q[len - 3]));
        d = Math.min(d, dist(y2, q[len - 2]));
        d = Math.min(d, dist(y2, q[len - 1]));
        lb += d;

        return lb;
    }

    private float lb_keogh_cumulative(int[] order, float[] t, float[] uo, float[] lo, float[] cb, int j, int len,
                                      float best_so_far) {
        float lb = 0;
        for (int i = 0; i < len && lb < best_so_far; i++) {
            float x = t[(order[i] + j)];
            float d = 0;
            if (x > uo[i])
                d = dist(x, uo[i]);
            else if (x < lo[i])
                d = dist(x, lo[i]);
            lb += d;
            cb[order[i]] = d;
        }
        return lb;
    }

    private float lb_keogh_data_cumulative(int[] order, float[] tz, float[] qo, float[] cb, float[] l, float[] u,
                                           int offset, int len, float best_so_far) {
        float lb = 0;
        for (int i = 0; i < len && lb < best_so_far; i++) {
            float uu = u[offset + order[i]];
            float ll = l[offset + order[i]];
            float d = 0;
            if (qo[i] > uu)
                d = dist(qo[i], uu);
            else {
                if (qo[i] < ll)
                    d = dist(qo[i], ll);
            }
            lb += d;
            cb[order[i]] = d;
        }
        return lb;
    }

    private float dist(float x, float y) {
        return ((x - y) * (x - y));
    }

    private static class Index {
        float value;
        int index;
    }

    ;
}
