package camp_res_system.ui;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;

import javax.swing.*;

/** Decorative original project photograph, cropped to fit without stretching. */
final class PhotoPanel extends JPanel {
    private static final Image PHOTO = load();

    private static Image load() {
        var resource = PhotoPanel.class.getResource("/images/campsite.jpg");
        return resource == null ? null : new ImageIcon(resource).getImage();
    }

    PhotoPanel() {
        super(new BorderLayout(12, 12));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(26, 28, 26, 28));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.clip(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 24, 24));
        g.setColor(Theme.FOREST);
        g.fillRect(0, 0, getWidth(), getHeight());
        if (PHOTO != null) {
            double scale =
                    Math.max(
                            (double) getWidth() / PHOTO.getWidth(null),
                            (double) getHeight() / PHOTO.getHeight(null));
            int w = (int) (PHOTO.getWidth(null) * scale), h = (int) (PHOTO.getHeight(null) * scale);
            g.drawImage(PHOTO, (getWidth() - w) / 2, (getHeight() - h) / 2, w, h, this);
        }
        g.setPaint(
                new GradientPaint(
                        0,
                        0,
                        new Color(10, 34, 26, 185),
                        getWidth(),
                        getHeight(),
                        new Color(10, 34, 26, 85)));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.dispose();
    }
}
