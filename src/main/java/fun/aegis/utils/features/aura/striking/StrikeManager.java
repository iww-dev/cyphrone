package fun.aegis.utils.features.aura.striking;

import fun.aegis.features.impl.movement.ElytraTarget;
import fun.aegis.utils.display.interfaces.QuickImports;
import fun.aegis.utils.client.managers.event.types.EventType;
import fun.aegis.features.impl.combat.Aura;
import fun.aegis.features.impl.combat.TriggerBot;
import fun.aegis.utils.features.aura.warp.Turns;
import fun.aegis.utils.features.aura.utils.MathAngle;
import fun.aegis.utils.features.aura.utils.RaycastAngle;
import fun.aegis.utils.features.aura.warp.TurnsConnection;
import fun.aegis.utils.features.aura.utils.Pressing;
import fun.aegis.features.impl.movement.AutoSprint;
import fun.aegis.events.item.UsingItemEvent;
import fun.aegis.events.packet.PacketEvent;
import fun.aegis.main.listener.impl.EventListener;
import fun.aegis.utils.interactions.interact.PlayerInteractionHelper;
import fun.aegis.utils.interactions.simulate.PlayerSimulation;
import fun.aegis.utils.math.calc.Calculate;
import fun.aegis.utils.math.time.StopWatch;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.text.Text;
import fun.aegis.display.hud.Notifications;
import java.util.concurrent.ThreadLocalRandom;

@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StrikeManager implements QuickImports {
    private final StopWatch attackTimer = new StopWatch(), shieldWatch = new StopWatch(),
            sprintCooldown = new StopWatch();
    private final Pressing clickScheduler = new Pressing();
    private int count = 0;
    private boolean prevSprinting;
    
    // Дополнительные системы для обхода
    private long lastAttackTime = 0;
    private float attackVariation = 1.0f;
    private long lastVariationUpdate = 0;
    private int attackDelayTicks = 0;
    private boolean shouldDelayAttack = false;
    private long lastDelayUpdate = 0;
    private float critChanceVariation = 1.0f;
    private long lastCritVariationUpdate = 0;
    private Vec3d lastAttackPos = Vec3d.ZERO;
    private float rotationSmoothing = 0f;

    void tick() {
        // Таймеры обновляются автоматически через StopWatch
        // Не нужно вызывать update() - StopWatch сам отслеживает время
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastVariationUpdate > 150) {
            attackVariation = 0.9f + (ThreadLocalRandom.current().nextFloat() * 0.2f);
            lastVariationUpdate = currentTime;
        }
        
        // Задержка для атаки - 40% шанс, 1-2 тика
        if (currentTime - lastDelayUpdate > 100) { // Проверка быстрее (100ms вместо 200ms)
            shouldDelayAttack = ThreadLocalRandom.current().nextFloat() < 0.4f; // 40% шанс
            attackDelayTicks = ThreadLocalRandom.current().nextInt(1, 3); // 1-2 тика задержки
            lastDelayUpdate = currentTime;
        }
        
        // Обновляем crit вариативность
        if (currentTime - lastCritVariationUpdate > 180) {
            critChanceVariation = 0.85f + (ThreadLocalRandom.current().nextFloat() * 0.3f);
            lastCritVariationUpdate = currentTime;
        }
    }

    void onPacket(PacketEvent e) {
        Packet<?> packet = e.getPacket();
        if (packet instanceof HandSwingC2SPacket || packet instanceof UpdateSelectedSlotC2SPacket) {
            clickScheduler.recalculate();
        }
        
        // Отслеживаем спринт через пакеты
        if (packet instanceof ClientCommandC2SPacket cmd) {
            if (cmd.getMode() == ClientCommandC2SPacket.Mode.START_SPRINTING) {
                prevSprinting = true;
            } else if (cmd.getMode() == ClientCommandC2SPacket.Mode.STOP_SPRINTING) {
                prevSprinting = false;
            }
        }
        
        // Отслеживаем позицию атак для анализа паттернов
        if (packet instanceof PlayerMoveC2SPacket move && mc.player != null) {
            lastAttackPos = mc.player.getPos();
        }
    }

    void onUsingItem(UsingItemEvent e) {
        if (e.getType() == EventType.START && !shieldWatch.finished(50)) {
            e.cancel();
        }
    }

    private ClientCommandC2SPacket.Mode lastSprintCommand = null;
    private boolean pendingStartSprint = false;
    private boolean pendingStopSprint = false;
    private boolean didStopSprint = false;
    private boolean lastSprintState = false;
    private long lastSprintToggleTime = 0;

    void handleAttack(StrikerConstructor.AttackPerpetratorConfigurable config) {
        if (config == null || config.getTarget() == null || mc.player == null)
            return;

        long currentTime = System.currentTimeMillis();
        
        // Минимальная задержка для обхода - только иногда и только на 1 тик
        if (shouldDelayAttack && attackDelayTicks > 0) {
            attackDelayTicks--;
            return;
        }

        if (canAttack(config, 0))
            preAttackEntity(config);

        LivingEntity target = config.getTarget();
        if (target == null)
            return;

        boolean elytraMode = Aura.getInstance() != null &&
                Aura.getInstance().getTarget() != null &&
                Aura.getInstance().getTarget().isGliding() &&
                mc.player != null &&
                mc.player.isGliding();

        if (elytraMode) {
            Vec3d targetVelocity = target.getVelocity();
            if (targetVelocity == null)
                return;

            double targetSpeed = targetVelocity.horizontalLength();
            float leadTicks = 0;
            if (ElytraTarget.shouldElytraTarget && ElytraTarget.getInstance() != null) {
                leadTicks = ElytraTarget.getInstance().elytraForward.getValue();
            }

            // Улучшенный расчёт предиктивной позиции БЕЗ лишних смещений
            Vec3d predictedPos = target.getPos().add(targetVelocity.multiply(leadTicks));
            
            Box predictedBox = new Box(
                    predictedPos.x - target.getWidth() / 2,
                    predictedPos.y,
                    predictedPos.z - target.getWidth() / 2,
                    predictedPos.x + target.getWidth() / 2,
                    predictedPos.y + target.getHeight(),
                    predictedPos.z + target.getWidth() / 2);

            if (mc.player == null || mc.interactionManager == null)
                return;

            Vec3d eyePos = mc.player.getEyePos();
            Turns rotation = TurnsConnection.INSTANCE.getRotation();
            if (rotation == null) {
                return;
            }
            Vec3d lookVec = rotation.toVector();
            if (!predictedBox.raycast(eyePos, eyePos.add(lookVec.multiply(config.getMaximumRange()))).isPresent()) {
                return;
            }
        }

        // Проверяем raycast и canAttack один раз
        if (!RaycastAngle.rayTrace(config) || !canAttack(config, 0))
            return;

        String sprintMode = getSprintMode();
        if (sprintMode != null && sprintMode.equals("Legit")) {
            if (!isSprinting()) {
                attackEntity(config);
            }
        } else if (sprintMode != null && sprintMode.equals("Packet")) {
            if (mc.player != null && mc.interactionManager != null) {
                mc.player.setSprinting(false);
                mc.player.sendSprintingPacket();
            }
            attackEntity(config);
        }
    }

    private String getSprintMode() {
        if (Aura.getInstance() != null && Aura.getInstance().isState()) {
            return Aura.getInstance().getSprintReset().getSelected();
        } else if (TriggerBot.getInstance() != null && TriggerBot.getInstance().isState()) {
            return TriggerBot.getInstance().sprintReset.getSelected();
        }
        return "Legit";
    }

    void preAttackEntity(StrikerConstructor.AttackPerpetratorConfigurable config) {
        if (config.isShouldUnPressShield() && mc.player.isUsingItem()
                && mc.player.getActiveItem().getItem().equals(Items.SHIELD)) {
            mc.interactionManager.stopUsingItem(mc.player);
            shieldWatch.reset();
        }
        String sprintMode = getSprintMode();
        if (sprintMode != null && sprintMode.equals("Legit")) {
            // HolyWorld должен работать со спринтом - не отключаем его
            if (Aura.getInstance() != null && Aura.getInstance().getAimMode().isSelected("HolyWorld")) {
                return;
            }
            // Для остальных режимов отключаем спринт перед атакой
            if (mc.player.isSprinting() && getTargetDistance() <= getAttackRange()) {
                AutoSprint.tickStop = 2;
                mc.options.sprintKey.setPressed(false);
                mc.player.setSprinting(false);
                return;
            }
            return;
        }
    }

    void postAttackEntity(StrikerConstructor.AttackPerpetratorConfigurable config) {
        if (config == null || mc.player == null)
            return;
        
        // Сбрасываем таймеры после атаки
        shieldWatch.reset();
        lastAttackTime = System.currentTimeMillis();
    }

    void attackEntity(StrikerConstructor.AttackPerpetratorConfigurable config) {
        Aura aura = Aura.getInstance();
        if (aura != null && aura.isState() && aura.getAttackSetting().isSelected("Fake Lag")) {
            aura.tickStop = 1;
        }
        attack(config);
        breakShield(config);
        attackTimer.reset();
        count++;
    }

    private void breakShield(StrikerConstructor.AttackPerpetratorConfigurable config) {
        if (config == null || config.getTarget() == null || mc.player == null || mc.interactionManager == null)
            return;

        LivingEntity target = config.getTarget();
        if (!target.isUsingItem() || !target.getActiveItem().getItem().equals(Items.SHIELD))
            return;

        Turns angleToPlayer = MathAngle.fromVec3d(mc.player.getBoundingBox().getCenter().subtract(target.getEyePos()));
        boolean facingUs = Math
                .abs(TurnsConnection.computeAngleDifference(target.getYaw(), angleToPlayer.getYaw())) < 90;

        if (!config.isShouldBreakShield() || !facingUs)
            return;

        // Поиск топора в хотбаре для мгновенного переключения
        int axeSlot = -1;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() instanceof AxeItem) {
                axeSlot = i;
                break;
            }
        }

        if (axeSlot != -1) {
            int originalSlot = mc.player.getInventory().selectedSlot;

            // Быстрое переключение через пакеты
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(axeSlot));
            
            // Добавляем микро-задержку для реалистичности
            try {
                Thread.sleep(ThreadLocalRandom.current().nextLong(1, 3));
            } catch (InterruptedException e) {
                // Игнорируем
            }
            
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);
            
            // Возвращаемся на оригинальный слот
            if (originalSlot != axeSlot) {
                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(originalSlot));
            }

            if (Aura.getInstance() != null && Aura.getInstance().isState()) {
                Notifications.getInstance().addList(Text.literal("Shield broken for ").append(target.getDisplayName()),
                        2000);
            }
        }
    }

    private void attack(StrikerConstructor.AttackPerpetratorConfigurable config) {
        if (config == null || config.getTarget() == null || mc.player == null || mc.interactionManager == null)
            return;

        Aura aura = Aura.getInstance();
        if (aura != null && aura.isState()) {
            aura.reach();
        }

        float chance = Calculate.getRandom(0, 100);
        
        // Применяем вариативность к шансу попадания
        float adjustedChance = chance * critChanceVariation;
        
        boolean shouldAttack = false;
        
        if (aura != null && aura.isState()
                && aura.getAttackSetting().isSelected("Hit Chance")) {
            shouldAttack = adjustedChance < aura.getHitChance().getValue();
        } else if (TriggerBot.getInstance() != null && TriggerBot.getInstance().isState()
                && TriggerBot.getInstance().attackSetting.isSelected("Hit Chance")) {
            shouldAttack = adjustedChance < TriggerBot.getInstance().hitChance.getValue();
        } else {
            // Если Hit Chance не выбран, всегда атакуем
            shouldAttack = true;
        }
        
        if (shouldAttack) {
            mc.interactionManager.attackEntity(mc.player, config.getTarget());
            clickScheduler.recalculate();
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }

    private boolean isSprinting() {
        return EventListener.serverSprint && !mc.player.isGliding() && !mc.player.isTouchingWater();
    }

    private float getAttackRange() {
        if (Aura.getInstance() != null && Aura.getInstance().isState()) {
            return Aura.getInstance().getAttackRange().getValue() + Aura.getInstance().getReachSpoofDistance();
        } else if (TriggerBot.getInstance() != null && TriggerBot.getInstance().isState()) {
            return TriggerBot.getInstance().attackRange.getValue();
        }
        return 3.0f;
    }

    private double getTargetDistance() {
        if (Aura.getInstance() != null && Aura.getInstance().isState() && Aura.getInstance().getTarget() != null
                && mc.player != null) {
            return mc.player.distanceTo(Aura.getInstance().getTarget());
        } else if (TriggerBot.getInstance() != null && TriggerBot.getInstance().isState()
                && TriggerBot.getInstance().target != null && mc.player != null) {
            return mc.player.distanceTo(TriggerBot.getInstance().target);
        }
        return 0;
    }

    public boolean canAttack(StrikerConstructor.AttackPerpetratorConfigurable config, int ticks) {
        for (int i = 0; i <= ticks; i++) {
            if (canCrit(config, i)) {
                return true;
            }
        }
        return false;
    }

    public boolean canCrit(StrikerConstructor.AttackPerpetratorConfigurable config, int ticks) {
        if (config == null || mc.player == null)
            return false;

        if (mc.player.isUsingItem() && !mc.player.getActiveItem().getItem().equals(Items.SHIELD)
                && config.isEatAndAttack()) {
            return false;
        }

        if (!clickScheduler.isCooldownComplete(false, 1)) {
            return false;
        }

        PlayerSimulation simulated = PlayerSimulation.simulateLocalPlayer(ticks);
        boolean noRestrict = !hasMovementRestrictions(simulated);
        boolean critState = isPlayerInCriticalState(simulated, ticks);

        if (Aura.getInstance() != null && Aura.getInstance().getSmartCrits().isValue()
                && Aura.getInstance().isState()) {
            if (noRestrict) {
                return critState || simulated.onGround;
            } else {
                return true;
            }
        }
        if (TriggerBot.getInstance() != null && TriggerBot.getInstance().smartCrits.isValue()
                && TriggerBot.getInstance().isState()) {
            if (noRestrict) {
                return critState || simulated.onGround;
            } else {
                return true;
            }
        }
        if (config.isOnlyCritical() && !hasMovementRestrictions(simulated)) {
            return isPlayerInCriticalState(simulated, ticks);
        }
        return true;
    }

    private boolean hasMovementRestrictions(PlayerSimulation simulated) {
        return simulated.hasStatusEffect(StatusEffects.BLINDNESS)
                || simulated.hasStatusEffect(StatusEffects.LEVITATION)
                || PlayerInteractionHelper.isBoxInBlock(simulated.boundingBox.expand(-1e-3), Blocks.COBWEB)
                || simulated.isSubmergedInWater()
                || simulated.isInLava()
                || simulated.isClimbing()
                || !PlayerInteractionHelper.canChangeIntoPose(EntityPose.STANDING, simulated.pos)
                || simulated.player.getAbilities().flying;
    }

    private boolean isPlayerInCriticalState(PlayerSimulation simulated, int ticks) {
        // Крит в майне работает так: прыгнул, и когда почти у земли ударить
        // fallDistance > 0 означает что мы в воздухе
        // Но крит срабатывает только если мы НЕ на земле И падаем (fallDistance растёт)
        
        boolean isFalling = simulated.fallDistance > 0;
        boolean notOnGround = !simulated.onGround;
        
        // Крит срабатывает когда:
        // 1. Мы в воздухе (не на земле)
        // 2. И падаем (fallDistance > 0)
        // 3. И не слишком высоко (fallDistance < 0.5 означает что мы почти у земли)
        return notOnGround && isFalling && simulated.fallDistance < 0.5f;
    }
}
