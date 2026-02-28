package fun.aegis.utils.features.aura.rotations.impl;

import fun.aegis.Aegis;
import fun.aegis.features.impl.combat.Aura;
import fun.aegis.utils.features.aura.rotations.constructor.RotateConstructor;
import fun.aegis.utils.features.aura.striking.StrikeManager;
import fun.aegis.utils.features.aura.warp.Turns;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Box;

import java.security.SecureRandom;
import java.util.concurrent.ThreadLocalRandom;
import java.util.LinkedList;
import java.util.Queue;
import java.util.ArrayList;
import java.util.List;

public class HWAngle extends RotateConstructor {
    private float tick = 0;
    private final SecureRandom secureRandom = new SecureRandom();
    
    // Для отслеживания momentum и ускорения
    private float currentMomentum = 0f;
    private float targetMomentum = 0f;
    private long lastMomentumUpdate = 0;
    private long reactionDelayEnd = 0;
    
    // Для вариативности
    private float speedVariation = 1.0f;
    private long lastSpeedVariationUpdate = 0;
    private float momentumInfluence = 0f;
    
    // Для микро-движений
    private float microMovementPhase = 0f;
    
    // Предиктивные системы
    private Vec3d lastTargetVelocity = Vec3d.ZERO;
    private float accelerationFactor = 0f;
    private float velocityMagnitude = 0f;
    
    // Chaos-based Spread Compensation
    private static final int SPREAD_COMPENSATION_ITERATIONS = 50;
    private static final int CHAOS_POINTS_COUNT = 5000;
    private static final int CHAOS_ITERATIONS = 3;
    private long lastSpreadCompensationUpdate = 0;
    private final java.util.Random noiseRandom = new java.util.Random();
    private Vec3d optimalPath = Vec3d.ZERO;
    private long lastChaosUpdate = 0;
    private boolean wasJumping = false;
    private long jumpDetectedTime = 0;
    private Vec3d jumpPredictedPos = Vec3d.ZERO;

    public HWAngle() {
        super("HolyWorld");
    }

    @Override
    public Turns limitAngleChange(Turns currentAngle, Turns targetAngle, Vec3d vec3d, Entity entity) {
        Aura aura = Aura.getInstance();
        StrikeManager attackHandler = Aegis.getInstance().getAttackPerpetrator().getAttackHandler();
        boolean canAttack = entity != null && attackHandler.canAttack(aura.getConfig(), 10);

        long currentTime = System.currentTimeMillis();
        
        boolean attackF = false;
        if (canAttack) {
            tick = 3;
            if (reactionDelayEnd == 0) {
                reactionDelayEnd = currentTime + ThreadLocalRandom.current().nextLong(40, 120);
            }
        }
        
        if (tick > 0) {
            attackF = true;
            tick--;
        }

        // ===== CHAOS THEORY PATHFINDING =====
        Vec3d targetPos = targetAngle.toVector();
        Vec3d finalTargetPos = targetPos;
        
        if (entity instanceof LivingEntity livingEntity && mc.player != null) {
            // Обновляем chaos pathfinding каждые 50ms
            if (currentTime - lastChaosUpdate > 50) {
                // 1. Основной pathfinding - от текущей позиции к хитбоксу врага
                Vec3d playerEyePos = mc.player.getEyePos();
                Box targetHitbox = livingEntity.getBoundingBox();
                Vec3d optimalTarget = calculateChaosPath(playerEyePos, targetHitbox, false);
                optimalPath = optimalTarget;
                
                // 2. Детекция прыжка и предикт
                if (!wasJumping && !livingEntity.isOnGround() && livingEntity.getVelocity().y > 0.1f) {
                    wasJumping = true;
                    jumpDetectedTime = currentTime;
                    
                    // Предиктим позицию врага в пике прыжка
                    jumpPredictedPos = calculateChaosPath(playerEyePos, targetHitbox, true);
                } else if (wasJumping && livingEntity.isOnGround()) {
                    wasJumping = false;
                }
                
                lastChaosUpdate = currentTime;
            }
            
            // Используем оптимальный путь или jump prediction
            if (wasJumping && currentTime - jumpDetectedTime < 300) {
                finalTargetPos = jumpPredictedPos;
            } else {
                finalTargetPos = optimalPath;
            }
            
            updateVelocityHistory(livingEntity, currentTime);
            
            Vec3d targetVelocity = livingEntity.getVelocity();
            lastTargetVelocity = targetVelocity;
            velocityMagnitude = (float) targetVelocity.length();
            accelerationFactor = calculateAcceleration(targetVelocity);
        }

        // Вычисляем разницу углов
        float yawDiff = MathHelper.wrapDegrees(targetAngle.getYaw() - currentAngle.getYaw());
        float pitchDiff = MathHelper.clamp(targetAngle.getPitch() - currentAngle.getPitch(), -90F, 90F);
        float angleDifference = (float) Math.hypot(Math.abs(yawDiff), Math.abs(pitchDiff));

        // Обновляем вариативность скорости
        if (currentTime - lastSpeedVariationUpdate > ThreadLocalRandom.current().nextLong(150, 250)) {
            speedVariation = 0.85f + (secureRandom.nextFloat() * 0.3f);
            lastSpeedVariationUpdate = currentTime;
        }

        // Momentum система
        updateMomentum(angleDifference, currentTime);

        // Базовые скорости с учётом предиктов
        float baseYawSpeed = 35.0f;
        float basePitchSpeed = 25.0f;

        // При атаке - snap с постепенным ускорением
        if (attackF && currentTime >= reactionDelayEnd) {
            float progress = 1.0f - (tick / 3.0f);
            float easeProgress = progress < 0.5f ? 2 * progress * progress : -1 + (4 - 2 * progress) * progress;
            
            // Ускорение зависит от momentum и скорости врага
            float velocityBoost = Math.min(velocityMagnitude * 20.0f, 25.0f);
            float accelerationBoost = 20.0f + (easeProgress * 35.0f) + (momentumInfluence * 15.0f) + velocityBoost;
            
            baseYawSpeed = 40.0f + accelerationBoost;
            basePitchSpeed = 30.0f + (accelerationBoost * 0.7f);
            
            baseYawSpeed += ThreadLocalRandom.current().nextFloat(-4, 6);
            basePitchSpeed += ThreadLocalRandom.current().nextFloat(-3, 4);
        } else if (attackF) {
            baseYawSpeed = 12.0f + (speedVariation * 3.0f);
            basePitchSpeed = 8.0f + (speedVariation * 2.0f);
        } else {
            // Адаптивная скорость с учётом скорости врага
            float velocityFactor = Math.min(velocityMagnitude * 15.0f, 20.0f);
            
            if (angleDifference > 40) {
                baseYawSpeed = 38.0f + (speedVariation * 5.0f) + velocityFactor;
                basePitchSpeed = 28.0f + (speedVariation * 4.0f) + (velocityFactor * 0.7f);
            } else if (angleDifference > 20) {
                baseYawSpeed = 32.0f + (speedVariation * 4.0f) + (velocityFactor * 0.7f);
                basePitchSpeed = 24.0f + (speedVariation * 3.0f) + (velocityFactor * 0.5f);
            } else {
                baseYawSpeed = 26.0f + (speedVariation * 3.0f) + (velocityFactor * 0.5f);
                basePitchSpeed = 19.0f + (speedVariation * 2.0f) + (velocityFactor * 0.3f);
            }
        }

        // Микро-движения
        microMovementPhase += 0.02f;
        float microMovementYaw = (float) (Math.sin(microMovementPhase) * 0.12f + Math.cos(microMovementPhase * 0.7f) * 0.08f);
        float microMovementPitch = (float) (Math.cos(microMovementPhase * 0.8f) * 0.09f + Math.sin(microMovementPhase * 1.2f) * 0.06f);

        // Легитные смещения
        float offsetX = (float) (Math.sin(currentTime / 1200D) * 0.8f);
        float offsetY = (float) (Math.cos(currentTime / 1500D) * 0.6f);

        Turns offsetAngle = new Turns(
                targetAngle.getYaw() + offsetX,
                MathHelper.clamp(targetAngle.getPitch() + offsetY, -90F, 90F)
        );

        Turns result = smoothRotation(currentAngle, offsetAngle, baseYawSpeed, basePitchSpeed, microMovementYaw, microMovementPitch);
        
        // ===== CHAOS-BASED SPREAD COMPENSATION =====
        // Компенсируем разброс оружия для идеального попадания
        if (entity instanceof LivingEntity && currentTime - lastSpreadCompensationUpdate > 30) {
            result = compensateSpread(result, targetAngle, currentTime);
            lastSpreadCompensationUpdate = currentTime;
        }
        
        return result;
    }

    // ===== CHAOS THEORY PATHFINDING =====
    
    private Vec3d calculateChaosPath(Vec3d startPos, Box targetHitbox, boolean isJumpPrediction) {
        Vec3d targetCenter = targetHitbox.getCenter();
        
        // Генерируем 5000 случайных точек вокруг хитбокса
        List<PathPoint> pathPoints = new ArrayList<>();
        
        // Расширяем область поиска для jump prediction
        float searchRadius = isJumpPrediction ? 3.5f : 2.5f;
        
        for (int i = 0; i < CHAOS_POINTS_COUNT; i++) {
            // Генерируем точку в сфере вокруг хитбокса
            float theta = (float) (Math.random() * 2 * Math.PI);
            float phi = (float) (Math.acos(2 * Math.random() - 1));
            float r = (float) (Math.random() * searchRadius);
            
            float x = (float) (r * Math.sin(phi) * Math.cos(theta));
            float y = (float) (r * Math.cos(phi)) + (isJumpPrediction ? 1.5f : 0.5f);
            float z = (float) (r * Math.sin(phi) * Math.sin(theta));
            
            Vec3d point = targetCenter.add(x, y, z);
            
            // Вычисляем метрики для этой точки
            float distance = (float) startPos.distanceTo(point);
            float suspicion = calculateSuspicion(startPos, point, targetCenter);
            float speed = calculateOptimalSpeed(distance);
            
            // Комбинированный скор: короче + быстрее + менее подозрительно
            float score = (distance * 0.4f) + (suspicion * 0.3f) + (speed * 0.3f);
            
            pathPoints.add(new PathPoint(point, score, distance, suspicion));
        }
        
        // Сортируем по скору (лучшие первые)
        pathPoints.sort((a, b) -> Float.compare(a.score, b.score));
        
        // Берём топ 100 точек и уточняем через несколько итераций
        Vec3d bestPath = pathPoints.get(0).position;
        
        for (int iteration = 0; iteration < CHAOS_ITERATIONS; iteration++) {
            List<PathPoint> refinedPoints = new ArrayList<>();
            
            // Вокруг лучшей точки генерируем новые точки с меньшим радиусом
            float refinementRadius = searchRadius / (iteration + 2);
            
            for (int i = 0; i < 500; i++) {
                float theta = (float) (Math.random() * 2 * Math.PI);
                float phi = (float) (Math.acos(2 * Math.random() - 1));
                float r = (float) (Math.random() * refinementRadius);
                
                float x = (float) (r * Math.sin(phi) * Math.cos(theta));
                float y = (float) (r * Math.cos(phi));
                float z = (float) (r * Math.sin(phi) * Math.sin(theta));
                
                Vec3d point = bestPath.add(x, y, z);
                
                float distance = (float) startPos.distanceTo(point);
                float suspicion = calculateSuspicion(startPos, point, targetCenter);
                float speed = calculateOptimalSpeed(distance);
                
                float score = (distance * 0.4f) + (suspicion * 0.3f) + (speed * 0.3f);
                refinedPoints.add(new PathPoint(point, score, distance, suspicion));
            }
            
            refinedPoints.sort((a, b) -> Float.compare(a.score, b.score));
            bestPath = refinedPoints.get(0).position;
        }
        
        return bestPath;
    }
    
    private float calculateSuspicion(Vec3d from, Vec3d to, Vec3d targetCenter) {
        // Чем более прямой путь - тем более подозрительно
        Vec3d directPath = targetCenter.subtract(from).normalize();
        Vec3d actualPath = to.subtract(from).normalize();
        
        float directness = (float) directPath.dotProduct(actualPath);
        
        // Чем более прямой путь (directness близко к 1) - тем выше suspicion
        return Math.max(0, directness);
    }
    
    private static class PathPoint {
        Vec3d position;
        float score;
        float distance;
        float suspicion;
        
        PathPoint(Vec3d position, float score, float distance, float suspicion) {
            this.position = position;
            this.score = score;
            this.distance = distance;
            this.suspicion = suspicion;
        }
    }
    
    // ===== ПРЕДИКТИВНЫЕ МЕТОДЫ =====
    
    private float calculateAcceleration(Vec3d currentVelocity) {
        if (lastTargetVelocity == Vec3d.ZERO) {
            lastTargetVelocity = currentVelocity;
            return 0f;
        }
        
        float currentMag = (float) currentVelocity.length();
        float lastMag = (float) lastTargetVelocity.length();
        float acceleration = currentMag - lastMag;
        
        return Math.max(-0.5f, Math.min(0.5f, acceleration));
    }
    
    // ===== CHAOS-BASED ANGLE COMPENSATION =====
    
    private Turns compensateSpread(Turns currentRotation, Turns targetRotation, long currentTick) {
        // Ищем угол, который выглядит "неточным" для античита,
        // но после применения естественного шума даст идеальное попадание
        
        Turns bestCompensation = currentRotation;
        float bestScore = Float.MAX_VALUE;
        
        for (int i = 0; i < SPREAD_COMPENSATION_ITERATIONS; i++) {
            // Генерируем случайный угол с смещением от целевого
            float offsetYaw = (noiseRandom.nextFloat() - 0.5f) * 3.0f; // ±1.5 градуса
            float offsetPitch = (noiseRandom.nextFloat() - 0.5f) * 2.0f; // ±1 градус
            
            Turns compensatedAngle = new Turns(
                    targetRotation.getYaw() + offsetYaw,
                    MathHelper.clamp(targetRotation.getPitch() + offsetPitch, -90F, 90F)
            );
            
            // Вычисляем сид на основе этого угла и тика
            long seed = calculateAngleSeed(compensatedAngle, currentTick);
            
            // Применяем "естественный" шум к углу
            Turns noisyAngle = applyNaturalNoise(compensatedAngle, seed);
            
            // Вычисляем насколько близко шумный угол к целевому
            float yawDiff = Math.abs(MathHelper.wrapDegrees(noisyAngle.getYaw() - targetRotation.getYaw()));
            float pitchDiff = Math.abs(noisyAngle.getPitch() - targetRotation.getPitch());
            float score = yawDiff + pitchDiff;
            
            // Также учитываем "подозрительность" - чем больше смещение, тем менее подозрительно
            float suspicionPenalty = Math.abs(offsetYaw) + Math.abs(offsetPitch);
            score += suspicionPenalty * 0.1f;
            
            if (score < bestScore) {
                bestScore = score;
                bestCompensation = compensatedAngle;
                
                if (score < 0.3f) {
                    break;
                }
            }
        }
        
        return bestCompensation;
    }
    
    private long calculateAngleSeed(Turns angle, long currentTick) {
        // Вычисляем сид на основе угла и текущего тика
        long yawBits = Float.floatToIntBits(angle.getYaw());
        long pitchBits = Float.floatToIntBits(angle.getPitch());
        
        return (yawBits ^ pitchBits) ^ currentTick;
    }
    
    private Turns applyNaturalNoise(Turns baseAngle, long seed) {
        // Применяем естественный шум, который выглядит как дрожание руки
        noiseRandom.setSeed(seed);
        
        // Используем синусоидальный шум для более органичного вида
        float noiseYaw = (float) Math.sin(seed / 100.0) * 0.5f + (noiseRandom.nextFloat() - 0.5f) * 0.3f;
        float noisePitch = (float) Math.cos(seed / 150.0) * 0.4f + (noiseRandom.nextFloat() - 0.5f) * 0.2f;
        
        return new Turns(
                baseAngle.getYaw() + noiseYaw,
                MathHelper.clamp(baseAngle.getPitch() + noisePitch, -90F, 90F)
        );
    }
    
    private void updateMomentum(float angleDifference, long currentTime) {
        if (currentTime - lastMomentumUpdate > 50) {
            targetMomentum = Math.min(1.0f, angleDifference / 60.0f);
            currentMomentum = MathHelper.lerp(0.15f, currentMomentum, targetMomentum);
            momentumInfluence = currentMomentum * (0.7f + noiseRandom.nextFloat() * 0.3f);
            lastMomentumUpdate = currentTime;
        }
    }

    private Turns smoothRotation(Turns current, Turns target, float yawSpeed, float pitchSpeed, float microYaw, float microPitch) {
        float yawDiff = MathHelper.wrapDegrees(target.getYaw() - current.getYaw());
        float pitchDiff = target.getPitch() - current.getPitch();

        float distanceFactor = Math.min(1.0F, Math.max(0.3F, (float) Math.hypot(Math.abs(yawDiff), Math.abs(pitchDiff)) / 50.0F));
        
        float adaptiveYawSpeed = yawSpeed * distanceFactor;
        float adaptivePitchSpeed = pitchSpeed * distanceFactor;

        float yawStep = Math.min(Math.abs(yawDiff), adaptiveYawSpeed);
        float pitchStep = Math.min(Math.abs(pitchDiff), adaptivePitchSpeed);

        float yawChange = MathHelper.clamp(yawDiff, -yawStep, yawStep);
        float pitchChange = MathHelper.clamp(pitchDiff, -pitchStep, pitchStep);

        return new Turns(
                current.getYaw() + yawChange + microYaw,
                MathHelper.clamp(current.getPitch() + pitchChange + microPitch, -90F, 90F)
        );
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0.16D, 0.2D, 0.15D);
    }
}
