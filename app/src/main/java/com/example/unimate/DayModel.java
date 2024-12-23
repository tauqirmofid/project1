package com.example.unimate;

public class DayModel {
    private String dayName;
    private String tasks;

    public DayModel(String dayName, String tasks) {
        this.dayName = dayName;
        this.tasks = tasks;
    }

    public String getDayName() {
        return dayName;
    }

    public String getTasks() {
        return tasks;
    }
}