package com.iflytek.skillhub.domain.skill.compat;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class PlatformAdapterRegistry {
    private final Map<String, AgentPlatformAdapter> adapters;

    public PlatformAdapterRegistry(List<AgentPlatformAdapter> adapterList) {
        this.adapters = new LinkedHashMap<>();
        for (AgentPlatformAdapter a : adapterList) {
            adapters.put(a.platformId(), a);
        }
    }

    public Optional<AgentPlatformAdapter> getAdapter(String platformId) {
        return Optional.ofNullable(adapters.get(platformId));
    }

    public Set<String> supportedPlatforms() {
        return Collections.unmodifiableSet(adapters.keySet());
    }

    public String generateInstall(String platformId, String skillName, String skillVersion) {
        AgentPlatformAdapter adapter = adapters.get(platformId);
        if (adapter == null) {
            throw new IllegalArgumentException("Unsupported platform: " + platformId);
        }
        return adapter.generateInstall(skillName, skillVersion);
    }
}
