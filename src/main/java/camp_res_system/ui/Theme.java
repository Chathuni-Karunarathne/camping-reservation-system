package camp_res_system.ui;

import com.formdev.flatlaf.FlatLightLaf;

import java.awt.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

public final class Theme {
    public static final Color FOREST = new Color(23, 83, 66),
            SAGE = new Color(229, 241, 234),
            PAPER = new Color(245, 247, 249),
            INK = new Color(28, 43, 40),
            MUTED = new Color(94, 111, 108),
            LINE = new Color(222, 230, 227),
            NAV = new Color(20, 47, 40);

    private Theme() {}

    public static void install() {
        FlatLightLaf.setup();
        UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 14));
        UIManager.put("Panel.background", PAPER);
        UIManager.put("Label.foreground", INK);
        UIManager.put("Button.arc", 16);
        UIManager.put("Button.background", Color.WHITE);
        UIManager.put("Button.foreground", INK);
        UIManager.put("Button.default.background", FOREST);
        UIManager.put("Button.default.foreground", Color.WHITE);
        UIManager.put("Component.arc", 14);
        UIManager.put("TextComponent.arc", 14);
        UIManager.put("Component.minimumHeight", 42);
        UIManager.put("Component.borderColor", LINE);
        UIManager.put("Component.focusWidth", 2);
        UIManager.put("Component.focusedBorderColor", FOREST);
        UIManager.put("TextComponent.selectionBackground", SAGE);
        UIManager.put("TextField.margin", new Insets(8, 12, 8, 12));
        UIManager.put("PasswordField.margin", new Insets(8, 12, 8, 12));
        UIManager.put("PasswordField.showRevealButton", true);
        UIManager.put("TableHeader.background", new Color(239, 244, 242));
        UIManager.put("TableHeader.foreground", FOREST);
        UIManager.put("Table.alternateRowColor", new Color(248, 250, 246));
        UIManager.put("Table.cellMargins", new Insets(0, 10, 0, 10));
        UIManager.put(
                "ScrollPane.border", BorderFactory.createLineBorder(new Color(225, 231, 224)));
        UIManager.put("Component.focusColor", FOREST);
        UIManager.put("Table.rowHeight", 38);
        UIManager.put("Table.selectionBackground", SAGE);
        UIManager.put("Table.selectionForeground", INK);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("TableHeader.height", 46);
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("Viewport.background", Color.WHITE);
    }

    public static JPanel panel(LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setOpaque(false);
        return p;
    }

    public static JPanel card() {
        JPanel p = new JPanel(new BorderLayout(16, 16)) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(getBackground());
                g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 22, 22);
                g.setColor(LINE);
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 22, 22);
                g.dispose();
            }
        };
        p.setOpaque(false);
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(22, 22, 22, 22));
        return p;
    }

    public static JLabel heading(String text, int size) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD, (float) size));
        return label;
    }

    public static JButton button(String text, boolean primary) {
        JButton b = new JButton(text);
        b.setMargin(new Insets(10, 16, 10, 16));
        b.setFont(b.getFont().deriveFont(Font.BOLD));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (primary) {
            b.setBackground(FOREST);
            b.setForeground(Color.WHITE);
        }
        return b;
    }

    /** Small scalable line icons, painted using the component's current text color. */
    public static Icon icon(String key) {
        return new Icon() {
            public int getIconWidth() { return 22; }
            public int getIconHeight() { return 22; }
            public void paintIcon(Component c, Graphics graphics, int x, int y) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.translate(x, y);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(c.getForeground());
                g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                switch (key) {
                    case "dashboard" -> {
                        for (int a : new int[] {3, 13}) for (int b : new int[] {3, 13})
                            g.drawRoundRect(a, b, 6, 6, 2, 2);
                    }
                    case "reservations" -> {
                        g.drawRoundRect(3, 5, 16, 15, 3, 3);
                        g.drawLine(3, 10, 19, 10);
                        g.drawLine(7, 2, 7, 7); g.drawLine(15, 2, 15, 7);
                    }
                    case "reports" -> {
                        g.drawLine(3, 3, 3, 19); g.drawLine(3, 19, 20, 19);
                        g.drawLine(7, 15, 7, 11); g.drawLine(12, 15, 12, 7);
                        g.drawLine(17, 15, 17, 3);
                    }
                    case "feedback" -> {
                        g.drawRoundRect(2, 3, 18, 13, 5, 5);
                        g.drawLine(6, 16, 6, 20); g.drawLine(6, 20, 11, 16);
                        g.drawLine(7, 8, 15, 8); g.drawLine(7, 12, 12, 12);
                    }
                    case "logout" -> {
                        g.drawLine(9, 3, 3, 3); g.drawLine(3, 3, 3, 19);
                        g.drawLine(3, 19, 9, 19); g.drawLine(8, 11, 20, 11);
                        g.drawLine(16, 7, 20, 11); g.drawLine(16, 15, 20, 11);
                    }
                    default -> {
                        g.drawPolygon(new int[] {2, 11, 20}, new int[] {19, 3, 19}, 3);
                        g.drawLine(11, 10, 15, 19); g.drawLine(11, 10, 7, 19);
                    }
                }
                g.dispose();
            }
        };
    }

    public static JLabel badge(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(FOREST);
        label.setBackground(SAGE);
        label.setOpaque(true);
        label.setBorder(new EmptyBorder(6, 10, 6, 10));
        label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
        return label;
    }

    public static JPanel emptyState(String title, String help) {
        JPanel panel = panel(new BorderLayout(0, 10));
        panel.setBorder(new EmptyBorder(12, 16, 12, 16));
        JLabel heading = heading(title, 21);
        heading.setIcon(icon("campsites"));
        heading.setIconTextGap(10);
        panel.add(heading, BorderLayout.NORTH);
        JTextArea detail = paragraph(help);
        detail.setRows(3);
        panel.add(detail, BorderLayout.CENTER);
        return panel;
    }

    public static JTextArea paragraph(String text) {
        JTextArea a = new JTextArea(text);
        a.setEditable(false);
        a.setFocusable(false);
        ((javax.swing.text.DefaultCaret) a.getCaret())
                .setUpdatePolicy(javax.swing.text.DefaultCaret.NEVER_UPDATE);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setOpaque(false);
        a.setFont(UIManager.getFont("Label.font"));
        a.setForeground(MUTED);
        return a;
    }
}
