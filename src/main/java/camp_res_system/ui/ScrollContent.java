package camp_res_system.ui;

import java.awt.*;

import javax.swing.*;

/** Fit forms to the viewport width; only scroll vertically when content needs more room. */
final class ScrollContent extends JPanel implements Scrollable {
    ScrollContent(LayoutManager layout) {
        super(layout);
        setOpaque(false);
    }

    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
        return 20;
    }

    public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
        return Math.max(20, visible.height - 40);
    }

    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    public boolean getScrollableTracksViewportHeight() {
        return getParent() instanceof JViewport viewport
                && getPreferredSize().height < viewport.getHeight();
    }
}
