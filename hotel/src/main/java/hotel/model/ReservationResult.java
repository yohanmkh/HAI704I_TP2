package hotel.model;

public class ReservationResult {
    private final Reservation reservation;
    private final boolean replayed;

    public ReservationResult(Reservation reservation, boolean replayed) {
        this.reservation = reservation;
        this.replayed = replayed;
    }

    public Reservation getReservation() { return reservation; }
    public boolean isReplayed() { return replayed; }
}
