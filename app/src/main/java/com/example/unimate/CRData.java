package com.example.unimate;

public class CRData {
    public String name, email, studentId, phone, department, batch, section, password;
    public boolean isVerified;

    public CRData() {
        // Default constructor required for Firebase
    }

    public CRData(String name, String email, String studentId, String phone, String department, String batch, String section, String password, boolean isVerified) {
        this.name = name;
        this.email = email;
        this.studentId = studentId;
        this.phone = phone;
        this.department = department;
        this.batch = batch;
        this.section = section;
        this.password = password;
        this.isVerified = isVerified;
    }
}
