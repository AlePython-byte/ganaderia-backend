package com.ganaderia4.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "collars")
public class Collar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "identifier", nullable = false, unique = true)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CollarStatus status;

    @OneToOne
    @JoinColumn(name = "cow_id", unique = true)
    private Cow cow;

    public Collar() {
    }

    public Collar(Long id, String token, CollarStatus status, Cow cow) {
        this.id = id;
        this.token = token;
        this.status = status;
        this.cow = cow;
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

    public CollarStatus getStatus() {
        return status;
    }

    public void setStatus(CollarStatus status) {
        this.status = status;
    }

    public Cow getCow() {
        return cow;
    }

    public void setCow(Cow cow) {
        this.cow = cow;
    }
}