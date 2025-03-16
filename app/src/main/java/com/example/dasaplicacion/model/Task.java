package com.example.dasaplicacion.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.util.Date;

@Entity(tableName = "tasks")
public class Task {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String title;
    private String description;
    private Date dateTime;

    public Task(String title, String description, Date dateTime) {
        this.title = title;
        this.description = description;
        this.dateTime = dateTime;
    }

    // Getters y setters
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getTitle() {
        return title;
    }
    public String getDescription() {
        return description;
    }
    public Date getDateTime() {
        return dateTime;
    }
}
