package it.unipi.riskDeV.DTO;

import lombok.Data;
import java.util.List;
import java.util.stream.Collectors;
import it.unipi.riskDeV.model.GeneralPackage;

@Data
public class GeneralPackageDTO {
    private String packageName;
    private String author;
    private String description;
    private String url;
    // Only the list of version numbers (ex. "1.0.0", "1.0.1") is returned
    private List<String> versions;

    // Constructor for the conversion: model -> DTO
    public GeneralPackageDTO(GeneralPackage model) {
        this.packageName = model.getPackage_name();
        this.author = model.getAuthor();
        this.description = model.getDescription();
        this.url = model.getPackage_url();
        
        // Extraction version numbers
        if (model.getVersions() != null) {
            this.versions = model.getVersions().stream()
                .map(GeneralPackage.PackageVersionSummary::getVersion)
                .collect(Collectors.toList());
        }
    }
}