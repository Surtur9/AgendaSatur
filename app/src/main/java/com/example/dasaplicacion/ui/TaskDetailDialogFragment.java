package com.example.dasaplicacion.ui;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.dasaplicacion.R;
import com.example.dasaplicacion.database.AppDatabase;
import com.example.dasaplicacion.model.Task;
import com.example.dasaplicacion.notifications.NotificationReceiver;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class TaskDetailDialogFragment extends DialogFragment {

    private Task task;
    private AppDatabase db;
    private OnTaskActionListener listener;

    // Interfaz para comunicar acciones a la actividad/fragmento que llama
    public interface OnTaskActionListener {
        void onTaskModified(Task task);
        void onTaskDeleted(Task task);
    }

    // Constructor
    public TaskDetailDialogFragment(Task task, AppDatabase db, OnTaskActionListener listener) {
        this.task = task;
        this.db = db;
        this.listener = listener;
    }

    public static TaskDetailDialogFragment newInstance(Task task, AppDatabase db, OnTaskActionListener listener) {
        return new TaskDetailDialogFragment(task, db, listener);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // Infla el layout con la CardView
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_task_detail, null);

        // Obtenemos las referencias a los elementos del layout
        ImageButton btnClose = view.findViewById(R.id.btn_close);
        TextView tvTitle = view.findViewById(R.id.tv_detail_title);
        TextView tvDateTime = view.findViewById(R.id.tv_detail_date_time);
        TextView tvDescription = view.findViewById(R.id.tv_detail_description);
        Button btnModify = view.findViewById(R.id.btn_modify);
        Button btnDelete = view.findViewById(R.id.btn_delete);

        // Configura el contenido
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        tvTitle.setText(task.getTitle());
        tvDateTime.setText(sdf.format(task.getDateTime()));
        tvDescription.setText(task.getDescription());

        btnClose.setOnClickListener(v -> dismiss());

        // Botón: Añadir al calendario
        btnModify.setText(getString(R.string.add_to_calendar));
        btnModify.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_INSERT)
                    .setData(CalendarContract.Events.CONTENT_URI)
                    .putExtra(CalendarContract.Events.TITLE, task.getTitle())
                    .putExtra(CalendarContract.Events.DESCRIPTION, task.getDescription())
                    .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, task.getDateTime().getTime());
            if (intent.resolveActivity(getContext().getPackageManager()) != null) {
                startActivity(intent);
                showCalendarResultDialog(getString(R.string.calendar_success));
            } else {
                showCalendarResultDialog(getString(R.string.calendar_not_available));
            }
            dismiss();
        });

        btnDelete.setOnClickListener(v -> {
            new Thread(() -> {
                db.taskDao().deleteTask(task);
                cancelNotification(task);
                if (listener != null && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        listener.onTaskDeleted(task);
                        showTaskDeletedDialog();
                    });
                }
            }).start();
            dismiss();
        });

        // Crea un AlertDialog
        androidx.appcompat.app.AlertDialog dialog =
                new androidx.appcompat.app.AlertDialog.Builder(requireContext()).create();
        dialog.setView(view);

        // Establecer el fondo del diálogo como transparente
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        return dialog;
    }

    // Método para mostrar un diálogo informativo al usuario tras intentar añadir al calendario
    private void showCalendarResultDialog(String message) {
        new AlertDialog.Builder(getContext())
                .setMessage(message)
                .setPositiveButton(getString(R.string.ok), (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    private void cancelNotification(Task task) {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getContext(), task.getId(),
                new Intent(getContext(), NotificationReceiver.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }

    private void showTaskDeletedDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.task_deleted_title))
                .setMessage(getString(R.string.task_deleted_message))
                .setPositiveButton(getString(R.string.ok), (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }
}
