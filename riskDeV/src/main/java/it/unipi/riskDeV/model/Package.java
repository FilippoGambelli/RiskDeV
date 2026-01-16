package it.unipi.riskDeV.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Data
@Document(collection = "package")
public class Package {

    @Id
    private String id;

    private String author;

    @Field("author_email")
    private String authorEmail;

    private String description;

    @Field("package_url")
    private String packageURL;

    private String summary;

    @Field("documentation")
    private String documentationURL;

    @Field("homepage")
    private String homepageURL;

}