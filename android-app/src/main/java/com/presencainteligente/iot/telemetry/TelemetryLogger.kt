package com.presencainteligente.iot.telemetry

import android.net.TrafficStats
import android.os.Process
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * TelemetryLogger: Responsável pelo benchmarking de rede e latência.
 * 
 * Engenharia de Software:
 * - Benchmarking: Utiliza TrafficStats para medir consumo real de dados por UID.
 * - Latência: Medição fim-a-fim entre a intenção de publicação e o callback do broker.
 * - Exportação: Formato CSV para facilitar análise estatística em pesquisas acadêmicas.
 */
class TelemetryLogger(private val cacheDir: File) {
    
    data class TelemetryEntry(
        val timestamp: Long,
        val latencyMs: Long,
        val bytesSent: Long,
        val action: String
    )

    private val logs = mutableListOf<TelemetryEntry>()
    private var lastTxBytes = TrafficStats.getUidTxBytes(Process.myUid())

    /**
     * Inicia o cronômetro para medição de latência.
     */
    fun startMeasurement(): Long = System.currentTimeMillis()

    /**
     * Finaliza a medição e registra os dados de tráfego e tempo.
     * @param startTime O tempo de início retornado por [startMeasurement].
     * @param action Identificador da ação (ex: "Publish_Present").
     */
    fun stopAndLog(startTime: Long, action: String) {
        val endTime = System.currentTimeMillis()
        val currentTxBytes = TrafficStats.getUidTxBytes(Process.myUid())
        
        // TrafficStats pode retornar -1 se o dispositivo não suportar a medição por UID
        val diffBytes = if (lastTxBytes != -1L && currentTxBytes != -1L) {
            currentTxBytes - lastTxBytes
        } else 0L

        val entry = TelemetryEntry(
            timestamp = endTime,
            latencyMs = endTime - startTime,
            bytesSent = if (diffBytes > 0) diffBytes else 0,
            action = action
        )
        
        logs.add(entry)
        lastTxBytes = currentTxBytes
        Log.d("TELEMETRY", "Ação: $action | Latência: ${entry.latencyMs}ms | Bytes: ${entry.bytesSent}")
    }

    /**
     * Exporta o log acumulado para um arquivo CSV no diretório de cache.
     * @return O caminho absoluto do arquivo gerado ou null em caso de falha.
     */
    fun exportToCSV(): String? {
        if (logs.isEmpty()) return null
        
        val fileName = "telemetry_${System.currentTimeMillis()}.csv"
        val file = File(cacheDir, fileName)
        
        return try {
            file.bufferedWriter().use { writer ->
                writer.write("Timestamp;Acao;LatenciaMs;BytesEnviados\n")
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
                logs.forEach { 
                    val date = sdf.format(Date(it.timestamp))
                    writer.write("$date;${it.action};${it.latencyMs};${it.bytesSent}\n")
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            Log.e("TELEMETRY", "Erro ao exportar CSV", e)
            null
        }
    }

    fun getLastLog() = logs.lastOrNull()
}
