package it.unipi.riskDeV.mapper;

import it.unipi.riskDeV.DTO.project.ProjectCreationDTO;
import it.unipi.riskDeV.DTO.project.ProjectDTO;
import it.unipi.riskDeV.model.Project;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProjectMapper {

    // From DTO to Entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "admin", ignore = true)
    @Mapping(target = "lastUpdate", ignore = true)   
    @Mapping(target = "collaborators", ignore = true) 
    Project toEntity(ProjectCreationDTO request);

    // From PackageInput DTO to ProjectPackage Entity
    // Missed mapping for riskScore and vulnerabilitiesCount as they will be set later
    Project.ProjectPackage toProjectPackageEntity(ProjectCreationDTO.PackageInput input);

    // From Entity to DTO
    ProjectDTO toDto(Project project);
}