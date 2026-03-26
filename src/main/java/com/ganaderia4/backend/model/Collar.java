package com.ganaderia4.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "collars")
public class Collar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String identifier;

    @Column(nullable = false)
    private String status;

    @OneToOne
    @JoinColumn(name = "cow_id", unique = true)
    private Cow cow;

    public Collar() {
    }

    public Collar(Long id, String identifier, String status, Cow cow) {
        this.id = id;
        this.identifier = identifier;
        this.status = status;
        this.cow = cow;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Cow getCow() {
        return cow;
    }

    public void setCow(Cow cow) {
        this.cow = cow;
    }
}