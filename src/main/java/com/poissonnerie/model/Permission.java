package com.poissonnerie.model;

public class Permission {
    private Integer id;
    private String code;
    private String description;
    private String module;

    public Permission() {}

    public Permission(String code, String description, String module) {
        this.code = code;
        this.description = description;
        this.module = module;
    }

    // Getters
    public Integer getId() { return id; }
    public String getCode() { return code; }
    public String getDescription() { return description; }
    public String getModule() { return module; }

    // Setters
    public void setId(Integer id) { this.id = id; }
    public void setCode(String code) { this.code = code; }
    public void setDescription(String description) { this.description = description; }
    public void setModule(String module) { this.module = module; }

    @Override
    public String toString() {
        return "Permission{" +
            "id=" + id +
            ", code='" + code + '\'' +
            ", description='" + description + '\'' +
            ", module='" + module + '\'' +
            '}';
    }
}
