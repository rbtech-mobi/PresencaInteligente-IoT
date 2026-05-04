package com.presencainteligente.iot.geofence

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.presencainteligente.iot.data.MqttRepository

class MqttWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val status = inputData.getString("status") ?: return Result.failure()
        val temperature = inputData.getInt("target_temperature", 24)
        
        val repository = MqttRepository.getInstance()
        val success = repository.publishCommand(status, temperature)
        
        return if (success) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}
