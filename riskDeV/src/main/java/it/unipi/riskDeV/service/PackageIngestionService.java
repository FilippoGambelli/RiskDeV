package it.unipi.riskDeV.service;

import it.unipi.riskDeV.APIClient.PyPiResponseDTO;
import it.unipi.riskDeV.DTO.PackageVersionDTO;
import it.unipi.riskDeV.APIClient.PyPiMapper;
import it.unipi.riskDeV.common.Result;
import it.unipi.riskDeV.model.PackageVersion.EmbeddedVulnerability;
import it.unipi.riskDeV.repository.PackageVersionRepository;
import it.unipi.riskDeV.APIClient.OsvApiClient;
import it.unipi.riskDeV.APIClient.PyPiApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
@RequiredArgsConstructor
@Slf4j
public class PackageIngestionService {

    private final PackageService packageService;
    private final PyPiApiClient pyPiApiClient;
    private final OsvApiClient osvApiClient;
    private final PackageVersionRepository packageVersionRepository;

    // Queue thread-safe of packages that we need to download
    private final Queue<String> packageQueue = new ConcurrentLinkedQueue<>();

    public void enqueuePackage(String packageName) {
        // Check duplicated packages in the queue
        if (!packageQueue.contains(packageName)) {
            packageQueue.offer(packageName);
            log.info("Package '{}' added to ingestion queue. Queue size: {}", packageName, packageQueue.size());
        }
    }

    // Every 2 seconds
    @Scheduled(fixedDelay = 2000)
    public void processQueue() {
        if (packageQueue.isEmpty()) {
            return;
        }

        String packageName = packageQueue.poll();
        log.info("START mass ingestion for package: {}", packageName);

        try {
            // Complete map of all the releases on Pypi
            Optional<PyPiResponseDTO> fullDataOpt = pyPiApiClient.getFullPackageMetadata(packageName);

            if (fullDataOpt.isEmpty() || fullDataOpt.get().getReleases() == null) {
                log.warn("No releases found on PyPI for {}", packageName);
                return;
            }

            // List of keys of versions (ex. "1.0.0", "1.1.0")
            List<String> allVersions = new ArrayList<>(fullDataOpt.get().getReleases().keySet());
            log.info("Found {} versions for {} on PyPI. Checking discrepancies...", allVersions.size(), packageName);

            int count = 0;
            // For each version
            for (String version : allVersions) {
                try {
                    // If it doesn't exist in our DB
                    if (shouldProcessVersion(packageName, version)) {
                        processSingleVersion(packageName, version);
                        count++;
                        // Delay to avoid ban
                        Thread.sleep(200);
                    }
                } catch (Exception e) {
                    log.error("Error processing {} version {}: {}", packageName, version, e.getMessage());
                }
            }
            log.info("COMPLETED mass ingestion for {}. Actually imported {} new versions.", packageName, count);

        } catch (Exception e) {
            log.error("Critical error during ingestion of {}", packageName, e);
        }
    }

    // Check if the version already exists in our db
    private boolean shouldProcessVersion(String packageName, String version) {
        boolean exists = packageVersionRepository.existsByPackageNameAndVersion(packageName, version);
        if (exists) {
            log.debug("Skipping {} {} - already exists in DB.", packageName, version);
        }
        return !exists;
    }

    // Processing a version data
    private void processSingleVersion(String packageName, String version) {
        log.info("Downloading and importing metadata for {} {}", packageName, version);

        // Download metadata
        var versionMetaOpt = pyPiApiClient.getPackageVersionMetadata(packageName, version);

        if (versionMetaOpt.isPresent()) {
            PackageVersionDTO dto = PyPiMapper.toPackageVersionDTO(versionMetaOpt.get());

            // Download vulnerabilities from osv
            try {
                List<EmbeddedVulnerability> vulns = osvApiClient.getVulnerabilities(packageName, version);
                dto.setVulnerabilities(vulns);
            } catch (Exception e) {
                log.warn("OSV failed for {} {}, saving without vulns.", packageName, version);
            }

            // Save
            Result<Void> result = packageService.addNewVersion(packageName, dto);

            if (result instanceof Result.Failure) {
                log.warn("Failed to save {} {}: {}", packageName, version, ((Result.Failure<?>) result).error().message());
            }
        }
    }
}