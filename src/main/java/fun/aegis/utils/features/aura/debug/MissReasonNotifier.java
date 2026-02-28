package fun.aegis.utils.features.aura.debug;

import fun.aegis.display.hud.Notifications;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import java.util.*;

public class MissReasonNotifier {
    
    public enum MissReason {
        // Основные причины
        LOW_HIT_CHANCE("HitChance", 1.0f, "probability was too low"),
        ANTICHEAT_FLAG("AntiCheat", 2.0f, "server detected suspicious rotation"),
        HIGH_PING("Ping", 1.5f, "your latency is too high or your luck..."),
        WRONG_ROTATION("Rotation", 1.8f, "rotation was not accurate enough"),
        WALL_BLOCKED("Wall", 2.5f, "target is behind a wall or obstacle"),
        OUT_OF_RANGE("Range", 2.0f, "target is too far away"),
        PLAYER_MOVED("Movement", 1.3f, "target moved too fast"),
        SHIELD_ACTIVE("Shield", 2.2f, "target blocked with shield"),
        
        // Дополнительные причины
        ROTATION_SPEED_TOO_HIGH("RotationSpeed", 1.9f, "rotation was too fast (suspicious)"),
        ROTATION_SPEED_TOO_LOW("RotationSpeed", 1.4f, "rotation was too slow (maybe lag)"),
        PACKET_LOSS("PacketLoss", 1.7f, "packets were lost during transmission."),
        SERVER_TICK_RATE("TickRate", 1.6f, "server tick rate mismatch"),
        PLAYER_SPRINTING("Sprint", 1.2f, "target is sprinting (harder to hit)"),
        PLAYER_JUMPING("Jump", 1.4f, "target is jumping (unpredictable)"),
        PLAYER_SNEAKING("Sneak", 1.1f, "target is sneaking (hitbox)"),
        PLAYER_FLYING("Flight", 2.3f, "target is flying (out of reach)"),
        PLAYER_GLIDING("umtrj", 2.1f, "(unpredictable trajectory)"),
        PLAYER_IN_WATER("Water", 1.5f, "target is in water (movement)"),
        PLAYER_IN_LAVA("Lava", 1.6f, "target is in lava (movement)"),
        PLAYER_CLIMBING("Climb", 1.3f, "target is climbing (hitbox)"),
        PLAYER_INVISIBLE("Invisible", 2.4f, "target has invisibility"),
        PLAYER_INVULNERABLE("Invulnerable", 2.5f, "target is invulnerable"),
        PLAYER_DEAD("Dead", 2.5f, "target died before hitreg"),
        
        // Ротация и предсказание
        PREDICTION_FAILED("Prediction", 1.8f, "failed to predict target pos"),
        ROTATION_JITTER("Jitter", 1.5f, "rotation had too much jitter"),
        ROTATION_DESYNC("Desync", 1.9f, "client-server rotation mismatch"),
        
        // Сетевые проблемы
        NETWORK_LATENCY("Latency", 1.6f, "network latency caused desync"),
        NETWORK_TIMEOUT("Timeout", 2.0f, "network timeout occurred"),
        NETWORK_CONGESTION("Congestion", 1.7f, "network is congested"),
        
        // Механика игры
        CRITICAL_HIT_FAILED("CriticalHit", 1.4f, "critical hit requirements not met"),
        KNOCKBACK_RESISTANCE("Resistance", 1.8f, "target has knockback resistance"),
        ARMOR_PROTECTION("Armor", 1.6f, "target armor is too strong"),
        ENCHANTMENT_PROTECTION("Enchant", 1.7f, "target has protection enchantment"),
        
        // Другие причины
        BLOCK_COLLISION("Collision", 1.5f, "hit block instead of entity"),
        ENTITY_DESPAWNED("Despawn", 2.3f, "target despawned before hitreg (maybe dead)"),
        ENTITY_TELEPORTED("Teleport", 2.4f, "target teleported away (maybe dead or admin tphere)"),
        ENTITY_MOUNTED("Mount", 1.9f, "target is mounted on entity (bp)"),
        ENTITY_PASSENGER("Passenger", 1.8f, "target is passenger in vehicle (vehmiss)"),
        
        // Ошибки клиента
        CLIENT_TICK_SKIP("ClientTick", 1.7f, "client tick was skipped (ctskip)"),
        CHUNK_UNLOADED("Chunk", 2.2f, "target chunk is unloaded (tarchunk)"),
        
        UNKNOWN("Unknown", 1.0f, "something went wrong (enemy is nigger?)");

        private final String shortName;
        private final float weight;
        private final String description;

        MissReason(String shortName, float weight, String description) {
            this.shortName = shortName;
            this.weight = weight;
            this.description = description;
        }

        public String getShortName() {
            return shortName;
        }

        public float getWeight() {
            return weight;
        }

        public String getDescription() {
            return description;
        }

        public String getMessage() {
            return "Missed due to ^" + shortName + "^ - " + description;
        }
    }

    /**
     * Отправляет уведомление в чат и HUD
     */
    public static void notifyMiss(MissReason reason) {
        if (reason == null) return;

        // HUD уведомление
        if (Notifications.getInstance() != null) {
            Text text = Text.literal(reason.getMessage())
                    .formatted(Formatting.RED);
            Notifications.getInstance().addList(text, 4000);
        }

        // Чат уведомление
        if (net.minecraft.client.MinecraftClient.getInstance().player != null) {
            Text chatText = Text.literal("[Aura] " + reason.getMessage())
                    .formatted(Formatting.RED);
            net.minecraft.client.MinecraftClient.getInstance().player.sendMessage(chatText, false);
        }
    }

    /**
     * Отправляет несколько причин сразу
     */
    public static void notifyMultipleMisses(List<MissReason> reasons) {
        if (reasons == null || reasons.isEmpty()) return;

        // Сортируем по весу (важности)
        reasons.sort((a, b) -> Float.compare(b.getWeight(), a.getWeight()));

        // Берем топ 3 причины
        List<MissReason> topReasons = reasons.subList(0, Math.min(3, reasons.size()));

        StringBuilder sb = new StringBuilder();
        sb.append("Missed due to: ");
        for (int i = 0; i < topReasons.size(); i++) {
            sb.append("^").append(topReasons.get(i).getShortName()).append("^");
            if (i < topReasons.size() - 1) {
                sb.append(" + ");
            }
        }

        String message = sb.toString();

        // HUD уведомление
        if (Notifications.getInstance() != null) {
            Text text = Text.literal(message)
                    .formatted(Formatting.RED);
            Notifications.getInstance().addList(text, 4000);
        }

        // Чат уведомление
        if (net.minecraft.client.MinecraftClient.getInstance().player != null) {
            Text chatText = Text.literal("[Aura] " + message)
                    .formatted(Formatting.RED);
            net.minecraft.client.MinecraftClient.getInstance().player.sendMessage(chatText, false);
        }
    }

    /**
     * Умный анализ с несколькими причинами
     */
    public static List<MissReason> analyzeSmartMiss(
            float hitChance, 
            int ping, 
            boolean wallBlocked, 
            boolean outOfRange, 
            float targetDistance, 
            float targetVelocity, 
            boolean shieldActive,
            boolean targetSprinting,
            boolean targetJumping,
            boolean targetSneaking,
            boolean targetFlying,
            boolean targetGliding,
            boolean targetInWater,
            boolean targetInLava,
            boolean targetClimbing,
            boolean targetInvisible,
            boolean targetInvulnerable,
            boolean targetDead,
            float rotationSpeed,
            float rotationAccuracy,
            int packetLoss,
            boolean rotationDesync,
            boolean predictionFailed,
            int serverTickRate,
            boolean blockCollision,
            boolean entityDespawned,
            boolean entityTeleported,
            boolean entityMounted
    ) {
        
        List<MissReason> reasons = new ArrayList<>();

        // Критические состояния (всегда добавляем)
        if (targetDead) {
            reasons.add(MissReason.PLAYER_DEAD);
            return reasons;
        }
        
        if (entityDespawned) {
            reasons.add(MissReason.ENTITY_DESPAWNED);
            return reasons;
        }
        
        if (entityTeleported) {
            reasons.add(MissReason.ENTITY_TELEPORTED);
            return reasons;
        }
        
        if (targetInvisible) {
            reasons.add(MissReason.PLAYER_INVISIBLE);
            return reasons;
        }
        
        if (targetInvulnerable) {
            reasons.add(MissReason.PLAYER_INVULNERABLE);
            return reasons;
        }

        // Физические препятствия
        if (wallBlocked) {
            reasons.add(MissReason.WALL_BLOCKED);
        }
        
        if (blockCollision) {
            reasons.add(MissReason.BLOCK_COLLISION);
        }
        
        if (outOfRange) {
            reasons.add(MissReason.OUT_OF_RANGE);
        }

        // Защита цели
        if (shieldActive) {
            reasons.add(MissReason.SHIELD_ACTIVE);
        }

        // Состояние цели (может быть несколько)
        if (targetFlying) {
            reasons.add(MissReason.PLAYER_FLYING);
        }
        
        if (targetGliding) {
            reasons.add(MissReason.PLAYER_GLIDING);
        }
        
        if (targetInWater) {
            reasons.add(MissReason.PLAYER_IN_WATER);
        }
        
        if (targetInLava) {
            reasons.add(MissReason.PLAYER_IN_LAVA);
        }
        
        if (targetClimbing) {
            reasons.add(MissReason.PLAYER_CLIMBING);
        }
        
        if (entityMounted) {
            reasons.add(MissReason.ENTITY_MOUNTED);
        }

        // Движение цели (может быть несколько)
        if (targetJumping) {
            reasons.add(MissReason.PLAYER_JUMPING);
        }
        
        if (targetSprinting && targetVelocity > 0.8f) {
            reasons.add(MissReason.PLAYER_SPRINTING);
        }
        
        if (targetSneaking) {
            reasons.add(MissReason.PLAYER_SNEAKING);
        }
        
        if (targetVelocity > 1.0f) {
            reasons.add(MissReason.PLAYER_MOVED);
        }

        // Проблемы с ротацией (может быть несколько)
        if (rotationDesync) {
            reasons.add(MissReason.ROTATION_DESYNC);
        }
        
        if (predictionFailed) {
            reasons.add(MissReason.PREDICTION_FAILED);
        }
        
        if (rotationSpeed > 180.0f) {
            reasons.add(MissReason.ROTATION_SPEED_TOO_HIGH);
        }
        
        if (rotationSpeed < 0.5f && rotationAccuracy < 0.8f) {
            reasons.add(MissReason.ROTATION_SPEED_TOO_LOW);
        }
        
        if (rotationAccuracy < 0.5f) {
            reasons.add(MissReason.WRONG_ROTATION);
        }

        // Сетевые проблемы (может быть несколько)
        if (ping > 300) {
            reasons.add(MissReason.NETWORK_LATENCY);
        }
        
        if (ping > 200) {
            reasons.add(MissReason.HIGH_PING);
        }
        
        if (packetLoss > 5) {
            reasons.add(MissReason.PACKET_LOSS);
        }
        
        if (serverTickRate < 15) {
            reasons.add(MissReason.SERVER_TICK_RATE);
        }

        // Хитчанс
        if (hitChance < 30) {
            reasons.add(MissReason.LOW_HIT_CHANCE);
        }

        // Если нет причин, добавляем неизвестную
        if (reasons.isEmpty()) {
            reasons.add(MissReason.UNKNOWN);
        }

        return reasons;
    }

    /**
     * Упрощенный анализ для быстрого использования
     */
    public static MissReason analyzeSimpleMiss(
            float hitChance, 
            int ping, 
            boolean wallBlocked, 
            boolean outOfRange, 
            float targetDistance, 
            float targetVelocity, 
            boolean shieldActive
    ) {
        
        if (shieldActive) {
            return MissReason.SHIELD_ACTIVE;
        }
        
        if (outOfRange) {
            return MissReason.OUT_OF_RANGE;
        }
        
        if (wallBlocked) {
            return MissReason.WALL_BLOCKED;
        }
        
        if (ping > 200) {
            return MissReason.HIGH_PING;
        }
        
        if (targetVelocity > 0.5f) {
            return MissReason.PLAYER_MOVED;
        }
        
        if (hitChance < 50) {
            return MissReason.LOW_HIT_CHANCE;
        }
        
        if (ping > 100) {
            return MissReason.ANTICHEAT_FLAG;
        }
        
        return MissReason.WRONG_ROTATION;
    }
}
