package com.reclizer.csgobox.utils;

/** Time-based inspection motion shared by the case and reward previews. Angles are radians. */
public final class PreviewRotation {
    private static final double SENSITIVITY = 0.018;
    private static final long RELEASE_HOLD_MS = 120;
    private final Axis yaw = new Axis(3);
    private final Axis pitch = new Axis(1.15);
    private boolean dragging;
    private long lastTime = -1;
    private long returnAt;

    public float yaw() { return (float) yaw.angle; }
    public float pitch() { return (float) pitch.angle; }
    public boolean isDragging() { return dragging; }

    public void grab(long now) {
        update(now);
        dragging = true;
        yaw.hold();
        pitch.hold();
    }

    public boolean drag(double dx, double dy, long now) {
        if (!dragging) return false;
        update(now);
        yaw.move(dx * SENSITIVITY);
        // Screen Y points down; a negative X-axis rotation tilts the front down with the pointer.
        pitch.move(-dy * SENSITIVITY);
        return true;
    }

    public void release(long now) {
        if (!dragging) return;
        update(now);
        dragging = false;
        returnAt = now + RELEASE_HOLD_MS;
    }

    public void update(long now) {
        if (lastTime < 0) { lastTime = now; return; }
        if (now <= lastTime) return;
        long start = lastTime;
        lastTime = now;
        if (dragging) {
            step((now - start) / 1000.0, 30);
            return;
        }
        // Split at the hold boundary so the same motion is produced at every frame rate.
        if (start < returnAt) {
            long heldUntil = Math.min(now, returnAt);
            step((heldUntil - start) / 1000.0, 30);
            start = heldUntil;
        }
        if (now > start) {
            yaw.target = pitch.target = 0;
            step((now - start) / 1000.0, 10);
        }
    }

    private void step(double seconds, double frequency) {
        yaw.step(seconds, frequency);
        pitch.step(seconds, frequency);
    }

    private static final class Axis {
        final double limit;
        double angle, velocity, target;
        Axis(double limit) { this.limit = limit; }
        void hold() { target = angle; velocity = 0; }
        void move(double delta) { target = Math.max(-limit, Math.min(limit, target + delta)); }
        void step(double seconds, double frequency) {
            // Exact critically damped spring: smooth velocity, no frame-dependent lerp or tick stepping.
            double offset = angle - target;
            double change = (velocity + frequency * offset) * seconds;
            double decay = Math.exp(-frequency * seconds);
            angle = target + (offset + change) * decay;
            velocity = (velocity - frequency * change) * decay;
            if (Math.abs(angle) > limit) {
                angle = Math.copySign(limit, angle);
                velocity = 0;
            }
            if (Math.abs(angle - target) < 0.0001 && Math.abs(velocity) < 0.001) {
                angle = target;
                velocity = 0;
            }
        }
    }
}
