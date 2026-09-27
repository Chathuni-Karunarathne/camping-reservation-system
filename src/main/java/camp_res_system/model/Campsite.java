package camp_res_system.model;

public record Campsite(
        String id,
        String name,
        String province,
        String description,
        long rateCents,
        boolean active) {
    @Override
    public String toString() {
        return id.isEmpty() ? name : name + " · " + id;
    }
}
