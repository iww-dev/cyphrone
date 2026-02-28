package fun.aegis.utils.features.aura.render;

import fun.aegis.utils.display.color.ColorAssist;
import fun.aegis.utils.display.geometry.Render3D;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class AimbotOutlineEffect {
    
    private static final float OUTLINE_WIDTH = 2.0f;
    private static final float GLOW_INTENSITY = 0.8f;
    
    /**
     * Рисует красивый аутлайн вокруг цели
     */
    public static void renderOutline(MatrixStack matrixStack, LivingEntity entity, float animationProgress) {
        if (entity == null || !entity.isAlive()) return;

        Box box = entity.getBoundingBox();
        
        // Основной аутлайн
        int outlineColor = ColorAssist.getColor(255, 100, 100, 200);
        Render3D.drawBox(box, outlineColor, OUTLINE_WIDTH);
        
        // Пульсирующий эффект
        float pulse = (float) Math.sin(System.currentTimeMillis() / 500.0) * 0.5f + 0.5f;
        int pulseColor = ColorAssist.getColor(
            (int)(255 * pulse),
            (int)(50 * pulse),
            (int)(50 * pulse),
            (int)(150 * pulse)
        );
        
        Box pulseBox = box.expand(0.1 * pulse);
        Render3D.drawBox(pulseBox, pulseColor, OUTLINE_WIDTH * 0.5f);
    }
    
    /**
     * Рисует подсветку ног цели
     */
    public static void renderLegHighlight(MatrixStack matrixStack, LivingEntity entity) {
        if (entity == null || !entity.isAlive()) return;

        Box box = entity.getBoundingBox();
        
        // Подсвечиваем нижнюю часть (ноги)
        double minX = box.minX;
        double minY = box.minY;
        double minZ = box.minZ;
        double maxX = box.maxX;
        double maxY = box.minY + (box.maxY - box.minY) * 0.3; // Нижние 30%
        double maxZ = box.maxZ;
        
        Box legBox = new Box(minX, minY, minZ, maxX, maxY, maxZ);
        
        // Яркая подсветка ног
        int legColor = ColorAssist.getColor(255, 150, 0, 255);
        Render3D.drawBox(legBox, legColor, 1.5f);
        
        // Дополнительный эффект свечения
        float glow = (float) Math.sin(System.currentTimeMillis() / 300.0) * 0.3f + 0.7f;
        int glowColor = ColorAssist.getColor(
            (int)(255 * glow),
            (int)(100 * glow),
            0,
            (int)(150 * glow)
        );
        
        Box glowBox = legBox.expand(0.05 * glow);
        Render3D.drawBox(glowBox, glowColor, 0.8f);
    }
    
    /**
     * Полный эффект для AimBot
     */
    public static void renderFullEffect(MatrixStack matrixStack, LivingEntity entity) {
        if (entity == null || !entity.isAlive()) return;

        float animProgress = (float) (System.currentTimeMillis() % 2000) / 2000.0f;
        
        renderOutline(matrixStack, entity, animProgress);
        renderLegHighlight(matrixStack, entity);
    }
}
