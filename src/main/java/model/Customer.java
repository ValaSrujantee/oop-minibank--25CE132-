package model;
public class Customer implements Cloneable {
    private final String name;
    private Address address;
    public Customer(String name, Address address) { this.name = name; this.address = address; }
    public String getName() { return name; }
    public Address getAddress() { return address; }
    @Override public Customer clone() {
        try {
            Customer copy = (Customer) super.clone();
            if (address != null) copy.address = new Address(address.getLine(), address.getCity(), address.getPincode());
            return copy;
        } catch (CloneNotSupportedException ex) { throw new AssertionError(ex); }
    }
    public static class Address {
        private final String line, city, pincode;
        public Address(String line, String city, String pincode) { this.line=line; this.city=city; this.pincode=pincode; }
        public String getLine() { return line; }
        public String getCity() { return city; }
        public String getPincode() { return pincode; }
        @Override public String toString() { return line + ", " + city + " - " + pincode; }
    }
}
