package br.com.uaicortes;

import java.util.Arrays;
import java.util.List;

/** Local visual comparison. Straight conversion from e.g. 24 to 30 fps is not a reversal. */
public final class ExportFrameOrder {
    public static final int WIDTH = 40, HEIGHT = 64;
    public static final int PIXELS = WIDTH * HEIGHT;
    private static final long SEARCH_US = 280_000L;
    private static final long MAX_BACKSTEP_US = 360_000L;

    public static final class Frame {
        public final long timeUs;
        public final byte[] luma;
        public Frame(long timeUs, byte[] luma) {
            if (luma.length != PIXELS) throw new IllegalArgumentException("grid size");
            this.timeUs = timeUs;
            this.luma = luma;
        }
    }

    public static final class Result {
        public final int frames, matched, reversals;
        public final long firstReversalUs;
        Result(int frames, int matched, int reversals, long firstReversalUs) {
            this.frames = frames;
            this.matched = matched;
            this.reversals = reversals;
            this.firstReversalUs = firstReversalUs;
        }
        public boolean sufficient() { return matched >= 8 && matched * 100 >= frames * 30; }
        public boolean hasPattern() { return sufficient() && reversals >= 2; }
    }

    public static Result compare(List<Frame> source, long sourceStartUs, List<Frame> output, long outputStartUs) {
        if (source.size() < 8 || output.size() < 8) return new Result(output.size(), 0, 0, -1L);
        // Center each pixel in time to reduce the weight of the fixed background or logo.
        float[][] a = describe(source), b = describe(output);
        int[] matches = new int[output.size()];
        Arrays.fill(matches, -1);
        int count = 0;
        for (int i = 0; i < output.size(); i++) {
            if (b[i] == null) continue;
            long expected = sourceStartUs + output.get(i).timeUs - outputStartUs;
            float best = -2f, runnerUp = -2f;
            int idx = -1;
            for (int j = 0; j < source.size(); j++) {
                if (a[j] == null || Math.abs(source.get(j).timeUs - expected) > SEARCH_US) continue;
                float score = 0;
                for (int k = 0; k < PIXELS; k++) score += b[i][k] * a[j][k];
                if (score > best) {
                    runnerUp = best;
                    best = score;
                    idx = j;
                } else if (score > runnerUp) runnerUp = score;
            }
            if (best >= .76f && best - runnerUp >= .0025f) {
                matches[i] = idx;
                count++;
            }
        }
        int reversals = 0;
        long first = -1L;
        for (int i = 1; i < matches.length; i++) {
            int previous = matches[i - 1], next = matches[i];
            if (previous < 0 || next < 0 || previous <= next) continue;
            Frame at = output.get(i);
            long sourceStep = source.get(previous).timeUs - source.get(next).timeUs;
            long displayStep = at.timeUs - output.get(i - 1).timeUs;
            if (sourceStep <= 0 || sourceStep > MAX_BACKSTEP_US || displayStep <= 0 || displayStep > 120_000L)
                continue;
            // A backwards match must replay a source frame already observed nearby, then advance again.
            boolean seen = false, advances = false;
            for (int j = Math.max(0, i - 6); j < i - 1; j++) if (matches[j] == next) seen = true;
            for (int j = i + 1; j < Math.min(i + 6, matches.length); j++)
                if (matches[j] >= previous) advances = true;
            if (seen && advances) {
                if (first < 0) first = at.timeUs;
                reversals++;
            }
        }
        return new Result(output.size(), count, reversals, first);
    }

    private static float[][] describe(List<Frame> frames) {
        float[] mean = new float[PIXELS];
        for (Frame f : frames)
            for (int i = 0; i < PIXELS; i++) mean[i] += (f.luma[i] & 255);
        for (int i = 0; i < PIXELS; i++) mean[i] /= frames.size();
        float[][] result = new float[frames.size()][];
        for (int index = 0; index < frames.size(); index++) {
            byte[] luma = frames.get(index).luma;
            float[] v = new float[PIXELS];
            float DC = 0f;
            for (int i = 0; i < PIXELS; i++) {
                v[i] = (luma[i] & 255) - mean[i];
                DC += v[i];
            }
            DC /= PIXELS;
            float sum = 0f;
            for (int i = 0; i < PIXELS; i++) {
                v[i] -= DC;
                sum += v[i] * v[i];
            }
            if (sum < PIXELS * .50f) continue; // Still/featureless content is inconclusive.
            float divisor = (float) Math.sqrt(sum);
            for (int i = 0; i < PIXELS; i++) v[i] /= divisor;
            result[index] = v;
        }
        return result;
    }
}
