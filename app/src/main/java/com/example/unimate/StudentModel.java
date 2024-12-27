package com.example.unimate;
public class StudentModel {
    public String email;
    public String studentId;
    public String department;
    public String batch;
    public String section;
    public String verificationCode;
    public boolean isVerified;

    public StudentModel() {}

    public StudentModel(String email, String studentId, String department, String batch, String section, String verificationCode, boolean isVerified) {
        this.email = email;
        this.studentId = studentId;
        this.department = department;
        this.batch = batch;
        this.section = section;
        this.verificationCode = verificationCode;
        this.isVerified = isVerified;
    }
}

