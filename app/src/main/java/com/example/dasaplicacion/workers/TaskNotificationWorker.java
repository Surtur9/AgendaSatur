package com.example.dasaplicacion.workers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.example.dasaplicacion.R;

public class TaskNotificationWorker extends Worker {

    public static final String CHANNEL_ID = "task_notifications_channel";
    public static final String KEY_TASK_TITLE = "task_title";
    public static final String KEY_TASK_ID = "task_id";
    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";

    public TaskNotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        // Consultar en SharedPreferences si las notificaciones están activadas
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean notificationsEnabled = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true);
        if (!notificationsEnabled) {
            // Si están desactivadas, finalizamos sin enviar nada
            return Result.success();
        }

        // Extraer datos de la tarea
        String taskTitle = getInputData().getString(KEY_TASK_TITLE);
        int taskId = getInputData().getInt(KEY_TASK_ID, 0);

        // Crear el canal de notificación si es necesario
        createNotificationChannel();

        // Construir la notificación
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.satur)
                .setContentTitle("Tarea próxima a vencer")
                .setContentText("La tarea \"" + taskTitle + "\" vence en 1 hora")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        // Enviar la notificación
        NotificationManager notificationManager =
                (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(taskId, builder.build());
        }
        return Result.success();
    }

    private void createNotificationChannel() {
        // Para API 26+ (Android Oreo y superiores)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Tareas";
            String description = "Notificaciones de tareas próximas a vencerse";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = (NotificationManager)
                    getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
