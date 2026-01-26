package it.unipi.riskDeV.model;

import org.springframework.data.mongodb.core.mapping.Field;

import it.unipi.riskDeV.DTO.packageVersion.ConstraintsDTO;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Constraints {

    private String full;
    private String name;

    @Field("version_gte")
    private String versionGte;

    @Field("version_lte")
    private String versionLte;

    @Field("version_gt")
    private String versionGt;

    @Field("version_lt")
    private String versionLt;

    @Field("version_eq")
    private String versionEq;

    @Field("version_neq")
    private String versionNeq;

    public Constraints(ConstraintsDTO constraintDto) {
        this.full = constraintDto.getFull();
        this.name = constraintDto.getName();
        this.versionGte = constraintDto.getVersionGte();
        this.versionLte = constraintDto.getVersionLte();
        this.versionGt = constraintDto.getVersionGt();
        this.versionLt = constraintDto.getVersionLt();
        this.versionEq = constraintDto.getVersionEq();
        this.versionNeq = constraintDto.getVersionNeq();
    }
}
