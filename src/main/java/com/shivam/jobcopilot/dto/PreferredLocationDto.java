package com.shivam.jobcopilot.dto;

public class PreferredLocationDto {
    private String cityName;
    private String countryCode;
    private String displayName;

    public PreferredLocationDto() {}

    public PreferredLocationDto(String cityName, String countryCode, String displayName) {
        this.cityName = cityName;
        this.countryCode = countryCode;
        this.displayName = displayName;
    }

    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
}