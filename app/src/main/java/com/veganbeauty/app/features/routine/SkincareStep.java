package com.veganbeauty.app.features.routine;

public class SkincareStep {
    private int index;
    private String name;
    private String description;
    private boolean isChecked;

    public SkincareStep(int index, String name, String description, boolean isChecked) {
        this.index = index;
        this.name = name;
        this.description = description;
        this.isChecked = isChecked;
    }

    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isChecked() { return isChecked; }
    public void setChecked(boolean checked) { isChecked = checked; }
}
