package it.unipi.riskDeV.APIClient;

import it.unipi.riskDeV.DTO.packageVersion.PackageVersionDTO;
import java.util.ArrayList;

public class PyPiMapper {

    public static PackageVersionDTO toPackageVersionDTO(PyPiResponseDTO pypiData) {
        PyPiResponseDTO.Info info = pypiData.getInfo();
        
        PackageVersionDTO dto = new PackageVersionDTO();
        dto.setPackageName(info.getName());
        dto.setVersion(info.getVersion());
        dto.setAuthor(info.getAuthor());
        dto.setAuthorEmail(info.getAuthorEmail());
        dto.setDescription(info.getSummary());
        dto.setRequiresPython(info.getRequiresPython());
        
        // dto.setDependencies(info.getRequiresDist() != null ? info.getRequiresDist() : new ArrayList<>());
        
        // URL
        if (info.getProjectUrls() != null) {
            dto.setPackageURL(info.getProjectUrls().get("Homepage"));
            dto.setDocumentationURL(info.getProjectUrls().get("Documentation"));
            // Fallback se Documentation è null
            if (dto.getPackageURL() == null && !info.getProjectUrls().isEmpty()) {
                dto.setPackageURL(info.getProjectUrls().values().iterator().next());
            }
        }

        if (pypiData.getUrls() != null && !pypiData.getUrls().isEmpty()) {
            // Take the date
            dto.setUploadTime(pypiData.getUrls().get(0).getUploadTime());
        } else {
            // Fallback if the list is empty (avoiding crash or errors)
            dto.setUploadTime(java.time.Instant.now().toString()); // <----------------------------------------------
        }
                
        // Vulnerabilities: PyPI JSON standard doesn't give us them
        dto.setVulnerabilities(new ArrayList<>());
        
        return dto;
    }
}