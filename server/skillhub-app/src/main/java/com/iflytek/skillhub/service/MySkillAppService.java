package com.iflytek.skillhub.service;

import com.iflytek.skillhub.domain.skill.Skill;
import com.iflytek.skillhub.domain.skill.SkillRepository;
import com.iflytek.skillhub.domain.skill.SkillVersionRepository;
import com.iflytek.skillhub.domain.skill.service.SkillLifecycleProjectionService;
import com.iflytek.skillhub.dto.PageResponse;
import com.iflytek.skillhub.dto.SkillSummaryResponse;
import com.iflytek.skillhub.repository.MySkillQueryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
/**
 * Application service that assembles the current user's owned and starred
 * skill lists with lifecycle context.
 */
@Service
public class MySkillAppService {
    public enum MySkillFilter {
        ALL,
        PENDING_REVIEW,
        PUBLISHED,
        REJECTED,
        ARCHIVED,
        HIDDEN
    }

    private final SkillRepository skillRepository;
    private final SkillVersionRepository skillVersionRepository;
    private final MySkillQueryRepository mySkillQueryRepository;
    private final SkillLifecycleProjectionService skillLifecycleProjectionService;

    public MySkillAppService(
            SkillRepository skillRepository,
            SkillVersionRepository skillVersionRepository,
            MySkillQueryRepository mySkillQueryRepository,
            SkillLifecycleProjectionService skillLifecycleProjectionService) {
        this.skillRepository = skillRepository;
        this.skillVersionRepository = skillVersionRepository;
        this.mySkillQueryRepository = mySkillQueryRepository;
        this.skillLifecycleProjectionService = skillLifecycleProjectionService;
    }

    public PageResponse<SkillSummaryResponse> listMySkills(String userId, int page, int size) {
        return listMySkills(userId, page, size, null, java.util.Set.of());
    }

    public PageResponse<SkillSummaryResponse> listMySkills(String userId,
                                                           int page,
                                                           int size,
                                                           String filter,
                                                           java.util.Set<String> platformRoles) {
        MySkillFilter normalizedFilter = parseFilter(filter);
        Page<Skill> skillPage = normalizedFilter == MySkillFilter.ALL
                ? skillRepository.findByOwnerId(userId, PageRequest.of(page, size))
                : filterSkillsByLifecycle(userId, page, size, normalizedFilter, platformRoles);
        List<SkillSummaryResponse> items = mySkillQueryRepository.getSkillSummaries(skillPage.getContent(), userId);

        return new PageResponse<>(items, skillPage.getTotalElements(), skillPage.getNumber(), skillPage.getSize());
    }

    private Page<Skill> filterSkillsByLifecycle(String userId,
                                                int page,
                                                int size,
                                                MySkillFilter filter,
                                                java.util.Set<String> platformRoles) {
        List<Skill> skills = skillRepository.findByOwnerId(userId);
        List<Skill> filtered = skills.stream()
                .filter(skill -> matchesFilter(skill, filter, platformRoles))
                .toList();
        int fromIndex = Math.min(page * size, filtered.size());
        int toIndex = Math.min(fromIndex + size, filtered.size());
        return new PageImpl<>(
                filtered.subList(fromIndex, toIndex),
                PageRequest.of(page, size),
                filtered.size()
        );
    }

    private boolean matchesFilter(Skill skill, MySkillFilter filter, java.util.Set<String> platformRoles) {
        if (filter == MySkillFilter.HIDDEN) {
            return platformRoles.contains("SUPER_ADMIN") && skill.isHidden();
        }

        if (skill.isHidden()) {
            return false;
        }
        if (filter == MySkillFilter.ARCHIVED) {
            return skill.getStatus() == com.iflytek.skillhub.domain.skill.SkillStatus.ARCHIVED;
        }
        if (skill.getStatus() == com.iflytek.skillhub.domain.skill.SkillStatus.ARCHIVED) {
            return false;
        }

        SkillLifecycleProjectionService.Projection projection = skillLifecycleProjectionService.projectForOwnerSummary(skill);
        SkillLifecycleProjectionService.VersionProjection ownerPreviewVersion = projection.ownerPreviewVersion();
        SkillLifecycleProjectionService.VersionProjection publishedVersion = projection.publishedVersion();

        return switch (filter) {
            case PENDING_REVIEW -> ownerPreviewVersion != null && "PENDING_REVIEW".equals(ownerPreviewVersion.status());
            case PUBLISHED -> publishedVersion != null;
            case REJECTED -> ownerPreviewVersion != null && "REJECTED".equals(ownerPreviewVersion.status());
            case ALL, ARCHIVED, HIDDEN -> true;
        };
    }

    private MySkillFilter parseFilter(String filter) {
        if (filter == null || filter.isBlank()) {
            return MySkillFilter.ALL;
        }
        try {
            return MySkillFilter.valueOf(filter.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return MySkillFilter.ALL;
        }
    }
}
