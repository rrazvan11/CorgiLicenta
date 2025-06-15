package ro.ong.corgi.model.Enums;

public enum StatusProiect{
    INSCRIERI_DESCHISE("Înscrieri Deschise"),
    INSCRIERI_INCHISE("Înscrieri Închise"),
    IN_CURS("În Curs"),
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