package com.example.dasaplicacion.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.example.dasaplicacion.model.Task;  // Importar Task
import java.util.Date;
import java.util.List;
import androidx.room.Delete;

@Dao
public interface TaskDao {
    @Insert
    long insertTask(Task task);

    @Query("SELECT * FROM tasks WHERE dateTime > :currentTime ORDER BY dateTime ASC")
    List<Task> getUpcomingTasks(Date currentTime);

    @Query("SELECT * FROM tasks WHERE date(dateTime/1000, 'unixepoch', 'localtime') = date(:selectedTimestamp/1000, 'unixepoch', 'localtime') ORDER BY dateTime ASC")
    List<Task> getTasksForDay(long selectedTimestamp);

    @Query("SELECT * FROM tasks")
    List<Task> getAllTasks();

    @Delete
    void deleteTask(Task task);

}
