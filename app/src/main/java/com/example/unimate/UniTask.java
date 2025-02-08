package com.example.unimate;

import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class UniTask {
    private String taskId; // Firestore document ID
    private String taskTitle;
    private String taskDetails;
    private boolean completed;
    private String classTime;
    private String batch;
    private String section;
    private String instructorAcronym;

    @ServerTimestamp
    private Date date;

    // Required empty constructor for Firestore
    public UniTask() {}



    public UniTask(String taskTitle, String taskDetails, String classTime,
                   String batch, String section,String instructorAcronym) {
        this.taskTitle = taskTitle;
        this.taskDetails = taskDetails;
        this.classTime = classTime;
        this.batch = batch;
        this.section = section;
        this.completed = false;
        this.instructorAcronym = instructorAcronym.toUpperCase();// Default to not completed
    }

    // Add correct setter for instructorAcronym
    @PropertyName("instructor_acronym")
    public void setInstructorAcronym(String instructorAcronym) {
        this.instructorAcronym = instructorAcronym;
    }

    // Add proper getter for instructorAcronym
    @PropertyName("instructor_acronym")
    public String getInstructorAcronym() {
        return instructorAcronym != null ? instructorAcronym : "";
    }

    @PropertyName("task_title")
    public String getTaskTitle() {
        return taskTitle;
    }

    @PropertyName("task_title")
    public void setTaskTitle(String taskTitle) {
        this.taskTitle = taskTitle;
    }

    @PropertyName("task_details")
    public String getTaskDetails() {
        return taskDetails;
    }

    @PropertyName("task_details")
    public void setTaskDetails(String taskDetails) {
        this.taskDetails = taskDetails;
    }

    @PropertyName("completed")
    public boolean isCompleted() {
        return completed;
    }

    @PropertyName("completed")
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    @PropertyName("class_time")
    public String getClassTime() {
        return classTime;
    }

    @PropertyName("class_time")
    public void setClassTime(String classTime) {
        this.classTime = classTime;
    }

    @PropertyName("batch")
    public String getBatch() {
        return batch;
    }

    @PropertyName("batch")
    public void setBatch(String batch) {
        this.batch = batch;
    }

    @PropertyName("section")
    public String getSection() {
        return section;
    }

    @PropertyName("section")
    public void setSection(String section) {
        this.section = section;
    }

    @PropertyName("date")
    public Date getDate() {
        return date;
    }

    @PropertyName("date")
    public void setDate(Date date) {
        this.date = date;
    }

    // Not stored in Firestore, just for local use
    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

}
