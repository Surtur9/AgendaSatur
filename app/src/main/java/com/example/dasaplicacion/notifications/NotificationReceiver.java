package com.example.dasaplicacion.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.example.dasaplicacion.R;

public class NotificationReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID = "task_notifications_channel";
    public static final String EXTRA_TASK_TITLE = "extra_task_title";
    public static final String EXTRA_TASK_ID = "extra_task_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Extraer información de la tarea desde el Intent
        String taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE);
        int taskId = intent.getIntExtra(EXTRA_TASK_ID, 0);

        // Crear NotificationChannel si es necesario
        createNotificationChannel(context);

        // Construir la notificación utilizando satur.png como icono
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.satur)  // Usar satur.png
                .setContentTitle("Tarea próxima a vencer")
                .setContentText("La tarea \"" + taskTitle + "\" vence en 1 hora")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        // Mostrar la notificación
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(taskId, builder.build());
        }
    }

    private void createNotificationChannel(Context context) {
        // Crear canal para API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Tareas";
            String description = "Notificaciones de tareas próximas a vencerse";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
