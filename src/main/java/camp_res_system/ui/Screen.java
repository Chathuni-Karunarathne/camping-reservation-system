package camp_res_system.ui;

import camp_res_system.config.AppContext;
import camp_res_system.i18n.Messages;

import com.toedter.calendar.JDateChooser;

import java.awt.Component;
import java.time.LocalDate;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import javax.swing.*;

/** Shared view operations; the host owns navigation, asynchronous work and dialogs. */
abstract class Screen {
    protected final AppFrame host;
    protected final AppContext app;
    protected final Messages m;
    protected final JPanel content;

    Screen(AppFrame host) {
        this.host = host;
        app = host.app;
        m = host.m;
        content = host.content;
    }

    String t(String key) {
        return host.t(key);
    }

    boolean admin() {
        return host.admin();
    }

    JButton action(String key, boolean primary, Runnable run) {
        return host.action(key, primary, run);
    }

    <T> void work(Callable<T> task, Consumer<T> done) {
        host.work(task, done);
    }

    void showPage(String page) {
        host.showPage(page);
    }

    void loadPage() {
        host.loadPage();
    }

    void success(String key) {
        host.success(key);
    }

    JPanel pageHeader(String title, String subtitle) {
        return host.pageHeader(title, subtitle);
    }

    JPanel stack() {
        return host.stack();
    }

    void field(JPanel form, int row, String label, JComponent component) {
        host.field(form, row, label, component);
    }

    JPanel row(Component... components) {
        return host.row(components);
    }

    JTable table(String... keys) {
        return host.table(keys);
    }

    JDialog dialog(String key, int width, int height) {
        return host.dialog(key, width, height);
    }

    void prepareDialog(JDialog dialog, JPanel root) {
        host.prepareDialog(dialog, root);
    }

    JDateChooser dateChooser(LocalDate date) {
        return host.dateChooser(date);
    }

    LocalDate date(JDateChooser chooser) {
        return host.date(chooser);
    }

    boolean confirm(String key) {
        return host.confirm(key);
    }

    void documentDialog(String title, String text) {
        host.documentDialog(title, text);
    }
}
