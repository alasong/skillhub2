package com.iflytek.skillhub.domain.security;

import java.util.Optional;

public interface HardDeprecationRepository {

    HardDeprecation save(HardDeprecation deprecation);

    Optional<HardDeprecation> findTopBySkillNameAndSkillVersionOrderByDeprecatedAtDesc(
            String skillName, String skillVersion);

    boolean existsBySkillNameAndSkillVersion(String skillName, String skillVersion);
}
