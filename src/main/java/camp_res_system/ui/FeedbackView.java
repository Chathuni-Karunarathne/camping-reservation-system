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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

final class FeedbackView extends Screen {
    FeedbackView(AppFrame host) {
        super(host);
    }

    void render() {
        content.add(
                pageHeader("feedback", admin() ? "feedback.admin.subtitle" : "feedback.subtitle"),
                BorderLayout.NORTH);
        if (admin()) {
            JTable table = table("id", "username", "submitted", "message");
            JTextArea detail = Theme.paragraph(t("feedback.select"));
            detail.setRows(5);
            List<Feedback> rows = new ArrayList<>();
            table.getSelectionModel()
                    .addListSelectionListener(
                            e -> {
                                if (table.getSelectedRow() >= 0)
                                    detail.setText(
                                            rows.get(
                                                            table.convertRowIndexToModel(
                                                                    table.getSelectedRow()))
                                                    .message());
                            });
            JSplitPane split =
                    new JSplitPane(
                            JSplitPane.VERTICAL_SPLIT,
                            new JScrollPane(table),
                            new JScrollPane(detail));
            split.setResizeWeight(0.7);
            content.add(split, BorderLayout.CENTER);
            work(
                    app.feedback::list,
                    list -> {
                        rows.addAll(list);
                        DefaultTableModel model = (DefaultTableModel) table.getModel();
                        for (Feedback f : list)
                            model.addRow(
                                    new Object[] {
                                        f.id(), f.username(), f.createdAt(), f.message()
                                    });
                        if (list.isEmpty()) detail.setText(t("empty.feedback"));
                    });
        } else {
            JPanel card = Theme.card();
            JPanel introduction = Theme.panel(new BorderLayout(0, 10));
            introduction.add(Theme.heading(t("feedback.prompt"), 23), BorderLayout.NORTH);
            introduction.add(Theme.paragraph(t("feedback.help")), BorderLayout.CENTER);
            card.add(introduction, BorderLayout.NORTH);
            JTextArea message = new JTextArea(10, 50);
            message.setMargin(new Insets(14, 14, 14, 14));
            message.getAccessibleContext().setAccessibleName(t("message"));
            message.setLineWrap(true);
            message.setWrapStyleWord(true);
            card.add(new JScrollPane(message), BorderLayout.CENTER);
            JLabel counter = new JLabel("0 / 2000");
            message.getDocument()
                    .addDocumentListener(
                            new DocumentListener() {
                                private void update() {
                                    counter.setText(message.getText().length() + " / 2000");
                                }

                                public void insertUpdate(DocumentEvent e) {
                                    update();
                                }

                                public void removeUpdate(DocumentEvent e) {
                                    update();
                                }

                                public void changedUpdate(DocumentEvent e) {
                                    update();
                                }
                            });
            card.add(
                    row(
                            action(
                                    "submitFeedback",
                                    true,
                                    () -> {
                                        String text = message.getText();
                                        work(
                                                () -> {
                                                    app.feedback.submit(text);
                                                    return true;
                                                },
                                                v -> {
                                                    message.setText("");
                                                    success("feedback.thanks");
                                                });
                                    }),
                            counter),
                    BorderLayout.SOUTH);
            content.add(card, BorderLayout.CENTER);
        }
    }
}
