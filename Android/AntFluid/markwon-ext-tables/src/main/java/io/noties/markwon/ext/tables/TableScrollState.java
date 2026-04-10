package io.noties.markwon.ext.tables;

/**
 * Shared horizontal scroll state for all rows in a single table.
 */
public class TableScrollState {
    float scrollOffset = 0f;
    int contentWidth = 0;
    int viewWidth = 0;

    public float getMaxScroll() {
        return Math.max(0, contentWidth - viewWidth);
    }

    public void scrollBy(float dx) {
        scrollOffset = Math.max(0, Math.min(scrollOffset + dx, getMaxScroll()));
    }

    public boolean canScroll() {
        return contentWidth > viewWidth;
    }
}
