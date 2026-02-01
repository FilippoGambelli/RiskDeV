package it.unipi.riskDeV.model.documentDB;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import it.unipi.riskDeV.DTO.packageVersion.AddPackageVersionDTO;
import it.unipi.riskDeV.DTO.packageVersion.EmbeddedVulnerabilityDTO;
import it.unipi.riskDeV.DTO.packageVersion.PackageVersionDTO;
import it.unipi.riskDeV.util.DependencyParser;
import it.unipi.riskDeV.util.VersionParser;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "package")
public class PackageVersion {

    @Id
    private String id;

    @Field("package_name")
    private String packageName;

    private String version;

    @Field("version_array")
    private List<Integer> versionArray = new ArrayList<>();

    private String author;

    @Field("author_email")
    private String authorEmail;

    private String description;

    @Field("package_url")
    private String packageURL;

    @Field("documentation")
    private String documentationURL;

    @Field("upload_time")
    private Instant uploadTime;

    @Field("requires_dist")
    private List<Constraints> dependencies = new ArrayList<>();

    @Field("requires_python")
    private String requiresPython;
    
    @Field("risk_score")
    private Double riskScore;

    private List<EmbeddedVulnerability> vulnerabilities = new ArrayList<>();    

    public PackageVersion(PackageVersionDTO dto) {
        this.packageName = dto.getPackageName();
        this.version = dto.getVersion();
        this.author = dto.getAuthor();
        this.authorEmail = dto.getAuthorEmail();
        this.description = dto.getDescription();
        this.packageURL = dto.getPackageURL();
        this.documentationURL = dto.getDocumentationURL();
        this.uploadTime = Instant.parse(dto.getUploadTime());
        this.requiresPython = dto.getRequiresPython();
        this.riskScore = dto.getRiskScore();
        this.versionArray = new ArrayList<>();

        this.dependencies = new ArrayList<>();
        if (dto.getDependencies() != null) {
            for (String dep : dto.getDependencies()) {
                this.dependencies.add(DependencyParser.parseFullString(dep));
            }
        }

        this.vulnerabilities = new ArrayList<>();
        for(EmbeddedVulnerabilityDTO tmp: dto.getVulnerabilities()) {
            this.vulnerabilities.add(new EmbeddedVulnerability(tmp));
        }
    }

    public PackageVersion(AddPackageVersionDTO dto, VersionParser versionParser) {
        this.packageName = dto.getPackageName();
        this.version = dto.getVersion();
        this.author = dto.getAuthor();
        this.authorEmail = dto.getAuthorEmail();
        this.description = dto.getDescription();
        this.packageURL = dto.getPackageURL();
        this.documentationURL = dto.getDocumentationURL();
        this.uploadTime = Instant.parse(dto.getUploadTime());
        this.requiresPython = dto.getRequiresPython();
        this.versionArray = versionParser.generateVersionArray(dto.getVersion());
        
        this.dependencies = new ArrayList<>();
        if (dto.getDependencies() != null) {
            for (String dep : dto.getDependencies()) {
                this.dependencies.add(DependencyParser.parseFullString(dep));
            }
        }

        this.vulnerabilities = new ArrayList<>();
        for(EmbeddedVulnerabilityDTO tmp: dto.getVulnerabilities()) {
            this.vulnerabilities.add(new EmbeddedVulnerability(tmp));
        }

        this.riskScore = 0.0;
    }
}