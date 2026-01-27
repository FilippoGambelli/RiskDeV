package it.unipi.riskDeV.model.documentDB;

import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

}
