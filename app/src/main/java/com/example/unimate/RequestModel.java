package com.example.unimate;

public class RequestModel {
    private String email;
    private String id;
    private String phone;
    private String batch;
    private String section;
    private String department;
    private String designation;
    private String role; // New field for role (e.g., "Teacher" or "CR")
    private boolean isVerified;

    // Default constructor (required for Firebase)
    public RequestModel() {
    }

    // Parameterized constructor (optional, for testing or manual instantiation)
    public RequestModel(String email, String id, String phone, String batch, String section,
                        String department, String designation, String role, boolean isVerified) {
        this.email = email;
        this.id = id;
        this.phone = phone;
        this.batch = batch;
        this.section = section;
        this.department = department;
        this.designation = designation;
        this.role = role;
        this.isVerified = isVerified;
    }

    // Getters
    public String getEmail() {
        return email;
    }

    public String getId() {
        return id;
    }

    public String getPhone() {
        return phone;
    }

    public String getBatch() {
        return batch;
    }

    public String getSection() {
        return section;
    }

    public String getDepartment() {
        return department;
    }

    public String getDesignation() {
        return designation;
    }

    public String getRole() {
        return role; // Getter for the new role field
    }

    public boolean isVerified() {
        return isVerified;
    }

    // Setters
    public void setEmail(String email) {
        this.email = email;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setBatch(String batch) {
        this.batch = batch;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public void setRole(String role) {
        this.role = role; // Setter for the new role field
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }
}
