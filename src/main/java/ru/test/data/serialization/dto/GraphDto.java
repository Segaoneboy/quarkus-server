package ru.test.data.serialization.dto;

import com.google.gson.annotations.SerializedName;
import ru.test.domain.Profile;

import java.util.ArrayList;
import java.util.List;

public class GraphDto {

    @SerializedName("schema_version")
    private int schemaVersion = 3;

    @SerializedName("profiles")
    private List<Profile> profiles = new ArrayList<>();

    @SerializedName("edges")
    private List<EdgeDto> edges = new ArrayList<>();

    public GraphDto(){}

    public GraphDto(int schemaVersion, List<Profile> profiles, List<EdgeDto> edges) {
        this.schemaVersion = schemaVersion;
        this.profiles = (profiles != null) ? profiles : new ArrayList<>();
        this.edges = (edges != null) ? edges : new ArrayList<>();
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public List<Profile> getProfiles() {
        return profiles;
    }

    public void setProfiles(List<Profile> profiles) {
        this.profiles = profiles;
    }

    public List<EdgeDto> getEdges() {
        return edges;
    }

    public void setEdges(List<EdgeDto> edges) {
        this.edges = edges;
    }

}
