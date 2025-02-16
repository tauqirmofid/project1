package com.demo.unimate;

public class DayModel {
    private String dayName;   // e.g. "Monday"

    // One string per row/time slot in item_day_card.xml
    private String class1;
    private String class2;
    private String class3;
    private String class4;
    private String class5;
    private String class6;
    private String class7;

    public DayModel(String dayName) {
        this.dayName = dayName;
        // Default all to "No Class" or empty
        this.class1 = "No Class";
        this.class2 = "No Class";
        this.class3 = "No Class";
        this.class4 = "No Class";
        this.class5 = "No Class";
        this.class6 = "No Class";
        this.class7 = "No Class";
    }

    public String getDayName() {
        return dayName;
    }

    // Getters/setters for each time slot
    public String getClass1() { return class1; }
    public void setClass1(String class1) { this.class1 = class1; }

    public String getClass2() { return class2; }
    public void setClass2(String class2) { this.class2 = class2; }

    public String getClass3() { return class3; }
    public void setClass3(String class3) { this.class3 = class3; }

    public String getClass4() { return class4; }
    public void setClass4(String class4) { this.class4 = class4; }

    public String getClass5() { return class5; }
    public void setClass5(String class5) { this.class5 = class5; }

    public String getClass6() { return class6; }
    public void setClass6(String class6) { this.class6 = class6; }

    public String getClass7() { return class7; }
    public void setClass7(String class7) { this.class7 = class7; }
}
