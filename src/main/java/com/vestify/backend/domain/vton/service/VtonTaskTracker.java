package com.vestify.backend.domain.vton.service;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VtonTaskTracker {

    // 🚀 GEÇİCİ HAFIZA: İşlemlerin durumunu burada tutuyoruz (İleride DB'ye taşıyacağız)
    private final Map<String, TaskResult> tasks = new ConcurrentHashMap<>();

    // İşlem başladığında PENDING (Bekliyor) olarak kaydet
    public void startTask(String taskId) {
        tasks.put(taskId, new TaskResult(taskId, "PENDING", null));
    }

    // İşlem bittiğinde COMPLETED (Tamamlandı) yap ve fotoğrafı ekle
    public void completeTask(String taskId, String resultImageUrl) {
        tasks.put(taskId, new TaskResult(taskId, "COMPLETED", resultImageUrl));
    }

    // Telefon sonucu sorduğunda bu metotla cevap vereceğiz
    public TaskResult getTask(String taskId) {
        return tasks.get(taskId);
    }

    // Telefonun anlayacağı JSON formatı (DTO)
    public static class TaskResult {
        public String taskId;
        public String status;
        public String resultImageUrl;

        public TaskResult(String taskId, String status, String resultImageUrl) {
            this.taskId = taskId;
            this.status = status;
            this.resultImageUrl = resultImageUrl;
        }
    }
}