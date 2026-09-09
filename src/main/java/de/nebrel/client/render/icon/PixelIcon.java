package de.nebrel.client.render.icon;

/**
 * A tiny hand-drawn bitmap: a square grid of filled and empty cells.
 *
 * <p>This exists in place of an image asset. {@code DrawContext} can fill
 * rectangles and draw the vanilla font, nothing else, and the client ships no
 * texture of its own — so an icon more detailed than a single glyph has to be
 * geometry too. A pixel grid is exactly that: each filled cell becomes one
 * (possibly merged) rectangle. See {@code RenderUtil.icon} for the draw.</p>
 *
 * <p>Kept Minecraft-free on purpose: the data is the part worth checking —
 * that every row is the same width, that only the two expected characters
 * appear, that nothing is empty by mistake — and none of that needs a game
 * instance to verify. {@code tools/verify-core.sh} compiles and tests this
 * class directly.</p>
 */
public final class PixelIcon {

    /** A filled cell. */
    public static final char FILLED = '#';
    /** An empty cell. */
    public static final char EMPTY = '.';

    private final String id;
    private final String[] rows;
    private final int size;

    /**
     * @param id   short name, for error messages and lookup
     * @param rows one string per row, top to bottom, {@link #FILLED}/{@link
     *             #EMPTY} only; must be square (as many rows as columns)
     */
    public PixelIcon(String id, String... rows) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("an icon needs an id");
        }
        if (rows == null || rows.length == 0) {
            throw new IllegalArgumentException("icon '" + id + "' has no rows");
        }
        int width = rows[0].length();
        if (width != rows.length) {
            throw new IllegalArgumentException("icon '" + id + "' is " + rows.length
                    + " rows but " + width + " columns wide; a pixel icon must be square");
        }
        for (String row : rows) {
            if (row.length() != width) {
                throw new IllegalArgumentException("icon '" + id
                        + "' has rows of differing length");
            }
            for (int i = 0; i < row.length(); i++) {
                char c = row.charAt(i);
                if (c != FILLED && c != EMPTY) {
                    throw new IllegalArgumentException("icon '" + id + "' row " + i
                            + " contains '" + c + "'; only '" + FILLED + "' and '"
                            + EMPTY + "' are allowed");
                }
            }
        }
        this.id = id;
        this.rows = rows.clone();
        this.size = width;
    }

    public String id() {
        return this.id;
    }

    /** Cells per side. Every icon in {@link Icons} shares one size. */
    public int size() {
        return this.size;
    }

    public boolean filled(int row, int col) {
        return this.rows[row].charAt(col) == FILLED;
    }

    /**
     * The filled spans of one row, as {@code [startCol, endColExclusive)}
     * pairs merged from consecutive cells.
     *
     * <p>Merging matters for the draw: one wide rectangle per run instead of
     * one small rectangle per cell means far fewer draw calls, and it also
     * avoids the hairline seams a run of individually-rounded adjacent
     * rectangles can show at odd scales.</p>
     */
    public int[] runsInRow(int row) {
        String line = this.rows[row];
        // Worst case is alternating cells - up to `size` single-cell runs,
        // two ints (start, end) apiece.
        int[] buffer = new int[this.size * 2];
        int count = 0;
        int col = 0;
        while (col < this.size) {
            if (line.charAt(col) == EMPTY) {
                col++;
                continue;
            }
            int start = col;
            while (col < this.size && line.charAt(col) == FILLED) {
                col++;
            }
            buffer[count++] = start;
            buffer[count++] = col;
        }
        int[] result = new int[count];
        System.arraycopy(buffer, 0, result, 0, count);
        return result;
    }

    /** True when at least one cell is filled. An all-empty icon is a mistake. */
    public boolean hasContent() {
        for (String row : this.rows) {
            if (row.indexOf(FILLED) >= 0) {
                return true;
            }
        }
        return false;
    }
}
