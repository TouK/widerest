package pl.touk.widerest.multitenancy;

import java.util.UUID;

public class IdentifierTool {

    public String generateIdentifier() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }

    public void verifyIdentifier(String identifier) {
        if (MultiTenancyConfig.DEFAULT_TENANT_IDENTIFIER.equals(identifier))
            return;
        UUID.fromString(identifier);
    }
}
