package com.demo.unimate;

import java.util.ArrayList;
import java.util.List;

public class ClassWithTasks {
    private String timeSlot;
    private String course;
    private String instructor;
    private String room;
    private List<UniTask> tasks = new ArrayList<>();

    public ClassWithTasks(String timeSlot, String course, String instructor, String room) {
        this.timeSlot = timeSlot;
        this.course = course;
        this.instructor = instructor;
        this.room = room;
    }
    public void setTasks(List<UniTask> tasks) {
        this.tasks.clear();
        this.tasks.addAll(tasks);
    }

    // Getters
    public String getTimeSlot() { return timeSlot; }
    public String getCourse() { return course; }
    public String getInstructor() { return instructor; }
    public String getRoom() { return room; }
    public List<UniTask> getTasks() { return tasks; }


    public void addTask(UniTask task) { tasks.add(task); }
    public boolean hasTasks() { return !tasks.isEmpty(); }
}
