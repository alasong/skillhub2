package com.iflytek.skillhub.domain.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.List;

@Service
public class SignatureVerifierService {

    private static final Logger log = LoggerFactory.getLogger(SignatureVerifierService.class);

    @Value("${skillhub.security.cosign.fulcio-url:https://fulcio.sigstore.dev}")
    private String fulcioUrl;

    @Value("${skillhub.security.cosign.rekor-url:https://rekor.sigstore.dev}")
    private String rekorUrl;

    @Value("${skillhub.security.cosign.allowed-issuers:https://token.actions.githubusercontent.com}")
    private List<String> allowedIssuers;

    public VerificationResult verify(byte[] skillArchive, byte[] signature, byte[] certificate) {
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("cosign-verify-");
            Path archivePath = tempDir.resolve("archive");
            Path sigPath = tempDir.resolve("signature.sig");
            Path certPath = tempDir.resolve("certificate.crt");

            Files.write(archivePath, skillArchive);
            Files.write(sigPath, signature);
            Files.write(certPath, certificate);

            ProcessBuilder pb = new ProcessBuilder(
                    "cosign", "verify-blob",
                    "--signature", sigPath.toString(),
                    "--certificate", certPath.toString(),
                    "--certificate-oidc-issuer", fulcioUrl,
                    archivePath.toString()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            String output = new String(process.getInputStream().readAllBytes());
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                return VerificationResult.verified(output);
            } else {
                log.warn("Cosign verification failed: {}", output);
                return VerificationResult.failed(output);
            }
        } catch (IOException | InterruptedException e) {
            log.error("Cosign verification error", e);
            return VerificationResult.error(e.getMessage());
        } finally {
            if (tempDir != null) {
                try { Files.walk(tempDir).sorted(java.util.Comparator.reverseOrder())
                      .forEach(p -> { try { Files.delete(p); } catch (IOException ignored) {} });
                } catch (IOException ignored) {}
            }
        }
    }

    public record VerificationResult(boolean verified, String message) {
        public static VerificationResult verified(String msg) {
            return new VerificationResult(true, msg);
        }
        public static VerificationResult failed(String msg) {
            return new VerificationResult(false, msg);
        }
        public static VerificationResult error(String msg) {
            return new VerificationResult(false, "ERROR: " + msg);
        }
    }
}
