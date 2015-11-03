package pl.touk.widerest.security.oauth2;

public enum Scope {
    CUSTOMER("customer"),
    CUSTOMER_REGISTERED("customer:registered"),
    STAFF("staff");

    private final String name;

    Scope(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public boolean matches(String name) {
        return name.startsWith(this.name);
    }
}
