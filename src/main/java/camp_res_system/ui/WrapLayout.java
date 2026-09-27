package camp_res_system.ui;

import java.awt.*;

/** Flow layout whose preferred height accounts for wrapping at the available width. */
final class WrapLayout extends FlowLayout {
    WrapLayout(int horizontalGap, int verticalGap) {
        super(LEFT, horizontalGap, verticalGap);
    }

    @Override
    public Dimension preferredLayoutSize(Container target) {
        return measure(target);
    }

    @Override
    public Dimension minimumLayoutSize(Container target) {
        Dimension size = measure(target);
        size.width = 0;
        return size;
    }

    private Dimension measure(Container target) {
        synchronized (target.getTreeLock()) {
            int width = target.getWidth();
            if (width <= 0 && target.getParent() != null) width = target.getParent().getWidth();
            if (width <= 0) width = Integer.MAX_VALUE;
            Insets insets = target.getInsets();
            int available = width - insets.left - insets.right - getHgap() * 2;
            int rowWidth = 0, rowHeight = 0, totalHeight = 0, maxWidth = 0;
            for (Component component : target.getComponents()) {
                if (!component.isVisible()) continue;
                Dimension size = component.getPreferredSize();
                int gap = rowWidth == 0 ? 0 : getHgap();
                if (rowWidth > 0 && rowWidth + gap + size.width > available) {
                    maxWidth = Math.max(maxWidth, rowWidth);
                    totalHeight += rowHeight + getVgap();
                    rowWidth = 0;
                    rowHeight = 0;
                    gap = 0;
                }
                rowWidth += gap + size.width;
                rowHeight = Math.max(rowHeight, size.height);
            }
            maxWidth = Math.max(maxWidth, rowWidth);
            totalHeight += rowHeight;
            return new Dimension(
                    maxWidth + insets.left + insets.right + getHgap() * 2,
                    totalHeight + insets.top + insets.bottom + getVgap() * 2);
        }
    }
}
