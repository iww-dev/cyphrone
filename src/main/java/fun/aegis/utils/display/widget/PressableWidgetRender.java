package fun.aegis.utils.display.widget;

import fun.aegis.common.animation.Animation;
import fun.aegis.common.animation.implement.Decelerate;
import fun.aegis.utils.display.font.Fonts;
import fun.aegis.utils.display.interfaces.QuickImports;
import fun.aegis.utils.display.shape.ShapeProperties;
import fun.aegis.utils.display.systemrender.builders.Builder;
import net.minecraft.client.gui.DrawContext;

import java.awt.Color;

public class PressableWidgetRender implements QuickImports {

    private final Animation animation = new Decelerate().setMs(200).setValue(8);

    public static void render(DrawContext context, int x, int y, int width, int height, boolean active, String text) {

        ShapeProperties animRect = ShapeProperties.create(
                        context.getMatrices(),
                        x + (width - width ) / 2f,
                        y + (height - height ) / 2f,
                        width ,
                        height
                )
                .round(4f)
                .thickness(3)
                .outlineColor(new Color(150,150,150,180).getRGB())
                .color( new Color(70, 70, 70, 140).getRGB(),
                        new Color(20, 20, 25, 140).getRGB(),
                        new Color(20, 20, 25, 140).getRGB(),
                        new Color(70, 70, 70, 140).getRGB())
                .build();
        rectangle.render(animRect);

        if (text != null && !text.isEmpty()) {
            float textWidth = Fonts.getSize(18, Fonts.Type.DEFAULT).getStringWidth(text);
            // Если текст слишком длинный, уменьшаем размер шрифта
            if (textWidth > width - 10) {
                Fonts.getSize(14, Fonts.Type.DEFAULT)
                        .drawString(context.getMatrices(),
                                text,
                                x + width / 2f - Fonts.getSize(14, Fonts.Type.DEFAULT).getStringWidth(text) / 2f,
                                y + 8f,
                                new Color(240, 240, 240, 255).getRGB());
            } else {
                Fonts.getSize(18, Fonts.Type.DEFAULT)
                        .drawString(context.getMatrices(),
                                text,
                                x + width / 2f - textWidth / 2f,
                                y + 7f,
                                new Color(240, 240, 240, 255).getRGB());
            }
        }
    }
}
