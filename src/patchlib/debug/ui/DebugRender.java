package patchlib.debug.ui;

import org.lwjgl.opengl.GL11;

import java.awt.Color;

/** Immediate-mode GL helpers for the debug menu chrome. Origin is bottom-left, y up, matching event
 * and position coordinates. Callers fold the panel alphaMult into the alpha they pass. */
public final class DebugRender {

    private DebugRender() { }

    private static void setColor(Color c, float alpha) {
        float a = Math.max(0f, Math.min(1f, alpha));
        GL11.glColor4ub((byte) c.getRed(), (byte) c.getGreen(), (byte) c.getBlue(), (byte) (255 * a));
    }

    /** Fills a rectangle with a flat color. */
    public static void fillRect(float x, float y, float w, float h, Color color, float alpha) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        setColor(color, alpha);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x, y + h);
        GL11.glVertex2f(x + w, y + h);
        GL11.glVertex2f(x + w, y);
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    /** Full rectangle outline. */
    public static void rectBorder(float x, float y, float w, float h, Color color, float alpha) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glLineWidth(1f);
        setColor(color, alpha);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x + w, y);
        GL11.glVertex2f(x + w, y + h);
        GL11.glVertex2f(x, y + h);
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    /** Small filled triangle centered at (cx, cy): points right when collapsed, down when expanded. */
    public static void triangle(float cx, float cy, float size, boolean expanded, Color color, float alpha) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        setColor(color, alpha);
        GL11.glBegin(GL11.GL_TRIANGLES);
        if (expanded) {
            GL11.glVertex2f(cx - size, cy + size * 0.5f);
            GL11.glVertex2f(cx + size, cy + size * 0.5f);
            GL11.glVertex2f(cx, cy - size * 0.6f);
        } else {
            GL11.glVertex2f(cx - size * 0.5f, cy + size);
            GL11.glVertex2f(cx - size * 0.5f, cy - size);
            GL11.glVertex2f(cx + size * 0.6f, cy);
        }
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }
}
