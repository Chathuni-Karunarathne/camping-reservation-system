package camp_res_system.ui;

import camp_res_system.i18n.Messages;
import camp_res_system.model.Reservation;

import java.util.List;

public final class Documents {
    private Documents() {}

    public static String bill(Reservation r, Messages m) {
        return "TENTTRACK\n"
                + m.text("bill.title")
                + " #"
                + r.id()
                + "\n\n"
                + m.text("campsite")
                + ": "
                + r.campsiteName()
                + " ("
                + r.campsiteId()
                + ")\n"
                + m.text("username")
                + ": "
                + r.username()
                + "\n"
                + m.text("arrival")
                + ": "
                + m.date(r.arrival())
                + "\n"
                + m.text("departure")
                + ": "
                + m.date(r.departure())
                + "\n"
                + m.text("nights")
                + ": "
                + r.nights()
                + "\n"
                + m.text("people")
                + ": "
                + r.people()
                + "\n"
                + m.text("rate")
                + ": "
                + m.money(r.rateCents())
                + "\n"
                + m.text("status")
                + ": "
                + m.text("status." + r.status())
                + "\n\n"
                + m.text("total")
                + ": "
                + m.money(r.totalCents())
                + "\n\n"
                + m.text("bill.note");
    }

    public static String report(List<Reservation> rows, Messages m) {
        StringBuilder out = new StringBuilder("TENTTRACK — " + m.text("reports") + "\n\n");
        for (Reservation r : rows)
            out.append('#')
                    .append(r.id())
                    .append(" | ")
                    .append(r.campsiteName())
                    .append(" | ")
                    .append(r.username())
                    .append(" | ")
                    .append(m.date(r.arrival()))
                    .append(" → ")
                    .append(m.date(r.departure()))
                    .append(" | ")
                    .append(m.text("status." + r.status()))
                    .append(" | ")
                    .append(m.money(r.totalCents()))
                    .append('\n');
        out.append('\n')
                .append(m.text("bookingValue"))
                .append(": ")
                .append(
                        m.money(
                                rows.stream()
                                        .filter(r -> r.status().equals("CONFIRMED"))
                                        .mapToLong(Reservation::totalCents)
                                        .sum()));
        return out.append("\n\n").append(m.text("report.note")).toString();
    }
}
