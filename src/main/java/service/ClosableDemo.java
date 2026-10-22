package service;
public class ClosableDemo implements AutoCloseable {
    @Override public void close() { System.out.println("Demo resource closed."); }
    public void use() { System.out.println("Using demo resource."); }
}
