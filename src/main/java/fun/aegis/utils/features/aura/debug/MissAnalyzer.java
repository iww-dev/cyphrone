package fun.aegis.utils.features.aura.debug;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import java.util.List;

/**
 * Продвинутый анализатор причин мисса с отслеживанием статистики
 */
public class MissAnalyzer {
    
    private static final float ROTATION_THRESHOLD = 0.1f;
    private static final float VELOCITY_THRESHOLD = 0.3f;
    private static final int PING_THRESHOLD_HIGH = 200;
    private static final int PING_THRESHOLD_MEDIUM = 100;
    
    private long lastAttackTime = 0;
    private Vec3d lastTargetPos = null;
    private float lastRotationDelta = 0;
    private int consecutiveMisses = 0;
    
    /**
     * Анализирует мисс с учетом истории (умный анализ с несколькими причинами)
     */
    public List<MissReasonNotifier.MissReason> analyzeSmartMissWithHistory(
            LivingEntity target,
            float hitChance,
            int ping,
            boolean wallBlocked,
            float playerReach,
            float targetDistance,
            float rotationAccuracy,
            float rotationSpeed
    ) {
        
        if (target == null) {
            return List.of(MissReasonNotifier.MissReason.ENTITY_DESPAWNED);
        }
        
        // Проверяем состояние цели
        if (!target.isAlive()) {
            return List.of(MissReasonNotifier.MissReason.PLAYER_DEAD);
        }
        
        if (target.isInvisible()) {
            return List.of(MissReasonNotifier.MissReason.PLAYER_INVISIBLE);
        }
        
        // Проверяем дальность
        if (targetDistance > playerReach) {
            return List.of(MissReasonNotifier.MissReason.OUT_OF_RANGE);
        }
        
        // Проверяем стены
        if (wallBlocked) {
            return List.of(MissReasonNotifier.MissReason.WALL_BLOCKED);
        }
        
        // Проверяем защиту
        if (target.isBlocking()) {
            return List.of(MissReasonNotifier.MissReason.SHIELD_ACTIVE);
        }
        
        // Анализируем движение цели
        List<MissReasonNotifier.MissReason> movementReasons = analyzeTargetMovement(target);
        if (!movementReasons.isEmpty()) {
            return movementReasons;
        }
        
        // Анализируем ротацию
        List<MissReasonNotifier.MissReason> rotationReasons = analyzeRotation(rotationAccuracy, rotationSpeed, ping);
        if (!rotationReasons.isEmpty()) {
            return rotationReasons;
        }
        
        // Анализируем сетевые проблемы
        List<MissReasonNotifier.MissReason> networkReasons = analyzeNetwork(ping);
        if (!networkReasons.isEmpty()) {
            return networkReasons;
        }
        
        // Анализируем хитчанс
        if (hitChance < 40) {
            return List.of(MissReasonNotifier.MissReason.LOW_HIT_CHANCE);
        }
        
        // Если ничего не подходит
        consecutiveMisses++;
        if (consecutiveMisses > 3) {
            return List.of(MissReasonNotifier.MissReason.ANTICHEAT_FLAG);
        }
        
        return List.of(MissReasonNotifier.MissReason.UNKNOWN);
    }
    
    /**
     * Анализирует движение цели (может быть несколько причин)
     */
    private List<MissReasonNotifier.MissReason> analyzeTargetMovement(LivingEntity target) {
        List<MissReasonNotifier.MissReason> reasons = new java.util.ArrayList<>();
        Vec3d currentPos = target.getPos();
        float velocity = (float) target.getVelocity().length();
        
        // Проверяем состояния
        if (target.isGliding()) {
            reasons.add(MissReasonNotifier.MissReason.PLAYER_GLIDING);
        }
        
        if (target.isSprinting() && velocity > 0.7f) {
            reasons.add(MissReasonNotifier.MissReason.PLAYER_SPRINTING);
        }
        
        if (!target.isOnGround()) {
            reasons.add(MissReasonNotifier.MissReason.PLAYER_JUMPING);
        }
        
        if (target.isSneaking()) {
            reasons.add(MissReasonNotifier.MissReason.PLAYER_SNEAKING);
        }
        
        if (target.isTouchingWater()) {
            reasons.add(MissReasonNotifier.MissReason.PLAYER_IN_WATER);
        }
        
        if (target.isInLava()) {
            reasons.add(MissReasonNotifier.MissReason.PLAYER_IN_LAVA);
        }
        
        if (target.isClimbing()) {
            reasons.add(MissReasonNotifier.MissReason.PLAYER_CLIMBING);
        }
        
        if (velocity > VELOCITY_THRESHOLD) {
            reasons.add(MissReasonNotifier.MissReason.PLAYER_MOVED);
        }
        
        // Проверяем телепортацию
        if (lastTargetPos != null) {
            double distance = currentPos.distanceTo(lastTargetPos);
            if (distance > 10.0) {
                lastTargetPos = currentPos;
                reasons.add(MissReasonNotifier.MissReason.ENTITY_TELEPORTED);
            }
        }
        
        lastTargetPos = currentPos;
        return reasons;
    }
    
    /**
     * Анализирует ротацию (может быть несколько причин)
     */
    private List<MissReasonNotifier.MissReason> analyzeRotation(float rotationAccuracy, float rotationSpeed, int ping) {
        List<MissReasonNotifier.MissReason> reasons = new java.util.ArrayList<>();
        
        // Проверяем точность ротации
        if (rotationAccuracy < 0.3f) {
            reasons.add(MissReasonNotifier.MissReason.WRONG_ROTATION);
        }
        
        // Проверяем скорость ротации
        if (rotationSpeed > 180.0f) {
            reasons.add(MissReasonNotifier.MissReason.ROTATION_SPEED_TOO_HIGH);
        }
        
        if (rotationSpeed < 0.5f && rotationAccuracy < 0.7f) {
            reasons.add(MissReasonNotifier.MissReason.ROTATION_SPEED_TOO_LOW);
        }
        
        // Проверяем десинк
        if (rotationAccuracy < 0.5f && ping > 100) {
            reasons.add(MissReasonNotifier.MissReason.ROTATION_DESYNC);
        }
        
        return reasons;
    }
    
    /**
     * Анализирует сетевые проблемы (может быть несколько причин)
     */
    private List<MissReasonNotifier.MissReason> analyzeNetwork(int ping) {
        List<MissReasonNotifier.MissReason> reasons = new java.util.ArrayList<>();
        
        if (ping > PING_THRESHOLD_HIGH) {
            reasons.add(MissReasonNotifier.MissReason.NETWORK_LATENCY);
        }
        
        if (ping > PING_THRESHOLD_MEDIUM) {
            reasons.add(MissReasonNotifier.MissReason.HIGH_PING);
        }
        
        return reasons;
    }
    
    /**
     * Сбрасывает счетчик мисса при успешной атаке
     */
    public void resetOnHit() {
        consecutiveMisses = 0;
        lastAttackTime = System.currentTimeMillis();
    }
    
    /**
     * Получает количество последовательных мисса
     */
    public int getConsecutiveMisses() {
        return consecutiveMisses;
    }
    
    /**
     * Получает время с последней атаки
     */
    public long getTimeSinceLastAttack() {
        return System.currentTimeMillis() - lastAttackTime;
    }
}
