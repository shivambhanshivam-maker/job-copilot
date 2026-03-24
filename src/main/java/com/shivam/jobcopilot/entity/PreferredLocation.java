package com.shivam.jobcopilot.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "preferred_locations")
public class PreferredLocation {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private String cityName;     // e.g. "Berlin"  — used in JSearch query string

    @Column(nullable = false)
    private String countryCode;  // e.g. "de"      — used in JSearch country= param

    private String displayName;  // e.g. "Berlin, Germany" — UI only

    public PreferredLocation() {}

    public UUID getId() { return id; }

    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
}