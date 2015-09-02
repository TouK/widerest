package pl.touk.widerest.multitenancy;

public interface TenantAdminService {

    void createAdminUser(String email, String password);

}
