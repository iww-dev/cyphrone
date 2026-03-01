package fun.aegis.utils.features.aura.clicking;

import net.minecraft.util.math.MathHelper;

public abstract class ClickMode {
    protected String name;
    protected float cps;
    protected long lastClickTime = 0;

    public ClickMode(String name) {
        this.name = name;
    }

    public void setCps(float cps) {
        this.cps = cps;
    }

    public abstract boolean shouldClick();

    public abstract void onClick();

    public String getName() {
        return name;
    }

    protected long getDelayBetweenClicks() {
        return (long) (1000.0f / cps);
    }

    public static class NormalClick extends ClickMode {
        public NormalClick() {
            super("Normal");
        }

        @Override
        public boolean shouldClick() {
            long currentTime = System.currentTimeMillis();
            long delay = getDelayBetweenClicks();
            return currentTime - lastClickTime >= delay;
        }

        @Override
        public void onClick() {
            lastClickTime = System.currentTimeMillis();
        }
    }

    public static class DragClicking extends ClickMode {
        private float dragVariation = 0;

        public DragClicking() {
            super("Drag Clicking");
        }

        @Override
        public boolean shouldClick() {
            long currentTime = System.currentTimeMillis();
            long baseDelay = getDelayBetweenClicks();
            dragVariation = (float) (Math.sin(currentTime / 100.0) * baseDelay * 0.3f);
            long adjustedDelay = (long) (baseDelay + dragVariation);
            return currentTime - lastClickTime >= adjustedDelay;
        }

        @Override
        public void onClick() {
            lastClickTime = System.currentTimeMillis();
        }
    }

    public static class ButterflyClicking extends ClickMode {
        private boolean leftClick = true;
        private long alternateTime = 0;

        public ButterflyClicking() {
            super("Butterfly");
        }

        @Override
        public boolean shouldClick() {
            long currentTime = System.currentTimeMillis();
            long delay = getDelayBetweenClicks();
            
            if (currentTime - lastClickTime >= delay) {
                if (currentTime - alternateTime >= delay / 2) {
                    leftClick = !leftClick;
                    alternateTime = currentTime;
                }
                return true;
            }
            return false;
        }

        @Override
        public void onClick() {
            lastClickTime = System.currentTimeMillis();
        }

        public boolean isLeftClick() {
            return leftClick;
        }
    }

    public static class JitterClicking extends ClickMode {
        private float jitterAmount = 0;

        public JitterClicking() {
            super("Jitter Clicking");
        }

        @Override
        public boolean shouldClick() {
            long currentTime = System.currentTimeMillis();
            long baseDelay = getDelayBetweenClicks();
            jitterAmount = (float) (Math.random() * baseDelay * 0.4f - baseDelay * 0.2f);
            long adjustedDelay = (long) (baseDelay + jitterAmount);
            return currentTime - lastClickTime >= adjustedDelay;
        }

        @Override
        public void onClick() {
            lastClickTime = System.currentTimeMillis();
        }
    }
}
