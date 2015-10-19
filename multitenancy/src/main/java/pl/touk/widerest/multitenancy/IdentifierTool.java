package pl.touk.widerest.multitenancy;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
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
