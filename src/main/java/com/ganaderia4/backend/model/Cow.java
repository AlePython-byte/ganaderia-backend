package com.ganaderia4.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "cows")
public class Cow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "identifier", nullable = false, unique = true)
    private String token;

    @Column(unique = true)
    private String internalCode;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CowStatus status;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    private String observations;

    public Cow() {
    }

    public Cow(Long id, String token, String internalCode, String name, CowStatus status, String observations) {
        this(id, token, internalCode, name, status, true, observations);
    }

    public Cow(Long id, String token, String internalCode, String name, CowStatus status, Boolean active, String observations) {
        this.id = id;
        this.token = token;
        this.internalCode = internalCode;
        this.name = name;
        this.status = status;
        this.active = active;
        this.observations = observations;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
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

    public CowStatus getStatus() {
        return status;
    }

    public void setStatus(CowStatus status) {
        this.status = status;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }
}
