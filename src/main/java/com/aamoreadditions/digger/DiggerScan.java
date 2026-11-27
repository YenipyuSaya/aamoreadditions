package com.aamoreadditions.digger;

/** Pure cursor and work-area policy of the Vertical Digger (AA order: X, then Z, then one layer down). */
public final class DiggerScan {
    private DiggerScan() {}

    public record Cursor(int x, int y, int z) {}

    /**
     * Inclusive column bounds relative to the machine (or in world space).
     * Strict Radius is the square {@code [-range, range]^2}; Strict Chunk is the union of every
     * 16x16 chunk that contains at least one column of that square, which is itself a rectangle.
     */
    public record Area(int minX, int maxX, int minZ, int maxZ) {
        public boolean contains(int x, int z) {
            return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
        }

        public int width() { return maxX - minX + 1; }
        public int depth() { return maxZ - minZ + 1; }
        public int columns() { return width() * depth(); }
    }

    /** AA: checkY below the world bottom = (re)start pending, at the bottom = finished, above = working. */
    public enum Phase { START, WORKING, DONE }

    public static Phase phase(int checkY, int minY) {
        return checkY < minY ? Phase.START : checkY == minY ? Phase.DONE : Phase.WORKING;
    }

    /**
     * Work-area columns relative to {@code (originX, originZ)}. {@code range} is the Strict Radius
     * (Phantom-Booster) range in both modes.
     */
    public static Area relative(int originX, int originZ, int range, boolean strictChunk) {
        int r = Math.max(0, range);
        if (!strictChunk) return new Area(-r, r, -r, r);
        int minX = Math.floorDiv(originX - r, 16) * 16 - originX;
        int maxX = Math.floorDiv(originX + r, 16) * 16 + 15 - originX;
        int minZ = Math.floorDiv(originZ - r, 16) * 16 - originZ;
        int maxZ = Math.floorDiv(originZ + r, 16) * 16 + 15 - originZ;
        return new Area(minX, maxX, minZ, maxZ);
    }

    public static Cursor start(int originY, Area area) {
        return new Cursor(area.minX(), originY - 1, area.minZ());
    }

    /**
     * Layer-by-layer: X increases first, then Z; after the last column of the band, Y decreases by {@code yStep}
     * (the vertical span just processed) and is clamped to {@code minY} so the scan becomes DONE
     * instead of wrapping into START.
     */
    public static Cursor next(Cursor c, Area area, int yStep, int minY) {
        int x = c.x() + 1, y = c.y(), z = c.z();
        if (x > area.maxX()) {
            x = area.minX();
            z++;
            if (z > area.maxZ()) {
                z = area.minZ();
                y = Math.max(minY, y - Math.max(1, yStep));
            }
        }
        return new Cursor(x, y, z);
    }

    /**
     * A running scan restarts from the top when the effective (Phantom Booster) range, the work-area
     * mode, or the vertical span (range-upgrade N; 0 = unknown, never a reason) changes mid-scan.
     */
    public static boolean restartForArea(int scanRange, boolean scanChunk, int scanSpan,
                                        int range, boolean chunk, int span, Phase phase) {
        return phase == Phase.WORKING && (scanRange != range || scanChunk != chunk
                || (scanSpan != 0 && span != 0 && scanSpan != span));
    }

    /** A saved cursor may be resumed after a reload when it is inside the saved scan box (or finished). */
    public static boolean resumable(Cursor c, int originY, int minY, int originX, int originZ, int scanRange, boolean scanChunk) {
        if (scanRange <= 0 || c.y() < minY || c.y() > originY - 1) return false;
        return relative(originX, originZ, scanRange, scanChunk).contains(c.x(), c.z());
    }

    /** First non-empty placeable entry in slot order (non-placeable entries are skipped), or -1. */
    public static <T> int firstPlaceable(java.util.List<T> slots, java.util.function.Predicate<T> empty,
                                         java.util.function.Predicate<T> placeable, int from) {
        for (int i = Math.max(0, from); i < slots.size(); i++) {
            T t = slots.get(i);
            if (!empty.test(t) && placeable.test(t)) return i;
        }
        return -1;
    }
}
