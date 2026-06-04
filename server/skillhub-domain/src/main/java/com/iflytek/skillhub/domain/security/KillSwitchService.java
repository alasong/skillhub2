package com.iflytek.skillhub.domain.security;

import com.iflytek.skillhub.domain.skill.SkillVersionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KillSwitchService {

    private static final Logger log = LoggerFactory.getLogger(KillSwitchService.class);

    private final HardDeprecationRepository deprecationRepository;
    private final SkillVersionRepository skillVersionRepository;
    private final CacheManager cacheManager;

    public KillSwitchService(HardDeprecationRepository deprecationRepository,
                             SkillVersionRepository skillVersionRepository,
                             @Nullable CacheManager cacheManager) {
        this.deprecationRepository = deprecationRepository;
        this.skillVersionRepository = skillVersionRepository;
        this.cacheManager = cacheManager;
    }

    @Transactional
    public HardDeprecation hardDeprecate(String skillName, String skillVersion,
                                          String reason, String adminUserId) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason is required for hard deprecation");
        }

        HardDeprecation dep = new HardDeprecation(skillName, skillVersion, reason, adminUserId);
        deprecationRepository.save(dep);

        // TODO: flag SkillVersion as deprecated via skillRepository.findBySlug() then
        // skillVersionRepository.findBySkillIdAndVersion() — needs slug→skillId resolution

        if (cacheManager != null) {
            var caches = new String[]{"skill-versions", "skill-install", "skill-search"};
            for (String cacheName : caches) {
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.evict(skillName + ":" + skillVersion);
                }
            }
        }

        log.warn("KILL-SWITCH: {}:{} deprecated by {} — {}", skillName, skillVersion, adminUserId, reason);

        return dep;
    }

    public boolean isHardDeprecated(String skillName, String skillVersion) {
        return deprecationRepository.existsBySkillNameAndSkillVersion(skillName, skillVersion);
    }
}
