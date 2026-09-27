package camp_res_system.i18n;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;

public final class Messages {
    private Locale locale = Locale.ENGLISH;
    private ResourceBundle bundle = ResourceBundle.getBundle("i18n.messages", locale);

    public void setLocale(Locale locale) {
        this.locale = locale;
        bundle = ResourceBundle.getBundle("i18n.messages", locale);
    }

    public Locale locale() {
        return locale;
    }

    public String text(String key) {
        return bundle.getString(key);
    }

    public String money(long cents) {
        var format = NumberFormat.getCurrencyInstance(locale);
        format.setCurrency(Currency.getInstance("LKR"));
        return format.format(java.math.BigDecimal.valueOf(cents, 2));
    }

    public String date(LocalDate date) {
        return date.format(
                DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale));
    }
}
