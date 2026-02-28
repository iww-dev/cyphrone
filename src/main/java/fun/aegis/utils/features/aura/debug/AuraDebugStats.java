package fun.aegis.utils.features.aura.debug;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Отслеживает статистику килауры для отладки
 */
public class AuraDebugStats {
    
    private int totalAttacks = 0;
    private int totalHits = 0;
    private int totalMisses = 0;
    private long sessionStartTime = System.currentTimeMillis();
    
    private final Map<MissReasonNotifier.MissReason, Integer> missReasonCounts = new HashMap<>();
    
    private float averageRotationAccuracy = 0.0f;
    private float averagePing = 0.0f;
    private int maxConsecutiveMisses = 0;
    private int currentConsecutiveMisses = 0;
    
    /**
     * Регистрирует успешную атаку
     */
    public void recordHit() {
        totalAttacks++;
        totalHits++;
        currentConsecutiveMisses = 0;
    }
    
    /**
     * Регистрирует мисс с одной причиной
     */
    public void recordMiss(MissReasonNotifier.MissReason reason) {
        totalAttacks++;
        totalMisses++;
        currentConsecutiveMisses++;
        
        if (currentConsecutiveMisses > maxConsecutiveMisses) {
            maxConsecutiveMisses = currentConsecutiveMisses;
        }
        
        missReasonCounts.put(reason, missReasonCounts.getOrDefault(reason, 0) + 1);
    }
    
    /**
     * Регистрирует мисс с несколькими причинами
     */
    public void recordMultipleMisses(List<MissReasonNotifier.MissReason> reasons) {
        totalAttacks++;
        totalMisses++;
        currentConsecutiveMisses++;
        
        if (currentConsecutiveMisses > maxConsecutiveMisses) {
            maxConsecutiveMisses = currentConsecutiveMisses;
        }
        
        // Регистрируем каждую причину
        for (MissReasonNotifier.MissReason reason : reasons) {
            missReasonCounts.put(reason, missReasonCounts.getOrDefault(reason, 0) + 1);
        }
    }
    
    /**
     * Обновляет среднюю точность ротации
     */
    public void updateRotationAccuracy(float accuracy) {
        averageRotationAccuracy = (averageRotationAccuracy + accuracy) / 2.0f;
    }
    
    /**
     * Обновляет средний пинг
     */
    public void updateAveragePing(int ping) {
        averagePing = (averagePing + ping) / 2.0f;
    }
    
    /**
     * Получает процент попаданий
     */
    public float getHitPercentage() {
        if (totalAttacks == 0) return 0.0f;
        return (float) totalHits / totalAttacks * 100.0f;
    }
    
    /**
     * Получает процент мисса
     */
    public float getMissPercentage() {
        if (totalAttacks == 0) return 0.0f;
        return (float) totalMisses / totalAttacks * 100.0f;
    }
    
    /**
     * Получает самую частую причину мисса
     */
    public MissReasonNotifier.MissReason getMostCommonMissReason() {
        return missReasonCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(MissReasonNotifier.MissReason.UNKNOWN);
    }
    
    /**
     * Получает топ 3 причины мисса
     */
    public List<MissReasonNotifier.MissReason> getTopMissReasons(int count) {
        return missReasonCounts.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(count)
                .map(Map.Entry::getKey)
                .toList();
    }
    
    /**
     * Получает время сессии в секундах
     */
    public long getSessionDurationSeconds() {
        return (System.currentTimeMillis() - sessionStartTime) / 1000;
    }
    
    /**
     * Получает статистику в виде строки
     */
    public String getStatsString() {
        return String.format(
            "Aura Stats: Hits=%d, Misses=%d, Accuracy=%.1f%%, AvgPing=%.0f, MaxConsecutiveMisses=%d, Session=%ds",
            totalHits, totalMisses, getHitPercentage(), averagePing, maxConsecutiveMisses, getSessionDurationSeconds()
        );
    }
    
    /**
     * Получает детальную статистику
     */
    public String getDetailedStats() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Aura Debug Statistics ===\n");
        sb.append(String.format("Total Attacks: %d\n", totalAttacks));
        sb.append(String.format("Total Hits: %d\n", totalHits));
        sb.append(String.format("Total Misses: %d\n", totalMisses));
        sb.append(String.format("Hit Accuracy: %.2f%%\n", getHitPercentage()));
        sb.append(String.format("Average Rotation Accuracy: %.2f%%\n", averageRotationAccuracy * 100));
        sb.append(String.format("Average Ping: %.0f ms\n", averagePing));
        sb.append(String.format("Max Consecutive Misses: %d\n", maxConsecutiveMisses));
        sb.append(String.format("Session Duration: %d seconds\n", getSessionDurationSeconds()));
        
        if (!missReasonCounts.isEmpty()) {
            sb.append("\nMiss Reasons (Top 10):\n");
            missReasonCounts.entrySet().stream()
                    .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                    .limit(10)
                    .forEach(e -> sb.append(String.format("  %s: %d times\n", e.getKey().getShortName(), e.getValue())));
        }
        
        return sb.toString();
    }
    
    /**
     * Сбрасывает статистику
     */
    public void reset() {
        totalAttacks = 0;
        totalHits = 0;
        totalMisses = 0;
        sessionStartTime = System.currentTimeMillis();
        missReasonCounts.clear();
        averageRotationAccuracy = 0.0f;
        averagePing = 0.0f;
        maxConsecutiveMisses = 0;
        currentConsecutiveMisses = 0;
    }
    
    // Getters
    public int getTotalAttacks() {
        return totalAttacks;
    }
    
    public int getTotalHits() {
        return totalHits;
    }
    
    public int getTotalMisses() {
        return totalMisses;
    }
    
    public float getAverageRotationAccuracy() {
        return averageRotationAccuracy;
    }
    
    public float getAveragePing() {
        return averagePing;
    }
    
    public int getMaxConsecutiveMisses() {
        return maxConsecutiveMisses;
    }
    
    public int getCurrentConsecutiveMisses() {
        return currentConsecutiveMisses;
    }
}
