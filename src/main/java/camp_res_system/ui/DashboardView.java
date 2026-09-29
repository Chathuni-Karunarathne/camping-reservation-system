package camp_res_system.ui;

import camp_res_system.model.*;
import camp_res_system.service.*;

import java.awt.*;
import java.awt.event.*;
import java.time.*;
import java.util.*;
import java.util.List;
import java.util.logging.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

final class DashboardView extends Screen {
    DashboardView(AppFrame host) {
        super(host);
    }

    private record DashboardData(List<Campsite> sites, List<Reservation> reservations) {}

    void render() {
        if (admin())
            content.add(
                    pageHeader(
                            "dashboard.title",
                            admin() ? "dashboard.admin.subtitle" : "dashboard.user.subtitle"),
                    BorderLayout.NORTH);
        else {
            JPanel banner = new PhotoPanel();
            banner.setPreferredSize(new Dimension(600, 156));
            JLabel heading = Theme.heading(t("dashboard.hero"), 29);
            heading.setForeground(Color.WHITE);
            banner.add(heading, BorderLayout.NORTH);
            JTextArea hint = Theme.paragraph(t("dashboard.heroHint"));
            hint.setForeground(Color.WHITE);
            banner.add(hint, BorderLayout.CENTER);
            content.add(banner, BorderLayout.NORTH);
        }
        work(
                () -> new DashboardData(app.campsites.search("", admin()), app.reservations.list()),
                data -> {
                    JPanel body = Theme.panel(new BorderLayout(20, 24));
                    JPanel metrics = Theme.panel(new GridLayout(1, 3, 16, 0));
                    List<Reservation> active =
                            data.reservations().stream()
                                    .filter(r -> r.status().equals("CONFIRMED"))
                                    .toList();
                    if (admin()) {
                        metrics.add(
                                metric(
                                        "activeSites",
                                        Long.toString(
                                                data.sites().stream()
                                                        .filter(Campsite::active)
                                                        .count())));
                        metrics.add(metric("confirmedBookings", Integer.toString(active.size())));
                        metrics.add(
                                metric(
                                        "bookingValue",
                                        m.money(
                                                active.stream()
                                                        .mapToLong(Reservation::totalCents)
                                                        .sum())));
                    } else {
                        metrics.add(metric("exploreSites", Integer.toString(data.sites().size())));
                        metrics.add(
                                metric(
                                        "upcoming",
                                        Long.toString(
                                                active.stream()
                                                        .filter(
                                                                r ->
                                                                        !r.arrival()
                                                                                .isBefore(
                                                                                        LocalDate
                                                                                                .now()))
                                                        .count())));
                        metrics.add(
                                metric(
                                        "reservedNights",
                                        Long.toString(
                                                active.stream()
                                                        .mapToLong(Reservation::nights)
                                                        .sum())));
                    }
                    body.add(metrics, BorderLayout.NORTH);
                    JPanel feature = Theme.card();
                    feature.setPreferredSize(new Dimension(600, 300));
                    feature.add(
                            Theme.heading(t(admin() ? "admin.next" : "user.next"), 24),
                            BorderLayout.NORTH);
                    JPanel overview = Theme.panel(new BorderLayout(0, 20));
                    overview.add(
                            Theme.paragraph(t(admin() ? "admin.next.text" : "user.next.text")),
                            BorderLayout.NORTH);
                    JTable upcoming = table("campsite", "arrival", "departure", "people");
                    List<Reservation> next =
                            active.stream()
                                    .filter(r -> !r.departure().isBefore(LocalDate.now()))
                                    .sorted(Comparator.comparing(Reservation::arrival))
                                    .limit(5)
                                    .toList();
                    DefaultTableModel model = (DefaultTableModel) upcoming.getModel();
                    for (Reservation r : next)
                        model.addRow(
                                new Object[] {
                                    r.campsiteName(), r.arrival(), r.departure(), r.people()
                                });
                    JPanel itinerary = Theme.panel(new BorderLayout(0, 8));
                    if (!next.isEmpty())
                        itinerary.add(new JLabel(t("upcoming")), BorderLayout.NORTH);
                    itinerary.add(
                            next.isEmpty()
                                    ? Theme.emptyState(
                                            t("empty.reservations"),
                                            t(
                                                    admin()
                                                            ? "dashboard.adminEmpty"
                                                            : "dashboard.emptyHelp"))
                                    : new JScrollPane(upcoming),
                            BorderLayout.CENTER);
                    overview.add(itinerary, BorderLayout.CENTER);
                    feature.add(overview, BorderLayout.CENTER);
                    feature.add(
                            row(
                                    action("campsites", true, () -> showPage("campsites")),
                                    action("reservations", false, () -> showPage("reservations"))),
                            BorderLayout.SOUTH);
                    body.add(feature, BorderLayout.CENTER);
                    body.add(
                            Theme.paragraph(t(admin() ? "report.note" : "booking.note")),
                            BorderLayout.SOUTH);
                    JPanel wrapper = new ScrollContent(new BorderLayout());
                    wrapper.add(body, BorderLayout.NORTH);
                    JScrollPane scroll = new JScrollPane(wrapper);
                    scroll.setBorder(null);
                    scroll.getViewport().setBackground(Theme.PAPER);
                    scroll.getVerticalScrollBar().setUnitIncrement(20);
                    content.add(scroll, BorderLayout.CENTER);
                    content.revalidate();
                    content.repaint();
                    SwingUtilities.invokeLater(
                            () -> scroll.getViewport().setViewPosition(new Point(0, 0)));
                });
    }

    private JPanel metric(String key, String value) {
        JPanel p = Theme.card();
        JLabel label = new JLabel(t(key));
        label.setForeground(Theme.MUTED);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 13f));
        p.add(label, BorderLayout.NORTH);
        JLabel number = Theme.heading(value, 28);
        number.setForeground(Theme.FOREST);
        p.add(number, BorderLayout.CENTER);
        return p;
    }
}
