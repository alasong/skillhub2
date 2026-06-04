package com.iflytek.skillhub.infra.jpa;

import com.iflytek.skillhub.domain.security.HardDeprecation;
import com.iflytek.skillhub.domain.security.HardDeprecationRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
interface JpaHardDeprecationRepository extends JpaRepository<HardDeprecation, UUID>, HardDeprecationRepository {

    @Override
    default Optional<HardDeprecation> findTopBySkillNameAndSkillVersionOrderByDeprecatedAtDesc(
            String skillName, String skillVersion) {
        return findBySkillNameAndSkillVersionOrderByDeprecatedAtDesc(skillName, skillVersion)
                .stream()
                .findFirst();
    }

    @Override
    default boolean existsBySkillNameAndSkillVersion(String skillName, String skillVersion) {
        return findBySkillNameAndSkillVersion(skillName, skillVersion).isPresent();
    }

    Optional<HardDeprecation> findBySkillNameAndSkillVersion(String skillName, String skillVersion);

    java.util.List<HardDeprecation> findBySkillNameAndSkillVersionOrderByDeprecatedAtDesc(
            String skillName, String skillVersion);
}
