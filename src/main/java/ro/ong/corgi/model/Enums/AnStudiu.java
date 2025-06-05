package ro.ong.corgi.model.Enums;

public enum AnStudiu {
    I_Licenta("Licență anul I"),
    II_Licenta("Licență anul II"),
    III_Licenta("Licență anul III"),

    IV_Licenta("Licență anul IV"),

    I_Master("Master anul I"),

    II_Master("Master anul II");

    private final String numeSpecializare;

    AnStudiu(String numeSpecializare) {
        this.numeSpecializare = numeSpecializare;
    }

    public String getNumeSpecializare() {
        return numeSpecializare;
    }
    @Override
    public String toString() {
        return numeSpecializare;
    }
}
