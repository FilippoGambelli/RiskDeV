package it.unipi.riskDeV.DTO.packageVersion;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReverseDependencyDTO {
    private String packageName;
    private List<String> versions = new ArrayList<>();
}
