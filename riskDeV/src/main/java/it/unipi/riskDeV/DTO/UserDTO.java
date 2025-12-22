package it.unipi.riskDeV.DTO;

public class UserDTO {
    private String nome;
    private String cognome;

    public UserDTO(String nome, String cognome) {
        this.nome = nome;
        this.cognome = cognome;
    }

    public String getNome() { return nome; }
    public String getCognome() { return cognome; }
}

