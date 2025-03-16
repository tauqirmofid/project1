package com.demo.unimate;

public class Course {
    private String day;
    private String timeSlot;
    private String courseName;
    private String instructor;
    private String room;

    public Course(String day, String timeSlot, String courseName, String instructor, String room) {
        this.day = day;
        this.timeSlot = timeSlot;
        this.courseName = courseName;
        this.instructor = instructor;
        this.room = room;
    }

    // Getters
    public String getDay() { return day; }
    public String getTimeSlot() { return timeSlot; }
    public String getCourseName() { return courseName; }
    public String getInstructor() { return instructor; }
    public String getRoom() { return room; }
}
