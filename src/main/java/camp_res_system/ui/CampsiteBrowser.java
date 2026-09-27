package camp_res_system.ui;

import camp_res_system.model.Campsite;

import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.*;
import javax.swing.event.*;

/** Customer browsing presents actionable listings rather than requiring table selection. */
final class CampsiteBrowser extends Screen {
    private final Consumer<Campsite> open;
    private final List<Campsite> sites = new ArrayList<>();
    private final JPanel listings = Theme.panel(new GridLayout(0, 2, 16, 16));
    private final JTextField search = new JTextField(22);
    private final JComboBox<String> province = new JComboBox<>();
    private final JComboBox<String> sort = new JComboBox<>();
    private final JLabel count = new JLabel();
    private JScrollPane scroll;

    CampsiteBrowser(AppFrame host, Consumer<Campsite> open) {
        super(host);
        this.open = open;
    }

    void render() {
        JPanel top = Theme.panel(new BorderLayout(0, 18));
        top.add(pageHeader("browse.title", "browse.subtitle"), BorderLayout.NORTH);
        search.getAccessibleContext().setAccessibleName(t("search"));
        search.putClientProperty("JTextField.placeholderText", t("search.placeholder"));
        search.putClientProperty("JTextField.showClearButton", true);
        province.addItem(t("allProvinces"));
        province.getAccessibleContext().setAccessibleName(t("province"));
        sort.getAccessibleContext().setAccessibleName(t("sort"));
        for (String key : new String[] {"sort.name", "sort.low", "sort.high"}) sort.addItem(t(key));
        JPanel filters = Theme.card();
        filters.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        filters.add(
                row(
                        new JLabel(t("search")),
                        search,
                        province,
                        sort,
                        action(
                                "resetFilters",
                                false,
                                () -> {
                                    search.setText("");
                                    province.setSelectedIndex(0);
                                    sort.setSelectedIndex(0);
                                })),
                BorderLayout.CENTER);
        top.add(filters, BorderLayout.CENTER);
        top.add(count, BorderLayout.SOUTH);
        content.add(top, BorderLayout.NORTH);
        JPanel wrapper = new ScrollContent(new BorderLayout());
        wrapper.add(listings, BorderLayout.NORTH);
        scroll = new JScrollPane(wrapper);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Theme.PAPER);
        scroll.getVerticalScrollBar().setUnitIncrement(22);
        scroll.getViewport()
                .addComponentListener(
                        new ComponentAdapter() {
                            @Override
                            public void componentResized(ComponentEvent event) {
                                int columns = scroll.getViewport().getWidth() < 720 ? 1 : 2;
                                GridLayout layout = (GridLayout) listings.getLayout();
                                if (layout.getColumns() != columns) {
                                    layout.setColumns(columns);
                                    listings.revalidate();
                                }
                            }
                        });
        content.add(scroll, BorderLayout.CENTER);
        content.add(Theme.paragraph(t("browse.note")), BorderLayout.SOUTH);
        search.getDocument()
                .addDocumentListener(
                        new DocumentListener() {
                            public void insertUpdate(DocumentEvent e) {
                                filter();
                            }

                            public void removeUpdate(DocumentEvent e) {
                                filter();
                            }

                            public void changedUpdate(DocumentEvent e) {
                                filter();
                            }
                        });
        province.addActionListener(e -> filter());
        sort.addActionListener(e -> filter());
        work(
                () -> app.campsites.search("", false),
                result -> {
                    sites.addAll(result);
                    result.stream()
                            .map(Campsite::province)
                            .distinct()
                            .sorted()
                            .forEach(province::addItem);
                    filter();
                });
    }

    private void filter() {
        String query = search.getText().trim().toLowerCase(Locale.ROOT);
        Comparator<Campsite> order =
                switch (sort.getSelectedIndex()) {
                    case 1 -> Comparator.comparingLong(Campsite::rateCents);
                    case 2 -> Comparator.comparingLong(Campsite::rateCents).reversed();
                    default -> Comparator.comparing(Campsite::name, String.CASE_INSENSITIVE_ORDER);
                };
        List<Campsite> matches =
                sites.stream()
                        .filter(
                                s ->
                                        province.getSelectedIndex() == 0
                                                || s.province().equals(province.getSelectedItem()))
                        .filter(
                                s ->
                                        (s.id()
                                                        + " "
                                                        + s.name()
                                                        + " "
                                                        + s.province()
                                                        + " "
                                                        + BigDecimal.valueOf(s.rateCents(), 2)
                                                                .toPlainString())
                                                .toLowerCase(Locale.ROOT)
                                                .contains(query))
                        .sorted(order)
                        .toList();
        listings.removeAll();
        count.setText(matches.size() + " " + t("browse.results"));
        if (matches.isEmpty()) {
            JPanel empty = Theme.card();
            empty.setPreferredSize(new Dimension(320, 230));
            empty.add(
                    Theme.emptyState(
                            t("empty.campsites"),
                            t(sites.isEmpty() ? "browse.empty" : "browse.noMatch")));
            listings.add(empty);
        } else for (Campsite site : matches) listings.add(card(site));
        listings.revalidate();
        listings.repaint();
        if (scroll != null)
            SwingUtilities.invokeLater(() -> scroll.getViewport().setViewPosition(new Point(0, 0)));
    }

    private JPanel card(Campsite site) {
        JPanel card = Theme.card();
        card.setPreferredSize(new Dimension(350, 350));
        JPanel header = Theme.panel(new BorderLayout(12, 12));
        header.add(Theme.badge(site.province()), BorderLayout.WEST);
        JLabel id = new JLabel(site.id());
        id.setForeground(Theme.MUTED);
        header.add(id, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);
        JPanel detail = Theme.panel(new BorderLayout(0, 10));
        JTextArea name = Theme.paragraph(site.name());
        name.setFont(name.getFont().deriveFont(Font.BOLD, 23f));
        name.setForeground(Theme.FOREST);
        name.setRows(2);
        detail.add(name, BorderLayout.NORTH);
        JTextArea description =
                Theme.paragraph(
                        site.description().isBlank()
                                ? t("browse.defaultDescription")
                                : site.description());
        description.setRows(3);
        JScrollPane descriptionScroll = new JScrollPane(description);
        descriptionScroll.setBorder(null);
        descriptionScroll.setOpaque(false);
        descriptionScroll.getViewport().setOpaque(false);
        detail.add(descriptionScroll, BorderLayout.CENTER);
        card.add(detail, BorderLayout.CENTER);
        JPanel footer = Theme.panel(new BorderLayout(0, 10));
        JPanel price = Theme.panel(new BorderLayout());
        price.add(Theme.heading(m.money(site.rateCents()), 22), BorderLayout.WEST);
        price.add(new JLabel(t("perPersonNight")), BorderLayout.EAST);
        footer.add(price, BorderLayout.NORTH);
        JButton button = action("viewBook", true, () -> open.accept(site));
        button.getAccessibleContext().setAccessibleName(t("viewBook") + ": " + site.name());
        footer.add(button, BorderLayout.SOUTH);
        card.add(footer, BorderLayout.SOUTH);
        return card;
    }
}
