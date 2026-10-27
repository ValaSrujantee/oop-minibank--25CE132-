package model;
public record BankInfo(String name, String branch) {
    @Override public String toString() { return name + " | Branch: " + branch; }
}
