package com.example.dasaplicacion.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;
import com.example.dasaplicacion.R;
import com.example.dasaplicacion.database.AppDatabase;
import com.example.dasaplicacion.model.Task;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.format.TitleFormatter;
import com.prolificinteractive.materialcalendarview.format.WeekDayFormatter;
import com.prolificinteractive.materialcalendarview.spans.DotSpan;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

public class CalendarFragment extends Fragment {

    private MaterialCalendarView calendarView;
    private RecyclerView tasksRecyclerView;
    private TasksAdapter tasksAdapter;
    private List<Task> tasksForSelectedDay;
    private AppDatabase db;
    // Conjunto de días con tareas, se actualizará a partir de la BBDD.
    private Set<CalendarDay> eventDates = new HashSet<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);
        calendarView = view.findViewById(R.id.calendarView);
        tasksRecyclerView = view.findViewById(R.id.recycler_tasks_day);

        // Instanciar la base de datos
        db = Room.databaseBuilder(getContext().getApplicationContext(),
                AppDatabase.class, "tasks_db").build();

        // Seleccionar el día de hoy por defecto
        CalendarDay today = CalendarDay.today();
        calendarView.setSelectedDate(today);

        // Configurar el formateador del título para mostrar el mes y el año utilizando recursos multilenguaje
        calendarView.setTitleFormatter(new TitleFormatter() {
            @Override
            public CharSequence format(CalendarDay day) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(day.getDate());
                int monthIndex = cal.get(Calendar.MONTH); // 0-based
                int year = cal.get(Calendar.YEAR);
                // Se obtiene el array de meses definido en los recursos según el idioma actual
                String[] months = getResources().getStringArray(R.array.calendar_months);
                return months[monthIndex] + " " + year;
            }
        });

        // Configurar el formateador de los días de la semana usando recursos
        calendarView.setWeekDayFormatter(new WeekDayFormatter() {
            @Override
            public CharSequence format(int dayOfWeek) {
                String[] weekDays = getResources().getStringArray(R.array.week_days);
                return weekDays[(dayOfWeek + 5) % 7];
            }
        });
        calendarView.state().edit().setFirstDayOfWeek(Calendar.MONDAY).commit();

        // Cargar los puntos (decoradores) a partir de las tareas en la base de datos
        loadEventDates();

        // Configurar RecyclerView para mostrar las tareas del día seleccionado
        tasksForSelectedDay = new ArrayList<>();
        tasksAdapter = new TasksAdapter(tasksForSelectedDay);
        tasksRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        tasksRecyclerView.setAdapter(tasksAdapter);

        // Cargar tareas para el día seleccionado desde la base de datos
        loadTasksForDay(today);

        // Actualizar la lista de tareas cuando se seleccione otro día
        calendarView.setOnDateChangedListener((widget, date, selected) -> loadTasksForDay(date));

        return view;
    }

    // Carga tareas para un día específico desde la BBDD
    private void loadTasksForDay(CalendarDay day) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Europe/Madrid"));
        cal.setTime(day.getDate());
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        final long startOfDay = cal.getTimeInMillis();

        new Thread(() -> {
            final List<Task> tasksFromDb = db.taskDao().getTasksForDay(startOfDay);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    tasksForSelectedDay.clear();
                    tasksForSelectedDay.addAll(tasksFromDb);
                    tasksAdapter.notifyDataSetChanged();
                });
            }
        }).start();
    }

    // Carga todos los días que tienen tareas y actualiza el decorador
    private void loadEventDates() {
        new Thread(() -> {
            final List<Task> allTasks = db.taskDao().getAllTasks();
            final Set<CalendarDay> daysWithTasks = new HashSet<>();
            for (Task task : allTasks) {
                Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Europe/Madrid"));
                cal.setTime(task.getDateTime());
                CalendarDay day = CalendarDay.from(cal.getTime());
                daysWithTasks.add(day);
            }
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    eventDates.clear();
                    eventDates.addAll(daysWithTasks);
                    calendarView.removeDecorators();
                    calendarView.addDecorator(new EventDecorator(eventDates));
                });
            }
        }).start();
    }

    // Decorador para marcar con un punto púrpura los días con tareas
    private class EventDecorator implements DayViewDecorator {
        private final Set<CalendarDay> dates;
        private final int color;

        public EventDecorator(Set<CalendarDay> dates) {
            this.dates = dates;
            this.color = Color.parseColor("#800080"); // Púrpura
        }

        @Override
        public boolean shouldDecorate(CalendarDay day) {
            return dates.contains(day);
        }

        @Override
        public void decorate(DayViewFacade view) {
            view.addSpan(new DotSpan(5, color));
        }
    }

    // Adapter para la lista de tareas del día seleccionado
    private class TasksAdapter extends RecyclerView.Adapter<TasksAdapter.TaskViewHolder> {
        private List<Task> tasks;

        public TasksAdapter(List<Task> tasks) {
            this.tasks = tasks;
        }

        public void setTasks(List<Task> tasks) {
            this.tasks = tasks;
        }

        @NonNull
        @Override
        public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_task, parent, false);
            return new TaskViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
            Task task = tasks.get(position);
            holder.bind(task);
            holder.itemView.setOnClickListener(v -> {
                TaskDetailDialogFragment dialog = TaskDetailDialogFragment.newInstance(task, db, new TaskDetailDialogFragment.OnTaskActionListener() {
                    @Override
                    public void onTaskModified(Task task) {
                        loadTasksForDay(CalendarDay.from(task.getDateTime()));
                        loadEventDates();
                    }
                    @Override
                    public void onTaskDeleted(Task task) {
                        loadTasksForDay(CalendarDay.from(task.getDateTime()));
                        loadEventDates();
                    }
                });
                dialog.show(getParentFragmentManager(), "TaskDetailDialog");
            });
        }

        @Override
        public int getItemCount() {
            return tasks.size();
        }

        class TaskViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle;
            TextView tvDateTime;

            public TaskViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tv_task_title);
                tvDateTime = itemView.findViewById(R.id.tv_task_datetime);
            }

            public void bind(Task task) {
                tvTitle.setText(task.getTitle());
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                sdf.setTimeZone(TimeZone.getTimeZone("Europe/Madrid"));
                tvDateTime.setText(sdf.format(task.getDateTime()));
            }
        }
    }
}
