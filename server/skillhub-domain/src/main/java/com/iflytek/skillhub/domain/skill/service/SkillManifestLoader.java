package com.iflytek.skillhub.domain.skill.service;

import com.iflytek.skillhub.domain.skill.metadata.SkillManifest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Loads a SkillManifest for a given skill name and version.
 *
 * <p>This component abstracts access to the stored skill.yaml manifest.
 * The actual storage lookup (from object storage, database, or file system)
 * is not yet implemented. When the manifest storage layer is connected,
 * implement the loading logic in this class.
 */
@Component
public class SkillManifestLoader {

    private static final Logger log = LoggerFactory.getLogger(SkillManifestLoader.class);

    /**
     * Loads the SkillManifest for the specified skill name and version.
     *
     * @param skillName    the skill name
     * @param skillVersion the skill version
     * @return the parsed SkillManifest
     * @throws UnsupportedOperationException until the manifest storage is connected
     */
    public SkillManifest load(String skillName, String skillVersion) {
        // TODO: implement when manifest storage is available
        // 1. Find the skill version record
        // 2. Load skill.yaml from object storage for that version
        // 3. Parse with SkillManifestParser
        // 4. Return the parsed SkillManifest
        throw new UnsupportedOperationException(
                "Manifest loading not yet implemented for " + skillName + ":" + skillVersion);
    }
}
