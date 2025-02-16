package com.demo.unimate;

public class TeacherData {
    public String name, email, acronym,teacherId, phone, department, designation, password;
    public boolean isVerified;

    public TeacherData() {
        // Default constructor required for Firebase
    }

    public TeacherData(String name, String email, String acronym,String teacherId, String phone, String department, String designation, String password, boolean isVerified) {
        this.name = name;
        this.email = email;
        this.acronym = acronym;
        this.teacherId = teacherId;
        this.phone = phone;
        this.department = department;
        this.designation = designation;
        this.password = password;
        this.isVerified = isVerified;
    }
}
