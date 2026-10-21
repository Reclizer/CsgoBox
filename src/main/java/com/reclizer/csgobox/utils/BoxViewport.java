package com.reclizer.csgobox.utils;

/** One 16:9 design space for rendering, hit testing and drag distances. */
public record BoxViewport(double scale, double left, double top) {
    public static final int WIDTH = 480;
    public static final int HEIGHT = 270;

    public static BoxViewport fit(int width, int height) {
        double scale = Math.min(Math.max(1, width) / (double) WIDTH,
                Math.max(1, height) / (double) HEIGHT);
        return new BoxViewport(scale, (width - WIDTH * scale) / 2,
                (height - HEIGHT * scale) / 2);
    }

    public double localX(double x) { return (x - left) / scale; }
    public double localY(double y) { return (y - top) / scale; }
    public double localDelta(double delta) { return delta / scale; }
    public double screenX(double x) { return left + x * scale; }
    public double screenY(double y) { return top + y * scale; }
}
