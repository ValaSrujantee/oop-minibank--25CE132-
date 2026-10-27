package model;

import java.io.Serial;
import java.io.Serializable;

public class Customer implements Cloneable, Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private static long customerCounter = 100;

    private final String name;
    private final String email;
    private final String mobile;
    private final String customerId;
    private Address address;

    public Customer(String name, String email, String mobile, Address address) {
        this.name = requireText(name, "name");
        this.email = requireText(email, "email");
        this.mobile = requireText(mobile, "mobile");
        this.customerId = generateCustomerId();
        this.address = address;
    }

    public Customer(String name, String email, String mobile) {
        this(name, email, mobile, null);
    }

    /** Kept for the Practical 3 address/clone demonstration. */
    public Customer(String name, Address address) {
        this(name, "unknown@example.invalid", "0000000000", address);
    }

    private static synchronized String generateCustomerId() {
        return "CUST" + ++customerCounter;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required.");
        return value.trim();
    }

    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getMobile() { return mobile; }
    public String getCustomerId() { return customerId; }
    public Address getAddress() { return address; }

    @Override public Customer clone() {
        try {
            Customer copy = (Customer) super.clone();
            if (address != null) copy.address = new Address(address.getLine(), address.getCity(), address.getPincode());
            return copy;
        } catch (CloneNotSupportedException ex) { throw new AssertionError(ex); }
    }

    public static class Address implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        private final String line, city, pincode;
        public Address(String line, String city, String pincode) { this.line=line; this.city=city; this.pincode=pincode; }
        public String getLine() { return line; }
        public String getCity() { return city; }
        public String getPincode() { return pincode; }
        @Override public String toString() { return line + ", " + city + " - " + pincode; }
    }
}
