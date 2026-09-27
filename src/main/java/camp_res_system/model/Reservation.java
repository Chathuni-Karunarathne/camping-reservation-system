package camp_res_system.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record Reservation(
        long id,
        String campsiteId,
        String campsiteName,
        long userId,
        String username,
        LocalDate arrival,
        LocalDate departure,
        int people,
        long rateCents,
        String status) {
    public long nights() {
        return ChronoUnit.DAYS.between(arrival, departure);
    }

    public long totalCents() {
        return Math.multiplyExact(Math.multiplyExact(rateCents, people), nights());
    }
}
