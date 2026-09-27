package camp_res_system.ui;

import camp_res_system.model.*;
import camp_res_system.service.*;

import com.toedter.calendar.JDateChooser;

import java.awt.*;
import java.awt.event.*;
import java.time.*;
import java.util.*;
import java.util.List;
import java.util.logging.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

final class ReservationsView extends Screen {
    ReservationsView(AppFrame host) {
        super(host);
    }

    void render() {
        content.add(pageHeader("reservations", "reservations.subtitle"), BorderLayout.NORTH);
        JTable table = reservationTable();
        List<Reservation> rows = new ArrayList<>();
        JLabel count = new JLabel();
        content.add(new JScrollPane(table), BorderLayout.CENTER);
        JButton bill =
                action(
                        "bill.title",
                        true,
                        () -> {
                            Reservation r = selected(table, rows);
                            work(
                                    () -> app.reservations.bill(r.id()),
                                    value ->
                                            documentDialog(
                                                    t("bill.title"), Documents.bill(value, m)));
                        });
        bill.setEnabled(false);
        JButton cancel =
                action(
                        "cancelReservation",
                        false,
                        () -> {
                            Reservation r = selected(table, rows);
                            if (confirm("cancel.confirm"))
                                work(
                                        () -> {
                                            app.reservations.cancel(r.id());
                                            return true;
                                        },
                                        v -> loadPage());
                        });
        cancel.setEnabled(false);
        table.getSelectionModel()
                .addListSelectionListener(
                        e -> {
                            bill.setEnabled(table.getSelectedRow() >= 0);
                            cancel.setEnabled(table.getSelectedRow() >= 0);
                        });
        content.add(row(count, bill, cancel), BorderLayout.SOUTH);
        work(
                app.reservations::list,
                list -> {
                    rows.addAll(list);
                    fillReservations(table, list);
                    count.setText(
                            list.isEmpty()
                                    ? t("empty.reservations")
                                    : list.size() + " " + t("results"));
                });
    }

    private JTable reservationTable() {
        return table(
                "id",
                "campsite",
                "username",
                "arrival",
                "departure",
                "people",
                "nights",
                "total",
                "status");
    }

    private Reservation selected(JTable table, List<Reservation> rows) {
        if (table.getSelectedRow() < 0) throw new AppException("error.select");
        return rows.get(table.convertRowIndexToModel(table.getSelectedRow()));
    }

    private void fillReservations(JTable table, List<Reservation> rows) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        for (Reservation r : rows)
            model.addRow(
                    new Object[] {
                        r.id(),
                        r.campsiteName(),
                        r.username(),
                        r.arrival(),
                        r.departure(),
                        r.people(),
                        r.nights(),
                        r.totalCents(),
                        t("status." + r.status())
                    });
    }

    void reports() {
        app.session.requireAdmin();
        JPanel top = Theme.panel(new BorderLayout(0, 14));
        top.add(pageHeader("reports", "reports.subtitle"), BorderLayout.NORTH);
        JDateChooser from = dateChooser(LocalDate.now().withDayOfMonth(1)),
                to = dateChooser(LocalDate.now().plusMonths(1));
        JComboBox<Campsite> site = new JComboBox<>();
        site.setPreferredSize(new Dimension(190, 34));
        site.addItem(new Campsite("", t("allCampsites"), "", "", 0, true));
        JTable table = reservationTable();
        List<Reservation> rows = new ArrayList<>();
        JLabel total = new JLabel(t("report.note"));
        JButton generate =
                action(
                        "generate",
                        true,
                        () -> {
                            LocalDate a = date(from), b = date(to);
                            Campsite s = (Campsite) site.getSelectedItem();
                            work(
                                    () ->
                                            app.reservations.report(
                                                    a,
                                                    b,
                                                    s == null || s.id().isEmpty() ? null : s.id()),
                                    list -> {
                                        rows.clear();
                                        rows.addAll(list);
                                        fillReservations(table, list);
                                        total.setText(
                                                (list.isEmpty()
                                                                ? t("empty.reservations")
                                                                : list.size() + " " + t("results"))
                                                        + " · "
                                                        + t("bookingValue")
                                                        + ": "
                                                        + m.money(
                                                                list.stream()
                                                                        .filter(
                                                                                r ->
                                                                                        r.status()
                                                                                                .equals(
                                                                                                        "CONFIRMED"))
                                                                        .mapToLong(
                                                                                Reservation
                                                                                        ::totalCents)
                                                                        .sum()));
                                    });
                        });
        top.add(
                row(new JLabel(t("from")), from, new JLabel(t("to")), to, site, generate),
                BorderLayout.CENTER);
        content.add(top, BorderLayout.NORTH);
        content.add(new JScrollPane(table), BorderLayout.CENTER);
        content.add(
                row(
                        total,
                        action(
                                "exportPrint",
                                false,
                                () -> documentDialog(t("reports"), Documents.report(rows, m)))),
                BorderLayout.SOUTH);
        work(() -> app.campsites.search("", true), sites -> sites.forEach(site::addItem));
    }
}
