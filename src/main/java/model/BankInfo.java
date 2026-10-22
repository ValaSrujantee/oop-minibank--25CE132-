package model;
public record BankInfo(String name, String hours, String supportEmail) {
    @Override public String toString() { return name + " | Hours: " + hours + " | Support: " + supportEmail; }
}
