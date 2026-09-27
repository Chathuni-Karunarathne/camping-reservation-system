package camp_res_system.ui;

import com.formdev.flatlaf.FlatLightLaf;

import java.awt.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

public final class Theme {
    public static final Color FOREST = new Color(30, 70, 53),
            SAGE = new Color(226, 234, 222),
            PAPER = new Color(246, 247, 242),
            INK = new Color(33, 48, 40),
            MUTED = new Color(96, 112, 101);

    private Theme() {}

    public static void install() {
        FlatLightLaf.setup();
        UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        UIManager.put("Panel.background", PAPER);
        UIManager.put("Label.foreground", INK);
        UIManager.put("Button.arc", 18);
        UIManager.put("Component.arc", 14);
        UIManager.put("TextComponent.arc", 14);
        UIManager.put("Component.minimumHeight", 38);
        UIManager.put("TextField.margin", new Insets(8, 12, 8, 12));
        UIManager.put("PasswordField.margin", new Insets(8, 12, 8, 12));
        UIManager.put("PasswordField.showRevealButton", true);
        UIManager.put("TableHeader.background", SAGE);
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
        UIManager.put("TableHeader.height", 40);
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("Viewport.background", Color.WHITE);
    }

    public static JPanel panel(LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setOpaque(false);
        return p;
    }

    public static JPanel card() {
        JPanel p = new JPanel(new BorderLayout(16, 16));
        p.setBackground(Color.WHITE);
        p.setBorder(
                BorderFactory.createCompoundBorder(
                        new javax.swing.border.LineBorder(new Color(224, 231, 222), 1, true),
                        new EmptyBorder(20, 20, 20, 20)));
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
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (primary) {
            b.setBackground(FOREST);
            b.setForeground(Color.WHITE);
        }
        return b;
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
        JPanel panel = panel(new GridBagLayout());
        JPanel copy = panel(new BorderLayout(0, 12));
        JLabel heading = heading(title, 21);
        heading.setHorizontalAlignment(SwingConstants.CENTER);
        copy.add(heading, BorderLayout.NORTH);
        JTextArea detail = paragraph(help);
        detail.setColumns(34);
        detail.setRows(3);
        copy.add(detail, BorderLayout.CENTER);
        panel.add(copy);
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
