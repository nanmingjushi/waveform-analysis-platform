package com.nan.waveform.vision.engine;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.*;

/**
 * @author nan chao
 * @since 2026/6/15 14:16
 *
 * 视觉算法引擎
 */

public class OpenCvVisionEngine {

    // ===================================================================
    // 子功能一：波形图像暂态最大值算法块
    // ===================================================================
    public static Map<String, Object> computeTransientMaxValue(Mat part, double perSegmentValue) {
        List<Integer> lineY = zantaiDetectHorizontalBlackLines(part, 0.6);
        if (lineY.size() < 3) {
            return Map.of("error", "检测到的黑实线不足3条", "lines", lineY);
        }
        Collections.sort(lineY);
        int y1 = lineY.get(0);
        int y2 = lineY.get(lineY.size() / 2);
        int y3 = lineY.get(lineY.size() - 1);

        List<Integer> dashLines = zantaiDetectHorizontalDashLines(part, 400, 800);
        Collections.sort(dashLines);

        int waveTopY = zantaiFindWaveformTopY(part, y1, y3);
        boolean isUp = waveTopY < y2;

        double value = zantaiCalcMaxValueByDashes(y2, dashLines, waveTopY, isUp, perSegmentValue);

        return Map.of("waveTopY", waveTopY, "value", value);
    }

    private static List<Integer> zantaiDetectHorizontalBlackLines(Mat img, double totalRunRatio) {
        List<Integer> lines = new ArrayList<>();
        int minTotalRun = (int) (img.cols() * totalRunRatio);
        for (int y = 0; y < img.rows(); y++) {
            int totalRun = 0, currRun = 0;
            for (int x = img.cols() - 1; x >= 0; x--) {
                double[] color = img.get(y, x);
                if (isBlackColor(color)) currRun++;
                else {
                    totalRun += currRun;
                    currRun = 0;
                }
            }
            totalRun += currRun;
            if (totalRun > minTotalRun) lines.add(y);
        }
        List<Integer> uniq = new ArrayList<>();
        int last = -1000;
        for (int y : lines) {
            if (y - last > 10) uniq.add(y);
            last = y;
        }
        return uniq;
    }

    private static boolean isBlackColor(double[] color) {
        return color[0] < 150 && color[1] < 150 && color[2] < 150;
    }

    private static int zantaiFindWaveformTopY(Mat img, int y1, int y3) {
        for (int y = y1 + 1; y < y3; y++) {
            for (int x = 0; x < img.cols(); x++) {
                double[] color = img.get(y, x);
                if (isColorful(color) && !isBlackColor(color)) return y;
            }
        }
        return y3;
    }

    private static boolean isColorful(double[] color) {
        double max = Math.max(color[0], Math.max(color[1], color[2]));
        double min = Math.min(color[0], Math.min(color[1], color[2]));
        return (max - min) > 40 && max > 80;
    }

    private static List<Integer> zantaiDetectHorizontalDashLines(Mat img, int minTotal, int maxTotal) {
        List<Integer> lines = new ArrayList<>();
        for (int y = 0; y < img.rows(); y++) {
            int total = 0, curr = 0;
            for (int x = 0; x < img.cols(); x++) {
                double[] color = img.get(y, x);
                if (isBlackColor(color)) curr++;
                else {
                    total += curr;
                    curr = 0;
                }
            }
            total += curr;
            if (total >= minTotal && total < maxTotal) lines.add(y);
        }
        List<Integer> uniq = new ArrayList<>();
        int last = -100;
        for (int y : lines) {
            if (y - last > 8) uniq.add(y);
            last = y;
        }
        return uniq;
    }

    private static double zantaiCalcMaxValueByDashes(int y2, List<Integer> dashLines, int waveTopY, boolean isUp, double perSegmentValue) {
        List<Integer> allY = new ArrayList<>(dashLines);
        allY.add(y2);
        Collections.sort(allY);

        if (isUp) {
            List<Integer> up = new ArrayList<>();
            for (int y : allY) if (y < y2) up.add(y);
            up.sort(Collections.reverseOrder());

            int prev = y2, section = 0;
            for (int y : up) {
                if (waveTopY <= y) {
                    double ratio = (prev - waveTopY) * 1.0 / (prev - y);
                    return section * perSegmentValue + ratio * perSegmentValue;
                }
                prev = y;
                section++;
            }
            if (!up.isEmpty()) {
                double ratio = (prev - waveTopY) * 1.0 / (prev - up.get(up.size() - 1));
                return section * perSegmentValue + ratio * perSegmentValue;
            } else {
                return (y2 - waveTopY) * perSegmentValue / (y2 - 0 + 1e-9);
            }
        } else {
            List<Integer> down = new ArrayList<>();
            for (int y : allY) if (y > y2) down.add(y);
            Collections.sort(down);

            int prev = y2, section = 0;
            for (int y : down) {
                if (waveTopY >= y) {
                    double ratio = (waveTopY - prev) * 1.0 / (y - prev);
                    return -(section * perSegmentValue + ratio * perSegmentValue);
                }
                prev = y;
                section++;
            }
            if (!down.isEmpty()) {
                double ratio = (waveTopY - prev) * 1.0 / (down.get(down.size() - 1) - prev);
                return -(section * perSegmentValue + ratio * perSegmentValue);
            } else {
                return -(waveTopY - y2) * perSegmentValue / (300.0);
            }
        }
    }

    // ===================================================================
    // 子功能二：波形图像稳态值识别算法块
    // ===================================================================

    public static Map<String, Object> computeSteadyStateValue(Mat roi, double perSegmentValue, double displayScale) {
        Map<String, Object> resMap = new LinkedHashMap<>();
        List<Integer> blackLines = wentaiDetectHorizontalBlackLines(roi, 0.6);
        if (blackLines.size() < 3) {
            resMap.put("error", "检测到的黑实线不足3条");
            return resMap;
        }
        Collections.sort(blackLines);
        int y1 = blackLines.get(0);
        int y2 = blackLines.get(blackLines.size() / 2);
        int y3 = blackLines.get(blackLines.size() - 1);

        List<Integer> dashYs = wentaiDetectHorizontalDashLines(roi, y1, y3);
        int[] yTrace = wentaiTraceWaveYPerColumn(roi, y1, y3);

        int w = roi.width();
        int xStart = (int) Math.round(w * (1.0 - 0.40));
        xStart = Math.max(0, Math.min(w - 1, xStart));
        List<Integer> ys = new ArrayList<>();
        for (int x = xStart; x < w; x++) {
            if (yTrace[x] >= 0) ys.add(yTrace[x]);
        }
        if (ys.size() < 10) {
            resMap.put("error", "稳态窗口有效样本不足");
            return resMap;
        }

        int ymax = ys.stream().max(Integer::compareTo).orElse(y2);
        int ymin = ys.stream().min(Integer::compareTo).orElse(y2);
        int devTop = Math.abs(ymin - y2);
        int devBottom = Math.abs(ymax - y2);
        int peakY = (devTop >= devBottom) ? ymin : ymax;
        boolean isUp = (devTop >= devBottom);

        double peakVal = wentaiPixelToValueByDashes(peakY, y2, dashYs, perSegmentValue, 300, isUp);
        double steadyRms = Math.abs(peakVal) / Math.sqrt(2.0);

        double[] samples = new double[w - xStart];
        int idx = 0;
        for (int x = xStart; x < w; x++) {
            int yy = yTrace[x];
            if (yy >= 0) {
                boolean upHere = yy < y2;
                double val = wentaiPixelToValueByDashes(yy, y2, dashYs, perSegmentValue, 300, upHere);
                samples[idx++] = Math.abs(val);
            }
        }
        double sampleRms = wentaiCalcRms(samples, idx);

        resMap.put("steadyPeakV", peakVal * displayScale);
        resMap.put("steadyRmsV", steadyRms * displayScale);
        resMap.put("sampleRmsV", sampleRms * displayScale);
        return resMap;
    }

    private static List<Integer> wentaiDetectHorizontalBlackLines(Mat roi, double runRatioThresh) {
        Mat gray = new Mat();
        Imgproc.cvtColor(roi, gray, Imgproc.COLOR_BGR2GRAY);
        Mat bin = new Mat();
        Imgproc.threshold(gray, bin, 0, 255, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU);
        int h = bin.rows(), w = bin.cols();
        byte[] data = new byte[h * w];
        bin.get(0, 0, data);

        List<Integer> lines = new ArrayList<>();
        for (int y = 0; y < h; y++) {
            int rowOff = y * w;
            int blackRun = 0, maxRun = 0;
            for (int x = 0; x < w; x++) {
                if ((data[rowOff + x] & 0xFF) > 0) {
                    blackRun++;
                    if (blackRun > maxRun) maxRun = blackRun;
                } else blackRun = 0;
            }
            if (maxRun >= w * runRatioThresh) lines.add(y);
        }
        gray.release();
        bin.release();
        return lines;
    }

    private static List<Integer> wentaiDetectHorizontalDashLines(Mat roi, int y1, int y3) {
        Mat gray = new Mat();
        Imgproc.cvtColor(roi, gray, Imgproc.COLOR_BGR2GRAY);
        Mat bin = new Mat();
        Imgproc.adaptiveThreshold(gray, bin, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY_INV, 15, 10);
        int h = bin.rows(), w = bin.cols();
        y1 = Math.max(0, y1);
        y3 = Math.min(h - 1, y3);
        byte[] data = new byte[h * w];
        bin.get(0, 0, data);

        double[] rowSum = new double[h];
        for (int y = y1; y <= y3; y++) {
            int off = y * w, cnt = 0;
            for (int x = 0; x < w; x++) if ((data[off + x] & 0xFF) > 0) cnt++;
            rowSum[y] = cnt;
        }
        double[] sm = new double[h];
        int win = 5;
        for (int y = y1; y <= y3; y++) {
            int L = Math.max(y1, y - win), R = Math.min(y3, y + win);
            double s = 0;
            int c = 0;
            for (int k = L; k <= R; k++) {
                s += rowSum[k];
                c++;
            }
            sm[y] = s / c;
        }
        double mean = 0;
        int c = 0;
        for (int y = y1; y <= y3; y++) {
            mean += sm[y];
            c++;
        }
        mean /= Math.max(1, c);

        List<Integer> peaks = new ArrayList<>();
        for (int y = y1 + 1; y < y3; y++) {
            if (sm[y] > sm[y - 1] && sm[y] > sm[y + 1] && sm[y] > mean * 1.2) peaks.add(y);
        }
        List<Integer> merged = new ArrayList<>();
        int tol = 3;
        for (int y : peaks) {
            if (merged.isEmpty() || y - merged.get(merged.size() - 1) > tol) merged.add(y);
            else {
                int prev = merged.get(merged.size() - 1);
                merged.set(merged.size() - 1, (prev + y) / 2);
            }
        }
        gray.release();
        bin.release();
        return merged;
    }

    private static int[] wentaiTraceWaveYPerColumn(Mat roi, int y1, int y3) {
        Mat hsv = new Mat();
        Imgproc.cvtColor(roi, hsv, Imgproc.COLOR_BGR2HSV);
        int h = roi.rows(), w = roi.cols();
        y1 = Math.max(0, y1);
        y3 = Math.min(h - 1, y3);
        int[] ys = new int[w];
        Arrays.fill(ys, -1);

        for (int x = 0; x < w; x++) {
            int found = -1;
            for (int y = y1; y <= y3; y++) {
                double[] p = hsv.get(y, x);
                if (p == null) continue;
                if (p[1] > 40 && p[2] > 40) {
                    found = y;
                    break;
                }
            }
            ys[x] = found;
        }
        int k = 3;
        int[] sm = new int[w];
        for (int x = 0; x < w; x++) {
            int L = Math.max(0, x - k), R = Math.min(w - 1, x + k);
            int cnt = 0, sum = 0;
            for (int i = L; i <= R; i++) {
                if (ys[i] >= 0) {
                    cnt++;
                    sum += ys[i];
                }
            }
            sm[x] = cnt == 0 ? -1 : (sum / cnt);
        }
        hsv.release();
        return sm;
    }

    private static double wentaiPixelToValueByDashes(int y, int y2, List<Integer> dashYs, double perSeg, int fallbackPixels, boolean isUp) {
        if (y < 0) return Double.NaN;
        List<Integer> all = new ArrayList<>(dashYs);
        all.add(y2);
        Collections.sort(all);
        if (isUp) {
            List<Integer> up = new ArrayList<>();
            for (int d : all) if (d < y2) up.add(d);
            up.sort(Collections.reverseOrder());
            int prev = y2, section = 0;
            for (int d : up) {
                if (y <= d) {
                    return section * perSeg + (prev - y) * 1.0 / (prev - d) * perSeg;
                }
                prev = d;
                section++;
            }
            if (!up.isEmpty()) return section * perSeg + (prev - y) * 1.0 / (prev - up.get(up.size() - 1)) * perSeg;
            else return (y2 - y) * perSeg / fallbackPixels;
        } else {
            List<Integer> down = new ArrayList<>();
            for (int d : all) if (d > y2) down.add(d);
            Collections.sort(down);
            int prev = y2, section = 0;
            for (int d : down) {
                if (y >= d) {
                    return -(section * perSeg + (y - prev) * 1.0 / (d - prev) * perSeg);
                }
                prev = d;
                section++;
            }
            if (!down.isEmpty())
                return -(section * perSeg + (y - prev) * 1.0 / (down.get(down.size() - 1) - prev) * perSeg);
            else return -(y - y2) * perSeg / fallbackPixels;
        }
    }

    private static double wentaiCalcRms(double[] arr, int validCount) {
        if (validCount <= 0) return Double.NaN;
        double s = 0;
        int c = 0;
        for (int i = 0; i < validCount; i++) {
            double v = arr[i];
            if (!Double.isNaN(v)) {
                s += v * v;
                c++;
            }
        }
        return c == 0 ? Double.NaN : Math.sqrt(s / c);
    }

    // ===================================================================
    // 子功能三：频率计算算法块
    // ===================================================================

    public static Map<String, Object> computeFrequency(Mat roi) {
        Map<String, Object> resMap = new LinkedHashMap<>();
        int h = roi.rows(), w = roi.cols();

        List<Integer> vlines = pinlvDetectVerticalBlackLines(roi, 0.55);
        if (vlines.size() < 2) {
            resMap.put("error", "竖实线检测不足，无法标定时间刻度");
            return resMap;
        }
        Collections.sort(vlines);
        if (!vlines.isEmpty() && vlines.get(0) < 5) vlines.remove(0);
        if (!vlines.isEmpty() && vlines.get(vlines.size() - 1) > w - 6) vlines.remove(vlines.size() - 1);
        if (vlines.size() < 2) {
            resMap.put("error", "有效竖实线不足");
            return resMap;
        }

        List<Integer> diffs = new ArrayList<>();
        for (int i = 1; i < vlines.size(); i++) {
            int d = vlines.get(i) - vlines.get(i - 1);
            if (d > 1) diffs.add(d);
        }
        if (diffs.isEmpty()) {
            resMap.put("error", "竖线间距异常");
            return resMap;
        }
        Collections.sort(diffs);
        double pixelsPerGrid = diffs.get(diffs.size() / 2);
        double secondsPerPixel = 0.025 / Math.max(1.0, pixelsPerGrid);

        int xStart = (int) Math.round(w * (1.0 - 0.60));
        xStart = Math.max(0, Math.min(w - 2, xStart));

        int[] yTrace = pinlvTraceWaveYCenterPerColumn(roi, 0, h - 1);
        int[] yWin = Arrays.copyOfRange(yTrace, xStart, w);

        double[] sig = pinlvCompactValid(yWin);
        if (sig.length < 30) {
            resMap.put("error", "稳态窗口有效样本不足");
            return resMap;
        }
        sig = pinlvMovingAverage(sig, 3);
        double mean = Arrays.stream(sig).average().orElse(0);
        for (int i = 0; i < sig.length; i++) sig[i] -= mean;

        int minLagPx = Math.max(3, (int) Math.round(0.012 / secondsPerPixel));
        int maxLagPx = Math.min(sig.length / 2, (int) Math.round(0.030 / secondsPerPixel));
        if (minLagPx >= maxLagPx) {
            resMap.put("error", "可搜索的周期像素范围无效");
            return resMap;
        }

        double periodPx;
        double score = pinlvBestAutocorrLagScore(sig, minLagPx, maxLagPx);
        int lag = pinlvBestAutocorrLag(sig, minLagPx, maxLagPx);
        if (score >= 0.15) {
            periodPx = lag;
        } else {
            int[] peaks = pinlvFindExtremaIndices(sig);
            List<Integer> deltas = new ArrayList<>();
            for (int i = 1; i < peaks.length; i++) {
                int d = peaks[i] - peaks[i - 1];
                if (d >= minLagPx && d <= maxLagPx) deltas.add(d);
            }
            if (deltas.isEmpty()) {
                resMap.put("error", "极值不足或间距异常");
                return resMap;
            }
            Collections.sort(deltas);
            periodPx = deltas.get(deltas.size() / 2);
        }

        double Tsec = periodPx * secondsPerPixel;
        double freq = (Tsec > 0) ? (1.0 / Tsec) : Double.NaN;

        resMap.put("periodMs", pinlvSanitizeNumber(Tsec * 1000.0));
        resMap.put("freqHz", pinlvSanitizeNumber(freq));
        return resMap;
    }

    private static List<Integer> pinlvDetectVerticalBlackLines(Mat roi, double runRatio) {
        Mat gray = new Mat();
        Imgproc.cvtColor(roi, gray, Imgproc.COLOR_BGR2GRAY);
        Mat bin = new Mat();
        Imgproc.threshold(gray, bin, 0, 255, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU);
        int h = bin.rows(), w = bin.cols();
        byte[] data = new byte[h * w];
        bin.get(0, 0, data);

        List<Integer> xs = new ArrayList<>();
        int minRun = (int) (h * runRatio);
        for (int x = 0; x < w; x++) {
            int maxRun = 0, run = 0;
            for (int y = 0; y < h; y++) {
                if ((data[y * w + x] & 0xFF) > 0) {
                    run++;
                    if (run > maxRun) maxRun = run;
                } else run = 0;
            }
            if (maxRun >= minRun) xs.add(x);
        }
        List<Integer> merged = new ArrayList<>();
        Integer curStart = null, curEnd = null;
        for (int x : xs) {
            if (curEnd == null || x - curEnd <= 4) {
                if (curStart == null) curStart = x;
                curEnd = x;
            } else {
                merged.add((curStart + curEnd) / 2);
                curStart = x;
                curEnd = x;
            }
        }
        if (curStart != null) merged.add((curStart + curEnd) / 2);
        gray.release();
        bin.release();
        return merged;
    }

    private static int[] pinlvTraceWaveYCenterPerColumn(Mat roi, int y1, int y3) {
        Mat hsv = new Mat();
        Imgproc.cvtColor(roi, hsv, Imgproc.COLOR_BGR2HSV);
        int h = roi.rows(), w = roi.cols();
        y1 = Math.max(0, y1);
        y3 = Math.min(h - 1, y3);
        int[] ys = new int[w];
        Arrays.fill(ys, -1);

        for (int x = 0; x < w; x++) {
            int top = -1, bottom = -1;
            for (int y = y1; y <= y3; y++) {
                double[] p = hsv.get(y, x);
                if (p == null) continue;
                if (p[1] > 40 && p[2] > 40) {
                    if (top == -1) top = y;
                    bottom = y;
                }
            }
            if (top != -1) ys[x] = (top + bottom) / 2;
        }
        int k = 3;
        int[] sm = new int[w];
        for (int x = 0; x < w; x++) {
            int L = Math.max(0, x - k), R = Math.min(w - 1, x + k);
            int cnt = 0, sum = 0;
            for (int i = L; i <= R; i++) {
                if (ys[i] >= 0) {
                    cnt++;
                    sum += ys[i];
                }
            }
            sm[x] = cnt == 0 ? -1 : (sum / cnt);
        }
        hsv.release();
        return sm;
    }

    private static double pinlvBestAutocorrLagScore(double[] s, int minLag, int maxLag) {
        double energy = 0;
        for (double v : s) energy += v * v;
        if (energy <= 1e-12) return 0;
        double maxScore = -1e9;
        for (int k = minLag; k <= maxLag; k++) {
            double acc = 0;
            int n = s.length - k;
            for (int i = 0; i < n; i++) acc += s[i] * s[i + k];
            double score = acc / Math.sqrt(energy * energy);
            if (score > maxScore) maxScore = score;
        }
        return maxScore;
    }

    private static int pinlvBestAutocorrLag(double[] s, int minLag, int maxLag) {
        double energy = 0;
        for (double v : s) energy += v * v;
        if (energy <= 1e-12) return minLag;
        double maxScore = -1e9;
        int bestLag = minLag;
        for (int k = minLag; k <= maxLag; k++) {
            double acc = 0;
            int n = s.length - k;
            for (int i = 0; i < n; i++) acc += s[i] * s[i + k];
            double score = acc / Math.sqrt(energy * energy);
            if (score > maxScore) {
                maxScore = score;
                bestLag = k;
            }
        }
        return bestLag;
    }

    private static int[] pinlvFindExtremaIndices(double[] s) {
        int n = s.length;
        int[] sgn = new int[n];
        for (int i = 1; i < n; i++) {
            double d = s[i] - s[i - 1];
            sgn[i] = (d > 0) ? 1 : ((d < 0) ? -1 : 0);
        }
        List<Integer> maxima = new ArrayList<>();
        List<Integer> minima = new ArrayList<>();
        for (int i = 1; i < n - 1; i++) {
            if (sgn[i - 1] > 0 && sgn[i] < 0) maxima.add(i);
            if (sgn[i - 1] < 0 && sgn[i] > 0) minima.add(i);
        }
        List<Integer> chosen = (maxima.size() >= minima.size()) ? maxima : minima;
        int minGap = Math.max(3, (int) (n * 0.03));
        List<Integer> filtered = new ArrayList<>();
        int last = -10000;
        for (int p : chosen) {
            if (p - last >= minGap) {
                filtered.add(p);
                last = p;
            }
        }
        return filtered.stream().mapToInt(i -> i).toArray();
    }

    private static double[] pinlvCompactValid(int[] y) {
        List<Double> v = new ArrayList<>();
        for (int value : y) if (value >= 0) v.add((double) value);
        double[] out = new double[v.size()];
        for (int i = 0; i < v.size(); i++) out[i] = v.get(i);
        return out;
    }

    private static double[] pinlvMovingAverage(double[] s, int win) {
        if (win <= 1) return s;
        int n = s.length;
        double[] out = new double[n];
        int half = win / 2;
        for (int i = 0; i < n; i++) {
            int L = Math.max(0, i - half);
            int R = Math.min(n - 1, i + half);
            double sum = 0;
            for (int k = L; k <= R; k++) sum += s[k];
            out[i] = sum / (R - L + 1);
        }
        return out;
    }

    private static Double pinlvSanitizeNumber(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return null;
        return v;
    }

    // ===================================================================
    // 子功能四：阶跃响应时间测算算法块
    // ===================================================================

    public static Map<String, Object> computeStepResponse(Mat cropped, double tLeft, double tRight) {
        Map<String, Object> resp = new LinkedHashMap<>();
        if (tRight <= tLeft) throw new IllegalArgumentException("tRight 必须大于 tLeft");

        int W = cropped.cols(), H = cropped.rows();
        List<Integer> yCurve = jieyueExtractRedCurve(cropped, W, H);
        yCurve = jieyueMovingAvg(yCurve, 7);

        int band = Math.max(3, (int) (0.10 * W));
        double yLow = jieyueMedian(yCurve.subList(0, band));
        double yHigh = jieyueMedian(yCurve.subList(W - band, W));

        if (Math.abs(yHigh - yLow) < 1e-6) {
            throw new IllegalStateException("稳态差太小，无法计算");
        }

        double[] s = new double[W];
        for (int i = 0; i < W; i++) {
            s[i] = (yCurve.get(i) - yLow) / (yHigh - yLow);
        }

        int[] grid = jieyueDetectVerticalBorders(cropped);
        int xLeftPx = grid[0];
        int xRightPx = grid[1];

        if (xRightPx <= xLeftPx || xRightPx - xLeftPx < 10) {
            xLeftPx = 0;
            xRightPx = W - 1;
        }

        double x5 = jieyueFindCrossingPoint(s, 0.05, true);
        double x95 = jieyueFindCrossingPoint(s, 0.95, true);

        double pixelSpan = xRightPx - xLeftPx;
        double timeSpan = tRight - tLeft;

        double t5 = tLeft + ((x5 - xLeftPx) / pixelSpan) * timeSpan;
        double t95 = tLeft + ((x95 - xLeftPx) / pixelSpan) * timeSpan;
        double tStep = t95 - t5;

        resp.put("t5", jieyueRoundToDecimal(t5, 6));
        resp.put("t95", jieyueRoundToDecimal(t95, 6));
        resp.put("tStep", jieyueRoundToDecimal(tStep, 6));
        return resp;
    }

    private static List<Integer> jieyueExtractRedCurve(Mat image, int width, int height) {
        List<Integer> yCurve = new ArrayList<>(width);
        for (int x = 0; x < width; x++) {
            int bestY = -1;
            double bestRedness = -1;
            for (int y = 0; y < height; y++) {
                double[] bgr = image.get(y, x);
                double redness = 2 * bgr[2] - bgr[1] - bgr[0];
                if (redness > bestRedness) {
                    bestRedness = redness;
                    bestY = y;
                }
            }
            yCurve.add(bestY);
        }
        return yCurve;
    }

    private static List<Integer> jieyueMovingAvg(List<Integer> arr, int win) {
        int n = arr.size();
        int k = Math.max(1, (win % 2 == 1 ? win : win + 1));
        int r = k / 2;
        List<Integer> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            int sum = 0, cnt = 0;
            for (int j = i - r; j <= i + r; j++) {
                int jj = Math.min(Math.max(j, 0), n - 1);
                sum += arr.get(jj);
                cnt++;
            }
            out.add(Math.round(sum * 1f / cnt));
        }
        return out;
    }

    private static double jieyueMedian(List<Integer> list) {
        int n = list.size();
        int[] sorted = new int[n];
        for (int i = 0; i < n; i++) sorted[i] = list.get(i);
        Arrays.sort(sorted);
        return (n % 2 == 1) ? sorted[n / 2] : (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0;
    }

    private static int[] jieyueDetectVerticalBorders(Mat roiBGR) {
        Mat gray = new Mat();
        Imgproc.cvtColor(roiBGR, gray, Imgproc.COLOR_BGR2GRAY);
        int W = gray.cols(), H = gray.rows();
        double[] colScore = new double[W];
        for (int x = 0; x < W; x++) {
            double s = 0;
            for (int y = 0; y < H; y++) {
                s += (255.0 - gray.get(y, x)[0]);
            }
            colScore[x] = s;
        }
        colScore = jieyueSmooth(colScore, 9);
        int leftMaxX = 0;
        double leftMax = -1;
        int leftEnd = Math.max(1, (int) (0.20 * W));
        for (int x = 0; x < leftEnd; x++) {
            if (colScore[x] > leftMax) {
                leftMax = colScore[x];
                leftMaxX = x;
            }
        }
        int rightMaxX = W - 1;
        double rightMax = -1;
        int rightStart = Math.min(W - 1, (int) (0.80 * W));
        for (int x = rightStart; x < W; x++) {
            if (colScore[x] > rightMax) {
                rightMax = colScore[x];
                rightMaxX = x;
            }
        }
        return new int[]{leftMaxX, rightMaxX};
    }

    private static double[] jieyueSmooth(double[] a, int win) {
        int n = a.length;
        int k = Math.max(1, (win % 2 == 1 ? win : win + 1));
        int r = k / 2;
        double[] out = new double[n];
        for (int i = 0; i < n; i++) {
            double s = 0;
            int c = 0;
            for (int j = i - r; j <= i + r; j++) {
                int jj = Math.min(Math.max(j, 0), n - 1);
                s += a[jj];
                c++;
            }
            out[i] = s / c;
        }
        return out;
    }

    private static double jieyueFindCrossingPoint(double[] s, double threshold, boolean risingEdge) {
        for (int i = 1; i < s.length; i++) {
            if (risingEdge) {
                if (s[i - 1] < threshold && s[i] >= threshold) {
                    if (Math.abs(s[i] - s[i - 1]) < 1e-12) return i - 1;
                    return (i - 1) + (i - (i - 1)) * (threshold - s[i - 1]) / (s[i] - s[i - 1]);
                }
            } else {
                if (s[i - 1] > threshold && s[i] <= threshold) {
                    if (Math.abs(s[i] - s[i - 1]) < 1e-12) return i - 1;
                    return (i - 1) + (i - (i - 1)) * (threshold - s[i - 1]) / (s[i] - s[i - 1]);
                }
            }
        }
        int closestIndex = 0;
        double minDiff = Math.abs(s[0] - threshold);
        for (int i = 1; i < s.length; i++) {
            double diff = Math.abs(s[i] - threshold);
            if (diff < minDiff) {
                minDiff = diff;
                closestIndex = i;
            }
        }
        return closestIndex;
    }

    private static double jieyueRoundToDecimal(double value, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }

    // ===================================================================
    // 子功能五：控制曲线响应时间测算算法块
    // ===================================================================

    public static Map<String, Object> computeControlCurveResponse(Mat roiBGR, double tLeftSec, double tRightSec) {
        Map<String, Object> map = new LinkedHashMap<>();
        Integer xGreen = xiangyingEstimateDominantVerticalX(roiBGR, true);
        Integer xBlue = xiangyingEstimateDominantVerticalX(roiBGR, false);

        map.put("xGreenPx", xGreen);
        map.put("xBluePx", xBlue);

        if (xGreen == null || xBlue == null) {
            map.put("note", "未能可靠提取蓝/绿竖直列（可调颜色阈值/内核高度）");
        } else {
            double span = tRightSec - tLeftSec;
            if (roiBGR.width() <= 1 || span <= 0) {
                map.put("note", "区域宽度或时间跨度异常");
            } else {
                double timeGreen = tLeftSec + (xGreen / (double) (roiBGR.width() - 1)) * span;
                double timeBlue = tLeftSec + (xBlue / (double) (roiBGR.width() - 1)) * span;
                map.put("timeGreen", timeGreen);
                map.put("timeBlue", timeBlue);
                map.put("responseTime", timeBlue - timeGreen);
            }
        }
        return map;
    }

    private static Integer xiangyingEstimateDominantVerticalX(Mat roiBGR, boolean green) {
        if (roiBGR.empty()) return null;
        Mat hsv = new Mat();
        Imgproc.cvtColor(roiBGR, hsv, Imgproc.COLOR_BGR2HSV);
        Scalar low = green ? new Scalar(35, 60, 50) : new Scalar(100, 80, 60);
        Scalar high = green ? new Scalar(85, 255, 255) : new Scalar(130, 255, 255);
        Mat maskHSV = new Mat();
        Core.inRange(hsv, low, high, maskHSV);

        List<Mat> bgr = new ArrayList<>(3);
        Core.split(roiBGR, bgr);
        Mat B = bgr.get(0), G = bgr.get(1);
        Mat excl = new Mat();
        Mat cond1 = new Mat(), cond2 = new Mat();
        if (green) {
            Imgproc.threshold(G, cond1, 80, 255, Imgproc.THRESH_BINARY);
            Core.subtract(G, B, excl);
            Imgproc.threshold(excl, cond2, 25, 255, Imgproc.THRESH_BINARY);
            Core.bitwise_and(cond1, cond2, excl);
        } else {
            Imgproc.threshold(B, cond1, 80, 255, Imgproc.THRESH_BINARY);
            Core.subtract(B, G, excl);
            Imgproc.threshold(excl, cond2, 25, 255, Imgproc.THRESH_BINARY);
            Core.bitwise_and(cond1, cond2, excl);
        }
        Mat mask = new Mat();
        Core.bitwise_and(maskHSV, excl, mask);

        int H = mask.rows();
        int kernelH = Math.max(7, Math.min(25, H / 18));
        Mat kVert = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(1, kernelH));
        Mat opened = new Mat();
        Imgproc.morphologyEx(mask, opened, Imgproc.MORPH_OPEN, kVert);
        Mat closed = new Mat();
        Imgproc.morphologyEx(opened, closed, Imgproc.MORPH_CLOSE, kVert);

        int W = closed.cols();
        int[] colCount = new int[W];
        for (int x = 0; x < W; x++) {
            Mat col = closed.col(x);
            colCount[x] = Core.countNonZero(col);
            col.release();
        }
        double[] sm = xiangyingSmooth(colCount, 9);
        int bestX = -1;
        double bestV = -1;
        for (int x = 0; x < W; x++) {
            if (sm[x] > bestV) {
                bestV = sm[x];
                bestX = x;
            }
        }

        double minSupport = Math.max(3, H * 0.10);
        if (bestX < 0 || bestV < minSupport) return null;

        int L = Math.max(0, bestX - 2), R = Math.min(W - 1, bestX + 2);
        double sumWX = 0, sumW = 0;
        for (int x = L; x <= R; x++) {
            sumWX += x * sm[x];
            sumW += sm[x];
        }
        int xRounded = (int) Math.round(sumW > 1e-6 ? (sumWX / sumW) : bestX);

        hsv.release();
        maskHSV.release();
        B.release();
        G.release();
        excl.release();
        cond1.release();
        cond2.release();
        mask.release();
        opened.release();
        closed.release();
        kVert.release();
        for (Mat m : bgr) m.release();
        return xRounded;
    }

    private static double[] xiangyingSmooth(int[] a, int win) {
        int n = a.length;
        int k = Math.max(1, (win % 2 == 1 ? win : win + 1));
        int r = k / 2;
        double[] out = new double[n];
        for (int i = 0; i < n; i++) {
            double s = 0;
            int c = 0;
            for (int j = i - r; j <= i + r; j++) {
                int jj = Math.min(Math.max(j, 0), n - 1);
                s += a[jj];
                c++;
            }
            out[i] = s / c;
        }
        return out;
    }
}
