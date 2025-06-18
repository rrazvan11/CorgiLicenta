package ro.ong.corgi.model.Enums;

public enum StatusProiect{
    INSCRIERI_DESCHISE("Înscrieri deschise"),
    INSCRIERI_INCHISE("Înscrieri închise"),
    IN_CURS("În curs"),
    FINALIZAT("Finalizat");

    private final String displayValue;

    StatusProiect(String displayValue) {
        this.displayValue = displayValue;
    }

    public String getDisplayValue() {
        return displayValue;
    }

    @Override
    public String toString() {
        return displayValue;
    }
}