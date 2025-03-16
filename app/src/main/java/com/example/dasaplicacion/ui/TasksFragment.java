package com.example.dasaplicacion.ui;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.dasaplicacion.R;
import com.example.dasaplicacion.database.AppDatabase;
import com.example.dasaplicacion.model.Task;
import com.example.dasaplicacion.notifications.NotificationReceiver;
import com.example.dasaplicacion.workers.TaskNotificationWorker;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class TasksFragment extends Fragment {

    private RecyclerView recyclerView;
    private TaskAdapter adapter;
    private FloatingActionButton fabAddTask;
    private AppDatabase db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tasks, container, false);

        recyclerView = view.findViewById(R.id.recycler_tasks);
        fabAddTask = view.findViewById(R.id.fab_add_task);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Instanciar la base de datos
        db = Room.databaseBuilder(getContext().getApplicationContext(),
                AppDatabase.class, "tasks_db").build();

        // Cargar las tareas desde la base de datos
        loadTasks();

        // Mostrar el diálogo para agregar una tarea al pulsar el FAB
        fabAddTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAddTaskDialog();
            }
        });

        return view;
    }

    // Método para cargar las tareas futuras de la base de datos
    private void loadTasks() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<Task> dbTasks = db.taskDao().getUpcomingTasks(new Date());
                // Ordenar las tareas por fecha/hora (ascendente)
                Collections.sort(dbTasks, new Comparator<Task>() {
                    @Override
                    public int compare(Task t1, Task t2) {
                        return t1.getDateTime().compareTo(t2.getDateTime());
                    }
                });
                // Actualizar la UI en el hilo principal
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (adapter == null) {
                                adapter = new TaskAdapter(dbTasks);
                                recyclerView.setAdapter(adapter);
                            } else {
                                adapter.setTasks(dbTasks);
                                adapter.notifyDataSetChanged();
                            }
                        }
                    });
                }
            }
        }).start();
    }

    // Diálogo para agregar una nueva tarea y programar la notificación con WorkManager
    private void showAddTaskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_task, null);
        builder.setView(dialogView);

        final EditText etTitle = dialogView.findViewById(R.id.et_task_title);
        final EditText etDescription = dialogView.findViewById(R.id.et_task_description);
        final Button btnDate = dialogView.findViewById(R.id.btn_task_date);
        final Button btnTime = dialogView.findViewById(R.id.btn_task_time);

        // Selección de fecha
        btnDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar c = Calendar.getInstance();
                new DatePickerDialog(getContext(), new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        Calendar selected = Calendar.getInstance();
                        selected.set(year, month, dayOfMonth);
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        btnDate.setText(sdf.format(selected.getTime()));
                    }
                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        // Selección de hora
        btnTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar c = Calendar.getInstance();
                new TimePickerDialog(getContext(), new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        Calendar selected = Calendar.getInstance();
                        selected.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        selected.set(Calendar.MINUTE, minute);
                        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                        btnTime.setText(sdf.format(selected.getTime()));
                    }
                }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
            }
        });

        builder.setTitle("Agregar Tarea");
        builder.setPositiveButton("Agregar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String title = etTitle.getText().toString().trim();
                String description = etDescription.getText().toString().trim();
                String dateStr = btnDate.getText().toString().trim();
                String timeStr = btnTime.getText().toString().trim();

                if (!TextUtils.isEmpty(title) && !TextUtils.isEmpty(dateStr) && !TextUtils.isEmpty(timeStr)) {
                    String dateTimeStr = dateStr + " " + timeStr;
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                    sdf.setTimeZone(TimeZone.getDefault());
                    try {
                        final Date dateTime = sdf.parse(dateTimeStr);
                        final Task newTask = new Task(title, description, dateTime);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                db.taskDao().insertTask(newTask);
                                scheduleNotification(newTask);
                                loadTasks();
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            showTaskCreatedDialog();
                                        }
                                    });
                                }
                            }
                        }).start();
                    } catch (ParseException e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Error al procesar la fecha/hora", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show();
                }
                dialog.dismiss();
            }
        });

        builder.setNegativeButton("Cancelar", null);
        builder.create().show();
    }

    // Método para programar una notificación 1 hora antes de la fecha/hora de la tarea usando WorkManager
    private void scheduleNotification(Task task) {
        long delay = task.getDateTime().getTime() - System.currentTimeMillis() - (60 * 60 * 1000);
        if (delay < 0) {
            return;
        }
        Data data = new Data.Builder()
                .putString(TaskNotificationWorker.KEY_TASK_TITLE, task.getTitle())
                .putInt(TaskNotificationWorker.KEY_TASK_ID, task.getId())
                .build();
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(TaskNotificationWorker.class)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .build();
        WorkManager.getInstance(getContext()).enqueue(workRequest);
    }

    // Adapter para el RecyclerView de tareas
    private class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
        private List<Task> taskList;

        public TaskAdapter(List<Task> taskList) {
            this.taskList = taskList;
        }
        public void setTasks(List<Task> taskList) {
            this.taskList = taskList;
        }
        @NonNull
        @Override
        public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
            return new TaskViewHolder(view);
        }
        @Override
        public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
            Task task = taskList.get(position);
            // Mostrar header si es el primer elemento o si cambia el día respecto al anterior
            if (position == 0 || !isSameDay(task.getDateTime(), taskList.get(position - 1).getDateTime())) {
                Calendar cal = Calendar.getInstance(TimeZone.getDefault());
                cal.setTime(task.getDateTime());
                int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK); // Domingo = 1, Lunes = 2, etc.
                int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
                int month = cal.get(Calendar.MONTH); // 0-indexado

                // Ajuste para que lunes sea el primer día (índice 0)
                int weekDayIndex = (dayOfWeek + 5) % 7;

                Context context = holder.itemView.getContext();
                String[] weekDays = context.getResources().getStringArray(R.array.week_days);
                String[] months = context.getResources().getStringArray(R.array.calendar_months);

                String headerText = weekDays[weekDayIndex] + ", " + dayOfMonth + " de " + months[month];
                holder.tvDayHeader.setText(headerText);
                holder.tvDayHeader.setVisibility(View.VISIBLE);
            } else {
                holder.tvDayHeader.setVisibility(View.GONE);
            }
            holder.bind(task);
            // Al hacer click en la carta se abre el diálogo con el detalle de la tarea
            holder.itemView.setOnClickListener(v -> {
                TaskDetailDialogFragment dialog = TaskDetailDialogFragment.newInstance(task, db, new TaskDetailDialogFragment.OnTaskActionListener() {
                    @Override
                    public void onTaskModified(Task task) {
                        loadTasks();
                    }
                    @Override
                    public void onTaskDeleted(Task task) {
                        loadTasks();
                    }
                });
                dialog.show(getParentFragmentManager(), "TaskDetailDialog");
            });
        }
        @Override
        public int getItemCount() {
            return taskList.size();
        }
        class TaskViewHolder extends RecyclerView.ViewHolder {
            TextView tvDayHeader;
            TextView tvTitle;
            TextView tvDateTime;
            public TaskViewHolder(@NonNull View itemView) {
                super(itemView);
                tvDayHeader = itemView.findViewById(R.id.tv_day_header);
                tvTitle = itemView.findViewById(R.id.tv_task_title);
                tvDateTime = itemView.findViewById(R.id.tv_task_datetime);
            }
            public void bind(Task task) {
                tvTitle.setText(task.getTitle());
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                sdf.setTimeZone(TimeZone.getDefault());
                tvDateTime.setText(sdf.format(task.getDateTime()));
            }
        }
        // Método auxiliar para comparar si dos fechas pertenecen al mismo día
        private boolean isSameDay(Date d1, Date d2) {
            Calendar c1 = Calendar.getInstance(TimeZone.getDefault());
            Calendar c2 = Calendar.getInstance(TimeZone.getDefault());
            c1.setTime(d1);
            c2.setTime(d2);
            return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                    && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
        }
    }

    private void showTaskCreatedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Tarea creada");
        builder.setMessage("La tarea ha sido creada exitosamente.");
        builder.setPositiveButton("OK", null);
        builder.create().show();
    }
}
