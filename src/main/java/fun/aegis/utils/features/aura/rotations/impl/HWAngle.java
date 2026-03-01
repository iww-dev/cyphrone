package fun.aegis.utils.features.aura.rotations.impl;

import fun.aegis.Aegis;
import fun.aegis.features.impl.combat.Aura;
import fun.aegis.utils.features.aura.rotations.constructor.RotateConstructor;
import fun.aegis.utils.features.aura.striking.StrikeManager;
import fun.aegis.utils.features.aura.warp.Turns;
import fun.aegis.utils.features.aura.utils.MathAngle;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class HWAngle extends RotateConstructor {
    private float tick = 0;
    
    // Momentum и ускорение
    private float currentMomentum = 0f;
    private float targetMomentum = 0f;
    private long lastMomentumUpdate = 0;
    private long reactionDelayEnd = 0;
    
    // Вариативность скорости
    private float speedVariation = 1.0f;
    private long lastSpeedVariationUpdate = 0;
    private float momentumInfluence = 0f;
    
    // Микро-движения
    private float microMovementPhase = 0f;
    
    // Предиктивные системы
    private Vec3d lastTargetVelocity = Vec3d.ZERO;
    private float velocityMagnitude = 0f;
    private long lastVelocityUpdate = 0;
    
    // Chaos Theory Pathfinding - ОПТИМИЗИРОВАНО ДЛЯ REACH
    private static final int CHAOS_POINTS_COUNT = 600; // Меньше точек для быстрого reach
    private static final int CHAOS_ITERATIONS = 1; // Одна итерация - максимально быстро
    private long lastChaosUpdate = 0;
    private Vec3d optimalPath = null;
    private Vec3d lastPlayerEyePos = Vec3d.ZERO;
    
    // Jump Prediction
    private boolean wasJumping = false;
    private long jumpDetectedTime = 0;
    private Vec3d jumpPredictedPos = null;
    private float lastJumpVelocity = 0f;
    private Vec3d jumpTrajectory = Vec3d.ZERO;
    
    // Spread Compensation - ОПТИМИЗИРОВАНО
    private static final int SPREAD_COMPENSATION_ITERATIONS = 25; // Вместо 100
    private long lastSpreadCompensationUpdate = 0;
    private final java.util.Random noiseRandom = new java.util.Random();
    
    // Кэширование для оптимизации
    private LivingEntity cachedTarget = null;
    private long cachedTargetTime = 0;
    
    // Дополнительные системы обхода
    private float jitterAmount = 0f;
    private long lastJitterUpdate = 0;
    private float rotationSmoothing = 0f;
    private float accelerationCurve = 0f;
    private long lastAccelerationUpdate = 0;
    
    // Обход Grim - Head Rotation Detection
    private float headRotationVariation = 0f;
    private long lastHeadRotationUpdate = 0;
    private float headRotationOffset = 0f;
    
    // Обход Watchdog - Rotation Consistency
    private float rotationConsistency = 0f;
    private long lastConsistencyUpdate = 0;
    private float consistencyVariation = 0f;
    
    // Обход Grim - Impossible Rotations
    private float impossibleRotationChance = 0f;
    private long lastImpossibleRotationUpdate = 0;
    private boolean useImpossibleRotation = false;
    
    // Обход Watchdog - Acceleration Patterns
    private float accelerationPattern = 0f;
    private long lastAccelerationPatternUpdate = 0;
    private float[] accelerationHistory = new float[10];
    private int accelerationHistoryIndex = 0;
    
    // ===== НОВЫЕ СИСТЕМЫ ОБХОДА =====
    
    // Velocity-based Prediction - предсказание движения врага
    private Vec3d predictedEnemyPos = null;
    private long lastPredictionUpdate = 0;
    private float predictionStrength = 0.3f; // 30% предсказания
    private float predictionError = 0.15f; // ±15% ошибка в предсказании
    
    // Head Targeting - целимся в голову вместо центра
    private float headTargetHeight = 0.85f; // 85% от верхней части хитбокса
    
    // Natural Jitter - микро-дрожание как у человека
    private float naturalJitterPhase = 0f;
    private long lastNaturalJitterUpdate = 0;
    
    // Reaction Time Variance - задержка перед атакой
    private long reactionTimeVariance = 0;
    private long lastReactionTimeUpdate = 0;
    
    // ===== ПРЕИМУЩЕСТВО БЕЗ ДЕТЕКЦИИ =====
    
    // Multi-point targeting - целимся в несколько точек
    private Vec3d[] targetPoints = new Vec3d[3]; // голова, туловище, ноги
    private long lastMultiPointUpdate = 0;
    
    // Strafe Prediction - предсказание стрейфа
    private Vec3d lastEnemyPos = null;
    private Vec3d enemyMovementDirection = Vec3d.ZERO;
    private long lastStrafeUpdate = 0;
    
    // Aim Assist - слабое притягивание на близкой дистанции
    private float aimAssistStrength = 0f;
    private long lastAimAssistUpdate = 0;
    private static final float AIM_ASSIST_RANGE = 5.0f; // 5 блоков
    private static final float AIM_ASSIST_PULL = 2.5f; // ±2.5 градуса

    public HWAngle() {
        super("HolyWorld");
    }

    @Override
    public Turns limitAngleChange(Turns currentAngle, Turns targetAngle, Vec3d vec3d, Entity entity) {
        if (mc.player == null) {
            return currentAngle;
        }
        
        Aura aura = Aura.getInstance();
        if (aura == null) {
            return currentAngle;
        }
        
        StrikeManager attackHandler = Aegis.getInstance().getAttackPerpetrator().getAttackHandler();
        boolean canAttack = entity != null && attackHandler.canAttack(aura.getConfig(), 10);

        long currentTime = System.currentTimeMillis();
        
        boolean attackF = false;
        if (canAttack) {
            tick = 3;
            if (reactionDelayEnd == 0) {
                // Reaction time зависит от дистанции - дальше враг, дольше реакция
                float distance = mc.player.distanceTo(entity);
                long baseReactionTime = 50 + (long)(distance * 10); // +10ms за каждый блок
                reactionTimeVariance = ThreadLocalRandom.current().nextLong(baseReactionTime, baseReactionTime + 100);
                reactionDelayEnd = currentTime + reactionTimeVariance;
            }
        }
        
        if (tick > 0) {
            attackF = true;
            tick--;
        } else {
            // Сбрасываем reactionDelayEnd когда атака заканчивается
            reactionDelayEnd = 0;
        }

        // ===== CHAOS THEORY PATHFINDING - REACH ADVANTAGE =====
        Turns finalTargetRotation = targetAngle;
        
        // Определяем фазу атаки: ДО (canAttack), ВО ВРЕМЯ (attackF), ПОСЛЕ (tick == 0 но был attackF)
        boolean isNearAttack = canAttack || attackF || (tick == 0 && reactionDelayEnd > 0);
        
        if (entity instanceof LivingEntity livingEntity) {
            if (cachedTarget != livingEntity || currentTime - cachedTargetTime > 100) {
                cachedTarget = livingEntity;
                cachedTargetTime = currentTime;
            }
            
            // Обновляем pathfinding часто ДО, ВО ВРЕМЯ и ПОСЛЕ атаки
            long updateInterval = 200; // По умолчанию редко
            if (isNearAttack) {
                updateInterval = 12; // При атаке/перед атакой - каждые 12ms для максимального reach
            }
            
            if (currentTime - lastChaosUpdate > updateInterval) {
                Vec3d playerEyePos = mc.player.getEyePos();
                
                Box targetHitbox = livingEntity.getBoundingBox();
                
                // Основной pathfinding - находит ближайшую точку на хитбоксе
                Vec3d optimalWorldPos = calculateChaosPath(playerEyePos, targetHitbox, false);
                if (optimalWorldPos != null) {
                    optimalPath = optimalWorldPos;
                }
                lastPlayerEyePos = playerEyePos;
                
                // Jump prediction - только если враг прыгает
                boolean isJumping = !livingEntity.isOnGround() && livingEntity.getVelocity().y > 0.1f;
                if (!wasJumping && isJumping) {
                    wasJumping = true;
                    jumpDetectedTime = currentTime;
                    lastJumpVelocity = (float) livingEntity.getVelocity().y;
                    jumpTrajectory = livingEntity.getVelocity();
                    Vec3d jumpPos = calculateChaosPath(playerEyePos, targetHitbox, true);
                    if (jumpPos != null) {
                        jumpPredictedPos = jumpPos;
                    }
                } else if (wasJumping && !isJumping) {
                    wasJumping = false;
                    jumpPredictedPos = null;
                }
                
                // Обновляем jump prediction при прыжке
                if (wasJumping && currentTime - jumpDetectedTime < 500) {
                    Vec3d jumpPos = calculateChaosPath(playerEyePos, targetHitbox, true);
                    if (jumpPos != null) {
                        jumpPredictedPos = jumpPos;
                    }
                }
                
                lastChaosUpdate = currentTime;
            }
            
            // Используем оптимальный путь или jump prediction
            Vec3d aimPoint = targetAngle.toVector();
            if (wasJumping && currentTime - jumpDetectedTime < 500 && jumpPredictedPos != null && jumpPredictedPos.length() > 0.1) {
                aimPoint = jumpPredictedPos;
            } else if (optimalPath != null && optimalPath.length() > 0.1) {
                // optimalPath это позиция в мире, нужно вычислить направление от глаз
                aimPoint = optimalPath;
                
                // Multi-point targeting - целимся в несколько точек и выбираем ближайшую к текущему углу
                if (currentTime - lastMultiPointUpdate > 60) {
                    Box targetHitbox = livingEntity.getBoundingBox();
                    float boxHeight = (float) (targetHitbox.maxY - targetHitbox.minY);
                    Vec3d center = targetHitbox.getCenter();
                    
                    // Голова (85%)
                    targetPoints[0] = new Vec3d(
                        center.x,
                        targetHitbox.minY + boxHeight * 0.85f,
                        center.z
                    );
                    // Туловище (50%)
                    targetPoints[1] = new Vec3d(
                        center.x,
                        targetHitbox.minY + boxHeight * 0.5f,
                        center.z
                    );
                    // Ноги (20%)
                    targetPoints[2] = new Vec3d(
                        center.x,
                        targetHitbox.minY + boxHeight * 0.2f,
                        center.z
                    );
                    
                    lastMultiPointUpdate = currentTime;
                }
                
                // Выбираем точку ближайшую к текущему углу
                Vec3d bestPoint = aimPoint;
                float bestDistance = Float.MAX_VALUE;
                for (Vec3d point : targetPoints) {
                    if (point != null) {
                        float dist = (float) currentAngle.toVector().distanceTo(point);
                        if (dist < bestDistance) {
                            bestDistance = dist;
                            bestPoint = point;
                        }
                    }
                }
                aimPoint = bestPoint;
                
                // ===== STRAFE PREDICTION =====
                // Предсказываем стрейф врага
                if (currentTime - lastStrafeUpdate > 50) {
                    if (lastEnemyPos != null) {
                        enemyMovementDirection = livingEntity.getPos().subtract(lastEnemyPos);
                        // Если враг движется, предсказываем его позицию
                        if (enemyMovementDirection.length() > 0.05) {
                            aimPoint = aimPoint.add(enemyMovementDirection.multiply(0.15)); // 15% предсказания
                        }
                    }
                    lastEnemyPos = livingEntity.getPos();
                    lastStrafeUpdate = currentTime;
                }
                
                // ===== AIM ASSIST =====
                // На близкой дистанции слегка притягиваем наведение
                float distance = (float) mc.player.distanceTo(livingEntity);
                if (distance < AIM_ASSIST_RANGE && currentTime - lastAimAssistUpdate > 40) {
                    aimAssistStrength = (1.0f - (distance / AIM_ASSIST_RANGE)) * 0.5f; // Максимум 0.5
                    lastAimAssistUpdate = currentTime;
                }
                
                // Добавляем velocity prediction - предсказываем куда будет враг
                if (currentTime - lastPredictionUpdate > 50) {
                    Vec3d enemyVelocity = livingEntity.getVelocity();
                    if (enemyVelocity.length() > 0.1) {
                        // Предсказываем позицию на 100ms вперед с ошибкой
                        float errorFactor = 1.0f + (float)(Math.random() * predictionError * 2 - predictionError);
                        Vec3d predictedPos = aimPoint.add(enemyVelocity.multiply(0.1 * predictionStrength * errorFactor));
                        aimPoint = predictedPos;
                    }
                    lastPredictionUpdate = currentTime;
                }
            }
            
            // Конвертируем позицию в мире в направление от глаз игрока
            Vec3d directionFromEyes = aimPoint.subtract(mc.player.getEyePos());
            if (directionFromEyes.length() > 0.1) {
                Turns predictedRotation = MathAngle.fromVec3d(directionFromEyes);
                
                // Применяем aim assist - слегка притягиваем к врагу
                if (aimAssistStrength > 0.01f) {
                    float assistYaw = (float) (Math.random() * AIM_ASSIST_PULL * 2 - AIM_ASSIST_PULL) * aimAssistStrength;
                    float assistPitch = (float) (Math.random() * AIM_ASSIST_PULL * 2 - AIM_ASSIST_PULL) * aimAssistStrength;
                    finalTargetRotation = new Turns(
                        predictedRotation.getYaw() + assistYaw,
                        MathHelper.clamp(predictedRotation.getPitch() + assistPitch, -90F, 90F)
                    );
                } else {
                    finalTargetRotation = predictedRotation;
                }
            }
            
            // Обновляем velocity prediction
            updateVelocityPrediction(livingEntity, currentTime);
        }

        // Вычисляем разницу углов
        float yawDiff = MathHelper.wrapDegrees(finalTargetRotation.getYaw() - currentAngle.getYaw());
        float pitchDiff = MathHelper.clamp(finalTargetRotation.getPitch() - currentAngle.getPitch(), -90F, 90F);
        float angleDifference = (float) Math.hypot(Math.abs(yawDiff), Math.abs(pitchDiff));

        // Обновляем вариативность скорости
        if (currentTime - lastSpeedVariationUpdate > ThreadLocalRandom.current().nextLong(100, 200)) {
            speedVariation = 0.75f + (ThreadLocalRandom.current().nextFloat() * 0.5f);
            lastSpeedVariationUpdate = currentTime;
        }

        // Обновляем jitter для обхода Watchdog
        if (currentTime - lastJitterUpdate > 50) {
            jitterAmount = (ThreadLocalRandom.current().nextFloat() - 0.5f) * 0.3f;
            lastJitterUpdate = currentTime;
        }

        // Обновляем acceleration curve
        if (currentTime - lastAccelerationUpdate > 75) {
            accelerationCurve = ThreadLocalRandom.current().nextFloat() * 0.5f;
            lastAccelerationUpdate = currentTime;
        }

        // ===== ОБХОД GRIM - HEAD ROTATION DETECTION =====
        if (currentTime - lastHeadRotationUpdate > 60) {
            headRotationVariation = (ThreadLocalRandom.current().nextFloat() - 0.5f) * 0.4f;
            headRotationOffset = ThreadLocalRandom.current().nextFloat() * 2.0f;
            lastHeadRotationUpdate = currentTime;
        }
        
        // ===== ОБХОД WATCHDOG - ROTATION CONSISTENCY =====
        if (currentTime - lastConsistencyUpdate > 80) {
            rotationConsistency = 0.7f + (ThreadLocalRandom.current().nextFloat() * 0.3f);
            consistencyVariation = (ThreadLocalRandom.current().nextFloat() - 0.5f) * 0.2f;
            lastConsistencyUpdate = currentTime;
        }
        
        // ===== ОБХОД GRIM - IMPOSSIBLE ROTATIONS =====
        if (currentTime - lastImpossibleRotationUpdate > 200) {
            impossibleRotationChance = ThreadLocalRandom.current().nextFloat();
            useImpossibleRotation = impossibleRotationChance < 0.15f; // 15% шанс
            lastImpossibleRotationUpdate = currentTime;
        }

        // Momentum система - ПОСЛЕ вычисления angleDifference
        updateMomentum(angleDifference, currentTime);

        // Базовые скорости
        float baseYawSpeed = 35.0f;
        float basePitchSpeed = 25.0f;
        
        // ===== ОБХОД WATCHDOG - ACCELERATION PATTERNS =====
        if (currentTime - lastAccelerationPatternUpdate > 100) {
            accelerationPattern = ThreadLocalRandom.current().nextFloat() * 0.6f;
            accelerationHistory[accelerationHistoryIndex] = baseYawSpeed;
            accelerationHistoryIndex = (accelerationHistoryIndex + 1) % 10;
            lastAccelerationPatternUpdate = currentTime;
        }

        // При атаке - snap с постепенным ускорением
        if (attackF && currentTime >= reactionDelayEnd) {
            float progress = 1.0f - (tick / 3.0f);
            float easeProgress = progress < 0.5f ? 2 * progress * progress : -1 + (4 - 2 * progress) * progress;
            
            // Ускорение зависит от momentum и скорости врага
            float velocityBoost = Math.min(velocityMagnitude * 25.0f, 35.0f);
            float accelerationBoost = 25.0f + (easeProgress * 45.0f) + (momentumInfluence * 20.0f) + velocityBoost + (accelerationCurve * 15.0f);
            
            // Применяем обход Watchdog - вариативность ускорения
            accelerationBoost += (accelerationPattern * 10.0f);
            
            baseYawSpeed = 45.0f + accelerationBoost;
            basePitchSpeed = 35.0f + (accelerationBoost * 0.8f);
            
            // Применяем обход Grim - head rotation variation
            baseYawSpeed += headRotationVariation * 5.0f;
            basePitchSpeed += (headRotationVariation * 0.7f) * 5.0f;
            
            baseYawSpeed += ThreadLocalRandom.current().nextFloat(-5, 8);
            basePitchSpeed += ThreadLocalRandom.current().nextFloat(-4, 6);
        } else if (attackF) {
            baseYawSpeed = 15.0f + (speedVariation * 4.0f);
            basePitchSpeed = 10.0f + (speedVariation * 3.0f);
        } else {
            // Адаптивная скорость с учётом скорости врага И дистанции
            float velocityFactor = Math.min(velocityMagnitude * 20.0f, 30.0f);
            float distance = (float) mc.player.distanceTo(entity);
            float distanceFactor = Math.min(distance / 10.0f, 1.0f); // Дальше враг, медленнее целимся
            
            if (angleDifference > 50) {
                baseYawSpeed = (42.0f + (speedVariation * 6.0f) + velocityFactor) * (0.7f + distanceFactor * 0.3f);
                basePitchSpeed = (32.0f + (speedVariation * 5.0f) + (velocityFactor * 0.8f)) * (0.7f + distanceFactor * 0.3f);
            } else if (angleDifference > 25) {
                baseYawSpeed = (36.0f + (speedVariation * 5.0f) + (velocityFactor * 0.8f)) * (0.75f + distanceFactor * 0.25f);
                basePitchSpeed = (28.0f + (speedVariation * 4.0f) + (velocityFactor * 0.6f)) * (0.75f + distanceFactor * 0.25f);
            } else {
                baseYawSpeed = (28.0f + (speedVariation * 4.0f) + (velocityFactor * 0.6f)) * (0.8f + distanceFactor * 0.2f);
                basePitchSpeed = (21.0f + (speedVariation * 3.0f) + (velocityFactor * 0.4f)) * (0.8f + distanceFactor * 0.2f);
            }
            
            // Применяем обход Watchdog - consistency variation
            baseYawSpeed *= rotationConsistency;
            basePitchSpeed *= rotationConsistency;
        }

        // Микро-движения (максимум 0.14 + jitter)
        microMovementPhase += 0.025f;
        float microMovementYaw = (float) (Math.sin(microMovementPhase) * 0.05f + Math.cos(microMovementPhase * 0.7f) * 0.03f) + jitterAmount * 0.5f;
        float microMovementPitch = (float) (Math.cos(microMovementPhase * 0.8f) * 0.04f + Math.sin(microMovementPhase * 1.2f) * 0.02f) + (jitterAmount * 0.3f);

        // Natural Jitter - микро-дрожание как у человека (±0.5-1 градус)
        if (currentTime - lastNaturalJitterUpdate > 40) {
            naturalJitterPhase = (float) (Math.random() * Math.PI * 2);
            lastNaturalJitterUpdate = currentTime;
        }
        float naturalJitterYaw = (float) Math.sin(naturalJitterPhase) * 0.3f; // Уменьшил с 0.7f
        float naturalJitterPitch = (float) Math.cos(naturalJitterPhase) * 0.2f; // Уменьшил с 0.5f

        // Легитные смещения
        float offsetX = (float) (Math.sin(currentTime / 1000D) * 0.5f + Math.cos(currentTime / 1500D) * 0.3f);
        float offsetY = (float) (Math.cos(currentTime / 1200D) * 0.4f + Math.sin(currentTime / 1800D) * 0.2f);

        Turns offsetAngle = new Turns(
                finalTargetRotation.getYaw() + offsetX,
                MathHelper.clamp(finalTargetRotation.getPitch() + offsetY, -90F, 90F)
        );

        Turns result = smoothRotation(currentAngle, offsetAngle, baseYawSpeed, basePitchSpeed, microMovementYaw + naturalJitterYaw, microMovementPitch + naturalJitterPitch);
        
        // ===== CHAOS-BASED SPREAD COMPENSATION - МАКСИМУМ =====
        if (entity instanceof LivingEntity && currentTime - lastSpreadCompensationUpdate > 20) {
            result = compensateSpread(result, finalTargetRotation, currentTime);
            lastSpreadCompensationUpdate = currentTime;
        }
        
        // ===== ВОЗВРАТ ПРИЦЕЛА ИГРОКУ ПОСЛЕ АТАКИ =====
        // Если атака закончилась, постепенно возвращаем прицел к текущему углу игрока
        if (!attackF && tick == 0 && reactionDelayEnd == 0) {
            // Плавно возвращаем к текущему углу камеры
            float returnSpeed = 15.0f; // Скорость возврата
            float returnYawDiff = MathHelper.wrapDegrees(currentAngle.getYaw() - result.getYaw());
            float returnPitchDiff = currentAngle.getPitch() - result.getPitch();
            
            float returnYawStep = Math.min(Math.abs(returnYawDiff), returnSpeed);
            float returnPitchStep = Math.min(Math.abs(returnPitchDiff), returnSpeed);
            
            result = new Turns(
                result.getYaw() + MathHelper.clamp(returnYawDiff, -returnYawStep, returnYawStep),
                MathHelper.clamp(result.getPitch() + MathHelper.clamp(returnPitchDiff, -returnPitchStep, returnPitchStep), -90F, 90F)
            );
        }
        
        return result;
    }
    
    private Vec3d calculateChaosPath(Vec3d startPos, Box targetHitbox, boolean isJumpPrediction) {
        if (targetHitbox == null || startPos == null) {
            return startPos != null ? startPos : Vec3d.ZERO;
        }
        
        Vec3d targetCenter = targetHitbox.getCenter();
        if (targetCenter == null) {
            return startPos;
        }
        
        // Целимся в голову (верхняя часть хитбокса) вместо центра
        Vec3d headTarget = new Vec3d(
            targetCenter.x,
            targetHitbox.minY + (targetHitbox.maxY - targetHitbox.minY) * headTargetHeight,
            targetCenter.z
        );
        
        // Генерируем точки НА ПОВЕРХНОСТИ хитбокса, не вокруг него
        List<PathPoint> pathPoints = new ArrayList<>(CHAOS_POINTS_COUNT);
        
        float boxWidth = (float) (targetHitbox.maxX - targetHitbox.minX);
        float boxHeight = (float) (targetHitbox.maxY - targetHitbox.minY);
        float boxDepth = (float) (targetHitbox.maxZ - targetHitbox.minZ);
        
        // Генерируем точки на 6 сторонах хитбокса
        for (int i = 0; i < CHAOS_POINTS_COUNT; i++) {
            Vec3d point = null;
            int side = i % 6;
            float u = (float) Math.random();
            float v = (float) Math.random();
            
            switch (side) {
                case 0: // Передняя сторона (maxZ)
                    point = new Vec3d(
                        targetHitbox.minX + u * boxWidth,
                        targetHitbox.minY + v * boxHeight,
                        targetHitbox.maxZ
                    );
                    break;
                case 1: // Задняя сторона (minZ)
                    point = new Vec3d(
                        targetHitbox.minX + u * boxWidth,
                        targetHitbox.minY + v * boxHeight,
                        targetHitbox.minZ
                    );
                    break;
                case 2: // Левая сторона (minX)
                    point = new Vec3d(
                        targetHitbox.minX,
                        targetHitbox.minY + u * boxHeight,
                        targetHitbox.minZ + v * boxDepth
                    );
                    break;
                case 3: // Правая сторона (maxX)
                    point = new Vec3d(
                        targetHitbox.maxX,
                        targetHitbox.minY + u * boxHeight,
                        targetHitbox.minZ + v * boxDepth
                    );
                    break;
                case 4: // Верх (maxY)
                    point = new Vec3d(
                        targetHitbox.minX + u * boxWidth,
                        targetHitbox.maxY,
                        targetHitbox.minZ + v * boxDepth
                    );
                    break;
                case 5: // Низ (minY)
                    point = new Vec3d(
                        targetHitbox.minX + u * boxWidth,
                        targetHitbox.minY,
                        targetHitbox.minZ + v * boxDepth
                    );
                    break;
            }
            
            if (point != null) {
                float distance = (float) startPos.distanceTo(point);
                float score = distance; // Просто расстояние - выбираем ближайшую точку
                pathPoints.add(new PathPoint(point, score));
            }
        }
        
        pathPoints.sort((a, b) -> Float.compare(a.score, b.score));
        
        if (pathPoints.isEmpty()) {
            return targetCenter;
        }
        
        // Возвращаем ближайшую точку - БЕЗ уточнения для скорости (reach advantage)
        Vec3d bestPath = pathPoints.get(0).position;
        return bestPath != null ? bestPath : targetCenter;
    }
    
    private float calculateSuspicion(Vec3d from, Vec3d to, Vec3d targetCenter) {
        Vec3d directPath = targetCenter.subtract(from);
        if (directPath.length() < 0.001) return 1.0f;
        
        directPath = directPath.normalize();
        Vec3d actualPath = to.subtract(from);
        if (actualPath.length() < 0.001) return 1.0f;
        
        actualPath = actualPath.normalize();
        float directness = (float) directPath.dotProduct(actualPath);
        
        // Чем ближе к 1.0, тем более "прямой" путь (менее подозрительный)
        // Возвращаем значение от 0 до 1, где 1 = не подозрительно
        return Math.max(0, directness);
    }
    
    private static class PathPoint {
        Vec3d position;
        float score;
        
        PathPoint(Vec3d position, float score) {
            this.position = position;
            this.score = score;
        }
    }
    
    // ===== VELOCITY PREDICTION =====
    
    private void updateVelocityPrediction(LivingEntity entity, long currentTime) {
        if (currentTime - lastVelocityUpdate > 30) {
            lastTargetVelocity = entity.getVelocity();
            velocityMagnitude = (float) lastTargetVelocity.length();
            lastVelocityUpdate = currentTime;
        }
    }
    
    private float calculateOptimalSpeed(float distance) {
        if (distance < 3) return 0.2f;
        if (distance < 8) return 0.4f;
        if (distance < 15) return 0.6f;
        if (distance < 25) return 0.75f;
        if (distance < 40) return 0.85f;
        return 0.95f;
    }
    
    // ===== CHAOS-BASED SPREAD COMPENSATION - МАКСИМУМ ИТЕРАЦИЙ =====
    
    private Turns compensateSpread(Turns currentRotation, Turns targetRotation, long currentTick) {
        Turns bestCompensation = currentRotation;
        float bestScore = Float.MAX_VALUE;
        
        for (int i = 0; i < SPREAD_COMPENSATION_ITERATIONS; i++) {
            float offsetYaw = (noiseRandom.nextFloat() - 0.5f) * 2.0f;
            float offsetPitch = (noiseRandom.nextFloat() - 0.5f) * 1.5f;
            
            // Иногда используем более экстремальные смещения
            if (ThreadLocalRandom.current().nextFloat() < 0.15f) {
                offsetYaw *= (1.2f + ThreadLocalRandom.current().nextFloat() * 0.3f);
                offsetPitch *= (1.1f + ThreadLocalRandom.current().nextFloat() * 0.2f);
            }
            
            Turns compensatedAngle = new Turns(
                    targetRotation.getYaw() + offsetYaw,
                    MathHelper.clamp(targetRotation.getPitch() + offsetPitch, -90F, 90F)
            );
            
            // Вычисляем разницу от целевого угла
            float yawDiff = Math.abs(MathHelper.wrapDegrees(compensatedAngle.getYaw() - targetRotation.getYaw()));
            float pitchDiff = Math.abs(compensatedAngle.getPitch() - targetRotation.getPitch());
            float score = yawDiff + pitchDiff;
            
            // Штраф за слишком большие смещения
            float suspicionPenalty = Math.abs(offsetYaw) * 0.3f + Math.abs(offsetPitch) * 0.3f;
            score += suspicionPenalty;
            
            if (score < bestScore) {
                bestScore = score;
                bestCompensation = compensatedAngle;
                
                // Если нашли хороший компромисс, выходим
                if (score < 1.0f) break;
            }
        }
        
        return bestCompensation;
    }
    
    private long calculateAngleSeed(Turns angle, long currentTick) {
        long yawBits = Float.floatToIntBits(angle.getYaw());
        long pitchBits = Float.floatToIntBits(angle.getPitch());
        return (yawBits ^ pitchBits) ^ currentTick ^ System.nanoTime();
    }
    
    private Turns applyNaturalNoise(Turns baseAngle, long seed) {
        noiseRandom.setSeed(seed);
        
        float noiseYaw = (float) Math.sin(seed / 50.0) * 0.35f + (noiseRandom.nextFloat() - 0.5f) * 0.25f;
        float noisePitch = (float) Math.cos(seed / 75.0) * 0.3f + (noiseRandom.nextFloat() - 0.5f) * 0.2f;
        
        return new Turns(
                baseAngle.getYaw() + noiseYaw,
                MathHelper.clamp(baseAngle.getPitch() + noisePitch, -90F, 90F)
        );
    }
    
    // ===== MOMENTUM SYSTEM =====
    
    private void updateMomentum(float angleDifference, long currentTime) {
        if (currentTime - lastMomentumUpdate > 40) {
            targetMomentum = Math.min(1.0f, angleDifference / 50.0f);
            currentMomentum = MathHelper.lerp(0.1f, currentMomentum, targetMomentum);
            momentumInfluence = currentMomentum * (0.6f + noiseRandom.nextFloat() * 0.4f);
            lastMomentumUpdate = currentTime;
        }
    }

    // ===== SMOOTH ROTATION =====

    private Turns smoothRotation(Turns current, Turns target, float yawSpeed, float pitchSpeed, float microYaw, float microPitch) {
        float yawDiff = MathHelper.wrapDegrees(target.getYaw() - current.getYaw());
        float pitchDiff = target.getPitch() - current.getPitch();

        // Гарантируем что скорости не нулевые
        if (yawSpeed <= 0) yawSpeed = 0.1f;
        if (pitchSpeed <= 0) pitchSpeed = 0.1f;

        float distanceFactor = Math.min(1.0F, Math.max(0.2F, (float) Math.hypot(Math.abs(yawDiff), Math.abs(pitchDiff)) / 40.0F));
        
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
