package it.unipi.riskDeV.DTO.packageVersion;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReverseDependencyDTO {
    @Schema(description = "package name", example = "numpy")
    private String packageName;

    @Schema(description = "list of versions")
    private List<String> versions = new ArrayList<>();
}
