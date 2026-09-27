package camp_res_system.ui;

import camp_res_system.model.*;
import camp_res_system.service.*;

import com.toedter.calendar.JDateChooser;

import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.List;
import java.util.logging.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

final class CampsitesView extends Screen {
    CampsitesView(AppFrame host) {
        super(host);
    }

    void render() {
        if (!admin()) {
            new CampsiteBrowser(host, this::bookingDialog).render();
            return;
        }
        JPanel top = Theme.panel(new BorderLayout(0, 16));
        top.add(
                pageHeader(
                        "campsites", admin() ? "campsites.admin.subtitle" : "campsites.subtitle"),
                BorderLayout.NORTH);
        JTextField search = new JTextField(23);
        search.putClientProperty("JTextField.placeholderText", t("search.placeholder"));
        JTable table = table("id", "campsite", "province", "rate", "status");
        List<Campsite> sites = new ArrayList<>();
        JLabel count = new JLabel();
        Runnable reload =
                () -> {
                    String query = search.getText();
                    work(
                            () -> app.campsites.search(query, admin()),
                            rows -> {
                                sites.clear();
                                sites.addAll(rows);
                                DefaultTableModel model = (DefaultTableModel) table.getModel();
                                model.setRowCount(0);
                                for (Campsite s : rows)
                                    model.addRow(
                                            new Object[] {
                                                s.id(),
                                                s.name(),
                                                s.province(),
                                                s.rateCents(),
                                                t(s.active() ? "active" : "archived")
                                            });
                                count.setText(
                                        rows.isEmpty()
                                                ? t("empty.campsites")
                                                : rows.size() + " " + t("results"));
                            });
                };
        JPanel tools = row(new JLabel(t("search")), search, action("search", true, reload));
        search.addActionListener(e -> reload.run());
        if (admin()) {
            tools.add(action("addCampsite", false, () -> editCampsite(null, reload)));
            tools.add(
                    action(
                            "loadDemo",
                            false,
                            () ->
                                    work(
                                            () -> {
                                                app.campsites.loadDemo();
                                                return true;
                                            },
                                            v -> reload.run())));
        }
        top.add(tools, BorderLayout.CENTER);
        content.add(top, BorderLayout.NORTH);
        content.add(new JScrollPane(table), BorderLayout.CENTER);
        JButton details =
                action(
                        admin() ? "editCampsite" : "viewBook",
                        true,
                        () -> {
                            int index = table.getSelectedRow();
                            if (index < 0) throw new AppException("error.select");
                            Campsite s = sites.get(table.convertRowIndexToModel(index));
                            if (admin()) editCampsite(s, reload);
                            else bookingDialog(s);
                        });
        details.setEnabled(false);
        JButton archive =
                action(
                        "archive",
                        false,
                        () -> {
                            int index = table.getSelectedRow();
                            if (index < 0) throw new AppException("error.select");
                            Campsite s = sites.get(table.convertRowIndexToModel(index));
                            if (confirm("archive.confirm"))
                                work(
                                        () -> {
                                            app.campsites.archive(s.id());
                                            return true;
                                        },
                                        v -> reload.run());
                        });
        archive.setEnabled(false);
        table.getSelectionModel()
                .addListSelectionListener(
                        e -> {
                            details.setEnabled(table.getSelectedRow() >= 0);
                            archive.setEnabled(table.getSelectedRow() >= 0);
                        });
        JPanel bottom = row(count, details);
        if (admin()) bottom.add(archive);
        content.add(bottom, BorderLayout.SOUTH);
        reload.run();
    }

    private void editCampsite(Campsite site, Runnable reload) {
        app.session.requireAdmin();
        JDialog d = dialog(site == null ? "addCampsite" : "editCampsite", 570, 720);
        JPanel root = Theme.card(), form = stack();
        JTextField id = new JTextField(site == null ? "" : site.id()),
                name = new JTextField(site == null ? "" : site.name()),
                province = new JTextField(site == null ? "" : site.province()),
                rate =
                        new JTextField(
                                site == null
                                        ? ""
                                        : BigDecimal.valueOf(site.rateCents(), 2).toPlainString());
        id.setEditable(site == null);
        JTextArea description = new JTextArea(site == null ? "" : site.description(), 4, 25);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        JCheckBox active = new JCheckBox(t("active"), site == null || site.active());
        field(form, 0, "id", id);
        field(form, 1, "campsite", name);
        field(form, 2, "province", province);
        field(form, 3, "rate", rate);
        field(form, 4, "description", new JScrollPane(description));
        field(form, 5, "status", active);
        root.add(new JScrollPane(form), BorderLayout.CENTER);
        JLabel error = new JLabel(" ");
        JButton save =
                action(
                        "save",
                        true,
                        () -> {
                            long cents;
                            try {
                                cents =
                                        new BigDecimal(rate.getText().trim())
                                                .movePointRight(2)
                                                .longValueExact();
                            } catch (ArithmeticException | NumberFormatException e) {
                                error.setText(t("error.rate"));
                                return;
                            }
                            Campsite changed =
                                    new Campsite(
                                            id.getText().trim(),
                                            name.getText(),
                                            province.getText(),
                                            description.getText(),
                                            cents,
                                            active.isSelected());
                            work(
                                    () -> {
                                        app.campsites.save(changed, site == null);
                                        return true;
                                    },
                                    v -> {
                                        d.dispose();
                                        reload.run();
                                    });
                        });
        root.add(row(save, action("close", false, d::dispose), error), BorderLayout.SOUTH);
        prepareDialog(d, root);
        d.setVisible(true);
    }

    private void bookingDialog(Campsite site) {
        JDialog d = dialog("viewBook", 860, 700);
        JPanel root = Theme.card();
        JPanel title = Theme.panel(new BorderLayout(0, 8));
        title.add(Theme.heading(site.name(), 27), BorderLayout.NORTH);
        title.add(Theme.paragraph(t("booking.steps")), BorderLayout.SOUTH);
        root.add(title, BorderLayout.NORTH);

        JPanel body = Theme.panel(new BorderLayout(22, 16));
        JPanel left = Theme.panel(new BorderLayout(0, 18));
        JPanel details = Theme.panel(new BorderLayout(0, 10));
        details.add(Theme.badge(site.province()), BorderLayout.NORTH);
        JTextArea description = Theme.paragraph(site.description());
        description.setRows(3);
        JScrollPane descriptionScroll = new JScrollPane(description);
        descriptionScroll.setBorder(null);
        details.add(descriptionScroll, BorderLayout.CENTER);
        left.add(details, BorderLayout.NORTH);

        JDateChooser arrival = dateChooser(LocalDate.now().plusDays(1));
        JDateChooser departure = dateChooser(LocalDate.now().plusDays(2));
        arrival.setMinSelectableDate(
                Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        JSpinner people = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        JPanel stay = Theme.panel(new BorderLayout(0, 14));
        JPanel dates = Theme.panel(new GridLayout(1, 2, 14, 0));
        JPanel start = stack(), end = stack();
        field(start, 0, "arrival", arrival);
        field(end, 0, "departure", departure);
        dates.add(start);
        dates.add(end);
        stay.add(dates, BorderLayout.NORTH);
        JPanel guests = stack();
        field(guests, 0, "people", people);
        stay.add(guests, BorderLayout.CENTER);
        stay.add(Theme.paragraph(t("booking.dateHelp")), BorderLayout.SOUTH);
        left.add(stay, BorderLayout.CENTER);
        JCheckBox accept = new JCheckBox(t("terms.accept"));
        JPanel consent = Theme.panel(new BorderLayout(0, 8));
        consent.add(
                action("terms.read", false, () -> documentDialog(t("terms"), t("terms.text"))),
                BorderLayout.NORTH);
        consent.add(accept, BorderLayout.SOUTH);
        left.add(consent, BorderLayout.SOUTH);
        body.add(left, BorderLayout.CENTER);

        JPanel summary = Theme.card();
        summary.setBackground(Theme.SAGE);
        summary.setPreferredSize(new Dimension(260, 0));
        summary.add(Theme.heading(t("booking.summary"), 20), BorderLayout.NORTH);
        JPanel amounts = Theme.panel(new BorderLayout(0, 16));
        JLabel estimate = Theme.heading("—", 28);
        amounts.add(estimate, BorderLayout.NORTH);
        JTextArea breakdown =
                Theme.paragraph(
                        m.money(site.rateCents())
                                + "\n"
                                + t("perPersonNight")
                                + "\n\n"
                                + t("booking.checkFirst"));
        breakdown.setRows(9);
        amounts.add(breakdown, BorderLayout.CENTER);
        summary.add(amounts, BorderLayout.CENTER);
        JTextArea availability = Theme.paragraph(t("booking.checkFirst"));
        availability.setRows(3);
        summary.add(availability, BorderLayout.SOUTH);
        body.add(summary, BorderLayout.EAST);
        root.add(body, BorderLayout.CENTER);

        final ReservationService.Quote[] quote = {null};
        JButton book =
                action(
                        "confirmBooking",
                        true,
                        () -> {
                            if (quote[0] == null) throw new AppException("error.quote");
                            LocalDate a = date(arrival), b = date(departure);
                            int guestsCount = (Integer) people.getValue();
                            long rate = quote[0].rateCents();
                            boolean agreed = accept.isSelected();
                            work(
                                    () ->
                                            app.reservations.book(
                                                    site.id(), a, b, guestsCount, rate, agreed),
                                    r -> {
                                        d.dispose();
                                        showPage("reservations");
                                        documentDialog(t("bill.title"), Documents.bill(r, m));
                                    });
                        });
        book.setEnabled(false);
        Runnable invalidate =
                () -> {
                    quote[0] = null;
                    book.setEnabled(false);
                    estimate.setText("—");
                    availability.setForeground(Theme.MUTED);
                    availability.setText(t("quote.refresh"));
                    breakdown.setText(
                            m.money(site.rateCents())
                                    + "\n"
                                    + t("perPersonNight")
                                    + "\n\n"
                                    + t("booking.checkFirst"));
                };
        arrival.addPropertyChangeListener(
                "date",
                e -> {
                    LocalDate selected = date(arrival);
                    if (selected != null
                            && date(departure) != null
                            && !date(departure).isAfter(selected)) {
                        departure.setDate(
                                Date.from(
                                        selected.plusDays(1)
                                                .atStartOfDay(ZoneId.systemDefault())
                                                .toInstant()));
                    }
                    invalidate.run();
                });
        departure.addPropertyChangeListener("date", e -> invalidate.run());
        people.addChangeListener(e -> invalidate.run());
        accept.addActionListener(
                e -> {
                    book.setEnabled(
                            quote[0] != null && quote[0].available() && accept.isSelected());
                    if (quote[0] != null && quote[0].available())
                        availability.setText(
                                t(accept.isSelected() ? "booking.ready" : "booking.acceptNext"));
                });
        JButton check =
                action(
                        "checkAvailability",
                        false,
                        () -> {
                            try {
                                people.commitEdit();
                            } catch (java.text.ParseException e) {
                                throw new AppException("error.booking");
                            }
                            LocalDate a = date(arrival), b = date(departure);
                            int guestsCount = (Integer) people.getValue();
                            work(
                                    () -> app.reservations.quote(site.id(), a, b, guestsCount),
                                    q -> {
                                        quote[0] = q;
                                        estimate.setText(m.money(q.totalCents()));
                                        breakdown.setText(
                                                m.money(q.rateCents())
                                                        + " × "
                                                        + guestsCount
                                                        + " "
                                                        + t("people")
                                                        + " × "
                                                        + q.nights()
                                                        + " "
                                                        + t("nights")
                                                        + "\n\n"
                                                        + t("booking.priceNote"));
                                        availability.setForeground(
                                                q.available()
                                                        ? Theme.FOREST
                                                        : new Color(160, 45, 38));
                                        availability.setText(
                                                t(
                                                        q.available()
                                                                ? (accept.isSelected()
                                                                        ? "booking.ready"
                                                                        : "booking.acceptNext")
                                                                : "unavailable"));
                                        book.setEnabled(q.available() && accept.isSelected());
                                    });
                        });
        root.add(row(action("close", false, d::dispose), check, book), BorderLayout.SOUTH);
        prepareDialog(d, root);
        d.setVisible(true);
    }
}
