package camp_res_system.ui;

import camp_res_system.config.AppContext;
import camp_res_system.i18n.Messages;
import camp_res_system.model.*;
import camp_res_system.service.*;

import com.toedter.calendar.JDateChooser;

import java.awt.*;
import java.awt.event.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.logging.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

/** Shared application shell. All persistence and password work runs outside the EDT. */
public final class AppFrame extends JFrame {
    private static final Logger LOG = Logger.getLogger(AppFrame.class.getName());
    final AppContext app;
    final Messages m = new Messages();
    final JPanel content = Theme.panel(new BorderLayout(20, 20));
    private final JLabel notice = new JLabel(" ");
    private boolean setup;
    private String page = "dashboard";
    private boolean busy;

    public AppFrame(AppContext app, boolean setup) {
        this.app = app;
        this.setup = setup;
        setTitle("TentTrack · Camping Reservation System");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 720));
        setSize(1200, 820);
        setLocationRelativeTo(null);
        renderShell();
    }

    public Messages messages() {
        return m;
    }

    String t(String key) {
        return m.text(key);
    }

    boolean admin() {
        return app.session.current() != null && app.session.current().role() == User.Role.ADMIN;
    }

    JButton action(String key, boolean primary, Runnable run) {
        JButton b = Theme.button(t(key), primary);
        b.addActionListener(e -> guard(run));
        return b;
    }

    private void guard(Runnable run) {
        try {
            run.run();
        } catch (RuntimeException e) {
            showError(e);
        }
    }

    private void showError(Throwable error) {
        if (error instanceof AppException expected) {
            notice.setText(t(expected.key()));
            if (expected.getCause() != null)
                LOG.log(Level.WARNING, expected.key(), expected.getCause());
        } else {
            LOG.log(Level.SEVERE, "Application operation failed", error);
            notice.setText(t("error.unexpected"));
        }
        notice.setForeground(new Color(160, 45, 38));
        for (Window w : getOwnedWindows())
            if (w.isVisible() && w instanceof JDialog d) {
                Object label = d.getRootPane().getClientProperty("errorLabel");
                if (label instanceof JTextArea area) {
                    area.setText(notice.getText());
                    area.setVisible(true);
                    area.getParent().revalidate();
                }
            }
    }

    void success(String key) {
        notice.setForeground(Theme.FOREST);
        notice.setText(t(key));
    }

    <T> void work(Callable<T> task, Consumer<T> done) {
        if (busy) return;
        busy = true;
        notice.setText(t("working"));
        notice.setForeground(Theme.MUTED);
        List<JRootPane> roots = new ArrayList<>();
        roots.add(getRootPane());
        for (Window w : getOwnedWindows())
            if (w.isVisible() && w instanceof JDialog d) roots.add(d.getRootPane());
        for (JRootPane root : roots) {
            JPanel glass = new JPanel();
            glass.setOpaque(false);
            glass.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            glass.addMouseListener(new MouseAdapter() {});
            root.setGlassPane(glass);
            glass.setVisible(true);
        }
        // Disable focused controls too; glass panes alone do not block keyboard activation.
        List<Component> enabled = new ArrayList<>();
        for (JRootPane root : roots) disable(root.getContentPane(), enabled);
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return task.call();
            }

            @Override
            protected void done() {
                busy = false;
                for (JRootPane root : roots) root.getGlassPane().setVisible(false);
                for (Component c : enabled) c.setEnabled(true);
                notice.setText(" ");
                try {
                    done.accept(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    showError(e);
                } catch (ExecutionException e) {
                    showError(e.getCause());
                } catch (RuntimeException e) {
                    showError(e);
                }
            }
        }.execute();
    }

    private void disable(Container root, List<Component> enabled) {
        for (Component c : root.getComponents()) {
            if (c instanceof Container child) disable(child, enabled);
            if (c.isEnabled()
                    && (c instanceof AbstractButton
                            || c instanceof JTextField
                            || c instanceof JComboBox<?>
                            || c instanceof JSpinner
                            || c instanceof JTable)) {
                enabled.add(c);
                c.setEnabled(false);
            }
        }
    }

    private void renderShell() {
        getRootPane().setDefaultButton(null);
        JPanel root = new JPanel(new BorderLayout());
        setContentPane(root);
        JPanel top = Theme.panel(new BorderLayout());
        top.setOpaque(true);
        top.setBackground(Color.WHITE);
        top.setBorder(new EmptyBorder(18, 26, 18, 26));
        JLabel brand = Theme.heading("TentTrack", 24);
        brand.setIcon(Theme.icon("campsites"));
        brand.setIconTextGap(12);
        brand.setForeground(Theme.FOREST);
        top.add(brand, BorderLayout.WEST);
        JPanel right = Theme.panel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        right.add(
                new JLabel(
                        app.session.current() == null
                                ? t("tagline")
                                : app.session.current().fullName()
                                        + " · "
                                        + t(admin() ? "role.admin" : "role.user")));
        JComboBox<String> languages = new JComboBox<>(new String[] {"English", "Español"});
        languages.setSelectedIndex(m.locale().getLanguage().equals("es") ? 1 : 0);
        languages.getAccessibleContext().setAccessibleName(t("language"));
        languages.addActionListener(
                e -> {
                    m.setLocale(
                            languages.getSelectedIndex() == 1
                                    ? Locale.forLanguageTag("es-ES")
                                    : Locale.ENGLISH);
                    renderShell();
                });
        right.add(languages);
        top.add(right, BorderLayout.EAST);
        root.add(top, BorderLayout.NORTH);
        if (app.session.current() != null) {
            JPanel nav = new JPanel();
            nav.setBackground(Theme.NAV);
            nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
            nav.setBorder(new EmptyBorder(28, 18, 24, 18));
            nav.setPreferredSize(new Dimension(224, 0));
            JLabel eyebrow = new JLabel(t(admin() ? "admin.workspace" : "your.escape"));
            eyebrow.setFont(eyebrow.getFont().deriveFont(11f));
            eyebrow.setForeground(Theme.SAGE);
            nav.add(eyebrow);
            nav.add(Box.createVerticalStrut(24));
            for (String key :
                    admin()
                            ? new String[] {
                                "dashboard", "campsites", "reservations", "reports", "feedback"
                            }
                            : new String[] {"dashboard", "campsites", "reservations", "feedback"}) {
                JButton b = action(key, false, () -> showPage(key));
                b.setHorizontalAlignment(SwingConstants.LEFT);
                b.setIcon(Theme.icon(key));
                b.setIconTextGap(12);
                b.setToolTipText(t(key));
                b.getAccessibleContext().setAccessibleDescription(
                        key.equals(page) ? t("nav.current") : t(key));
                b.setFont(b.getFont().deriveFont(key.equals(page) ? Font.BOLD : Font.PLAIN));
                b.putClientProperty("JButton.buttonType", "borderless");
                b.setAlignmentX(Component.LEFT_ALIGNMENT);
                b.setMaximumSize(new Dimension(188, 48));
                b.setBackground(key.equals(page) ? Theme.SAGE : Theme.NAV);
                b.setForeground(key.equals(page) ? Theme.INK : Color.WHITE);
                nav.add(b);
                nav.add(Box.createVerticalStrut(9));
            }
            nav.add(Box.createVerticalGlue());
            JButton logout =
                    action(
                            "logout",
                            false,
                            () -> {
                                app.session.logout();
                                page = "dashboard";
                                renderShell();
                            });
            logout.setMaximumSize(new Dimension(185, 44));
            logout.setIcon(Theme.icon("logout"));
            logout.setIconTextGap(12);
            logout.setAlignmentX(Component.LEFT_ALIGNMENT);
            nav.add(logout);
            root.add(nav, BorderLayout.WEST);
        }
        content.setBorder(new EmptyBorder(22, 28, 22, 28));
        root.add(content, BorderLayout.CENTER);
        notice.setBorder(new EmptyBorder(10, 26, 12, 26));
        notice.setOpaque(true);
        notice.setBackground(Color.WHITE);
        root.add(notice, BorderLayout.SOUTH);
        if (app.session.current() == null) authView(false);
        else loadPage();
        revalidate();
        repaint();
    }

    void showPage(String key) {
        page = key;
        renderShell();
    }

    void loadPage() {
        content.removeAll();
        switch (page) {
            case "campsites" -> new CampsitesView(this).render();
            case "reservations" -> new ReservationsView(this).render();
            case "reports" -> new ReservationsView(this).reports();
            case "feedback" -> new FeedbackView(this).render();
            default -> new DashboardView(this).render();
        }
        content.revalidate();
        content.repaint();
    }

    JPanel pageHeader(String title, String subtitle) {
        JPanel p = Theme.panel(new BorderLayout(0, 8));
        p.add(Theme.heading(t(title), 30), BorderLayout.NORTH);
        p.add(Theme.paragraph(t(subtitle)), BorderLayout.CENTER);
        return p;
    }

    JPanel stack() {
        JPanel p = Theme.panel(new GridBagLayout());
        return p;
    }

    void field(JPanel form, int row, String label, JComponent component) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = row * 2;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(6, 0, 4, 0);
        JLabel l = new JLabel(t(label));
        l.setFont(l.getFont().deriveFont(Font.BOLD, 13f));
        l.setLabelFor(component);
        form.add(l, g);
        g.gridy++;
        g.insets = new Insets(0, 0, 4, 0);
        form.add(component, g);
        component.getAccessibleContext().setAccessibleName(t(label));
    }

    JPanel row(Component... components) {
        JPanel p = Theme.panel(new WrapLayout(10, 4));
        for (Component c : components) p.add(c);
        return p;
    }

    private void authView(boolean register) {
        content.removeAll();
        JPanel split = Theme.panel(new GridLayout(1, 2, 28, 0));
        JPanel story = new PhotoPanel();
        JLabel eyebrow = new JLabel(t("auth.eyebrow"));
        eyebrow.setForeground(Theme.SAGE);
        story.add(eyebrow, BorderLayout.NORTH);
        JPanel introduction = Theme.panel(new BorderLayout(0, 18));
        JTextArea title = Theme.paragraph(t("auth.headline"));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 38f));
        title.setForeground(Color.WHITE);
        title.setRows(3);
        introduction.add(title, BorderLayout.NORTH);
        JTextArea copy = Theme.paragraph(t("auth.story"));
        copy.setForeground(Color.WHITE);
        copy.setRows(4);
        introduction.add(copy, BorderLayout.SOUTH);
        story.add(introduction, BorderLayout.SOUTH);
        split.add(story);
        JPanel card = Theme.card();
        card.add(
                pageHeader(
                        setup ? "setup.title" : register ? "register" : "login",
                        setup ? "setup.subtitle" : "auth.subtitle"),
                BorderLayout.NORTH);
        JPanel form = new ScrollContent(new BorderLayout(0, 8));
        JPanel credentials = stack();
        JPanel credentialWrapper = Theme.panel(new BorderLayout());
        credentialWrapper.add(credentials, BorderLayout.NORTH);
        form.add(credentialWrapper, BorderLayout.CENTER);
        JTextField name = new JTextField(22),
                email = new JTextField(22),
                username = new JTextField(22);
        JPasswordField password = new JPasswordField(22), confirmation = new JPasswordField(22);
        int i = 0;
        if (register || setup) {
            JPanel identity = Theme.panel(new GridLayout(1, 2, 12, 0));
            JPanel nameField = stack(), emailField = stack();
            field(nameField, 0, "fullName", name);
            field(emailField, 0, "email", email);
            identity.add(nameField);
            identity.add(emailField);
            form.add(identity, BorderLayout.NORTH);
        }
        field(credentials, i++, "username", username);
        field(credentials, i++, "password", password);
        if (register || setup) field(credentials, i, "confirmPassword", confirmation);
        username.putClientProperty("JTextField.placeholderText", t("username.hint"));
        email.putClientProperty("JTextField.placeholderText", "name@example.com");
        password.putClientProperty("JTextField.placeholderText", t("password.hint"));
        JScrollPane formScroll = new JScrollPane(form);
        formScroll.setBorder(null);
        formScroll.getVerticalScrollBar().setUnitIncrement(16);
        card.add(formScroll, BorderLayout.CENTER);
        JPanel bottom = Theme.panel(new BorderLayout(0, 8));
        JButton submit =
                action(
                        setup ? "setup.create" : register ? "register" : "login",
                        true,
                        () -> {
                            String n = name.getText(),
                                    em = email.getText(),
                                    un = username.getText();
                            char[] pw = password.getPassword(),
                                    confirm = confirmation.getPassword();
                            password.setText("");
                            confirmation.setText("");
                            work(
                                    () ->
                                            setup
                                                    ? app.auth.setupAdmin(n, em, un, pw, confirm)
                                                    : register
                                                            ? app.auth.register(
                                                                    n, em, un, pw, confirm)
                                                            : app.auth.login(un, pw),
                                    u -> {
                                        setup = false;
                                        page = "dashboard";
                                        renderShell();
                                    });
                        });
        bottom.add(submit, BorderLayout.NORTH);
        if (!setup)
            bottom.add(
                    action(
                            register ? "backLogin" : "createAccount",
                            false,
                            () -> authView(!register)),
                    BorderLayout.CENTER);
        if (register || setup) bottom.add(Theme.paragraph(t("password.help")), BorderLayout.SOUTH);
        card.add(bottom, BorderLayout.SOUTH);
        split.add(card);
        content.add(split);
        getRootPane().setDefaultButton(submit);
        content.revalidate();
        content.repaint();
        SwingUtilities.invokeLater(() -> (register || setup ? name : username).requestFocusInWindow());
    }

    JTable table(String... keys) {
        String[] labels = Arrays.stream(keys).map(this::t).toArray(String[]::new);
        JTable table =
                new JTable(
                        new DefaultTableModel(labels, 0) {
                            @Override
                            public boolean isCellEditable(int r, int c) {
                                return false;
                            }

                            @Override
                            public Class<?> getColumnClass(int column) {
                                return getRowCount() == 0 || getValueAt(0, column) == null
                                        ? Object.class
                                        : getValueAt(0, column).getClass();
                            }
                        });
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.setRowHeight(46);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.getTableHeader().setFont(table.getFont().deriveFont(Font.BOLD, 13f));
        table.getTableHeader().setReorderingAllowed(false);
        table.setDefaultRenderer(
                LocalDate.class,
                new javax.swing.table.DefaultTableCellRenderer() {
                    @Override
                    protected void setValue(Object value) {
                        setText(value instanceof LocalDate date ? m.date(date) : "");
                    }
                });
        for (int i = 0; i < keys.length; i++) {
            var column = table.getColumnModel().getColumn(i);
            column.setPreferredWidth(
                    keys[i].equals("campsite") ? 180 : keys[i].equals("id") ? 70 : 120);
            if (keys[i].equals("rate") || keys[i].equals("total"))
                column.setCellRenderer(
                        new javax.swing.table.DefaultTableCellRenderer() {
                            @Override
                            protected void setValue(Object value) {
                                setHorizontalAlignment(SwingConstants.RIGHT);
                                setText(
                                        value instanceof Number number
                                                ? m.money(number.longValue())
                                                : "");
                            }
                        });
        }
        return table;
    }

    JDialog dialog(String key, int width, int height) {
        JDialog d = new JDialog(this, t(key), true);
        d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        d.setSize(width, height);
        d.setLocationRelativeTo(this);
        d.getRootPane()
                .registerKeyboardAction(
                        e -> d.dispose(),
                        KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                        JComponent.WHEN_IN_FOCUSED_WINDOW);
        return d;
    }

    void prepareDialog(JDialog dialog, JPanel root) {
        JPanel top = Theme.panel(new BorderLayout(0, 8));
        Component previous =
                ((BorderLayout) root.getLayout()).getLayoutComponent(BorderLayout.NORTH);
        if (previous != null) top.add(previous, BorderLayout.NORTH);
        JTextArea error = Theme.paragraph(" ");
        error.setForeground(new Color(160, 45, 38));
        error.setRows(2);
        error.setVisible(false);
        top.add(error, BorderLayout.SOUTH);
        root.add(top, BorderLayout.NORTH);
        dialog.getRootPane().putClientProperty("errorLabel", error);
        dialog.setContentPane(root);
        dialog.getRootPane()
                .registerKeyboardAction(
                        e -> dialog.dispose(),
                        KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                        JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    JDateChooser dateChooser(LocalDate date) {
        JDateChooser chooser = new JDateChooser();
        chooser.setLocale(m.locale());
        chooser.setDateFormatString("yyyy-MM-dd");
        chooser.setDate(Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        chooser.setPreferredSize(new Dimension(160, 34));
        return chooser;
    }

    LocalDate date(JDateChooser chooser) {
        return chooser.getDate() == null
                ? null
                : chooser.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    boolean confirm(String key) {
        return JOptionPane.showConfirmDialog(
                        this,
                        t(key),
                        t("confirm"),
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE)
                == JOptionPane.YES_OPTION;
    }

    void documentDialog(String title, String text) {
        JDialog d = new JDialog(this, title, true);
        d.setSize(760, 620);
        d.setLocationRelativeTo(this);
        JPanel root = Theme.card();
        JTextArea document = new JTextArea(text);
        document.setEditable(false);
        document.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        document.setLineWrap(true);
        document.setWrapStyleWord(true);
        document.setMargin(new Insets(18, 18, 18, 18));
        root.add(new JScrollPane(document), BorderLayout.CENTER);
        root.add(
                row(
                        action(
                                "saveText",
                                true,
                                () -> {
                                    JFileChooser chooser = new JFileChooser();
                                    chooser.setSelectedFile(new java.io.File("tenttrack.txt"));
                                    if (chooser.showSaveDialog(d) == JFileChooser.APPROVE_OPTION) {
                                        var path = chooser.getSelectedFile().toPath();
                                        if (Files.exists(path) && !confirm("overwrite.confirm"))
                                            return;
                                        work(
                                                () -> {
                                                    Files.writeString(
                                                            path, text, StandardCharsets.UTF_8);
                                                    return true;
                                                },
                                                v -> success("saved"));
                                    }
                                }),
                        action(
                                "print",
                                false,
                                () ->
                                        work(
                                                () -> document.print(),
                                                v -> {
                                                    if (v) success("printed");
                                                })),
                        action("close", false, d::dispose)),
                BorderLayout.SOUTH);
        prepareDialog(d, root);
        d.setVisible(true);
    }
}
