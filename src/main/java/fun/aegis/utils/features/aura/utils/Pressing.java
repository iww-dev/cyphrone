package fun.aegis.utils.features.aura.utils;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.item.ItemStack;
import fun.aegis.utils.display.interfaces.QuickImports;
import fun.aegis.utils.client.packet.network.Network;
import fun.aegis.Aegis;
import fun.aegis.features.impl.combat.Aura;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Pressing implements QuickImports {
    private final int[] funTimeTicks = new int[]{10, 11, 10, 13}, spookyTicks = new int[]{11, 10, 13, 10, 12, 11, 12}, defaultTicks = new int[]{10, 11};
    long lastClickTime = System.currentTimeMillis();
    private static final long MINIMUM_COOLDOWN_MS = 500;


    public boolean isCooldownComplete(boolean dynamicCooldown, int ticks) {
        boolean isMace = isHoldingMace();
        
        long requiredDelay = 500;
        boolean is18Mode = false;
        
        try {
            Aura aura = Aura.getInstance();
            
            if (aura != null && aura.isState()) {
                if (aura.getMode18().isValue()) {
                    is18Mode = true;
                    Float cpsValue = aura.getCps().getValue();
                    if (cpsValue > 0) {
                        requiredDelay = (long) (1000.0f / cpsValue);
                    }
                }
            }
        } catch (Exception e) {
            requiredDelay = 500;
        }
        
        // Проверяем минимальную задержку между кликами
        boolean minimumDelayPassed = lastClickPassed() >= requiredDelay;
        
        if (is18Mode) {
            // В режиме 1.8 используем ТОЛЬКО CPS, без проверки cooldown
            return minimumDelayPassed;
        }
        
        // В режиме 1.9+ проверяем cooldown
        boolean cooldownReady = isMace || mc.player.getAttackCooldownProgress(ticks) > 0.9F;

        return cooldownReady && minimumDelayPassed;
    }

    public boolean hasTicksElapsedSinceLastClick(int ticks) {
        return lastClickPassed() >= (ticks * 50L * (20F / Network.TPS));
    }

    public long lastClickPassed() {
        return System.currentTimeMillis() - lastClickTime;
    }

    public void recalculate() {
        lastClickTime = System.currentTimeMillis();
    }

    int tickCount() {
        int count = Aegis.getInstance().getAttackPerpetrator().getAttackHandler().getCount();
        return switch (Network.server) {

            default -> defaultTicks[count % defaultTicks.length];
        };
    }

    private boolean isHoldingMace() {
        ItemStack mainHand = mc.player.getMainHandStack();

        return mainHand.getItem().getTranslationKey().toLowerCase().contains("mace");
    }
}
