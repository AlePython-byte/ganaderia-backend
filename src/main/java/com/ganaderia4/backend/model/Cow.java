package com.ganaderia4.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "cows")
public class Cow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String identifier;

    @Column(unique = true)
    private String internalCode;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String status;

    private String observations;

    public Cow() {
    }

    public Cow(Long id, String identifier, String internalCode, String name, String status, String observations) {
        this.id = id;
        this.identifier = identifier;
        this.internalCode = internalCode;
        this.name = name;
        this.status = status;
        this.observations = observations;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getInternalCode() {
        return internalCode;
    }

    public void setInternalCode(String internalCode) {
        this.internalCode = internalCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }
}