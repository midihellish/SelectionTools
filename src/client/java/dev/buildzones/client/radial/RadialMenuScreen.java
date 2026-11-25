package dev.buildzones.client.radial;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.buildzones.client.input.ModifierKeyState;
import java.util.function.Consumer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.text.Text;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

/**
 * Menu radial aberto pelo toque na tecla de modificador. Solta o cursor para o mouse escolher,
 * e também aceita teclado: 1 a 4 escolhem direto; setas movem o foco; Enter confirma.
 * Toda opção é escolhível; o que cada uma faz depende do que o jogador tem na mão e do modo.
 */
public final class RadialMenuScreen extends Screen {
    private static final RadialOption[] OPTIONS = RadialOption.values();
    private static final int INNER = 20;
    private static final int OUTER = 56;
    /** Raio em que cada ícone é centrado (meio do anel). */
    private static final double ICON_RADIUS = (INNER + OUTER) / 2.0;
    /** Meia-abertura de cada fatia, em graus. Deixa uma folga de 2 graus entre fatias. */
    private static final double HALF_SLICE = 43.0;
    private static final int ARC_STEPS = 12;
    /** Escala dos textos do menu: menores que o texto padrão do jogo. */
    private static final float TEXT_SCALE = 0.7F;
    private static final float HINT_SCALE = 0.75F;

    private static final int OVERLAY = 0x90000000;
    private static final int SLICE = 0xB0202830;
    private static final int SLICE_FOCUS = 0xE0246F8C;
    private static final int ICON = 0xFFD8D8D8;
    private static final int ICON_FOCUS = 0xFF7FF0FF;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int TEXT_HINT = 0xFFB8B8B8;
    private static final int TOOLTIP_BG = 0xE0101418;
    private static final int TOOLTIP_PAD = 4;

    private final Consumer<RadialOption> onPick;
    private int focus;

    public RadialMenuScreen(Consumer<RadialOption> onPick) {
        super(Text.translatable("buildzones.radial.title"));
        this.onPick = onPick;
    }

    /** Sem pausa: o menu é curto e o jogo continua rodando por baixo. */
    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int cx = width / 2;
        int cy = height / 2;
        context.fill(0, 0, width, height, OVERLAY);

        int hover = RadialLayout.sliceAt(mouseX - cx, mouseY - cy, INNER);
        if (hover >= 0) {
            focus = hover;
        }
        for (int i = 0; i < RadialLayout.SLICES; i++) {
            drawWedge(context, cx, cy, i, i == focus ? SLICE_FOCUS : SLICE);
        }

        // Só o ícone aparece no anel. O nome e a descrição vêm no tooltip, ao passar por cima.
        for (int i = 0; i < RadialLayout.SLICES; i++) {
            double rad = Math.toRadians(RadialLayout.centerDegrees(i));
            int ix = cx + (int) Math.round(Math.cos(rad) * ICON_RADIUS);
            int iy = cy + (int) Math.round(Math.sin(rad) * ICON_RADIUS);
            RadialIcons.draw(context, OPTIONS[i], ix, iy, i == focus ? ICON_FOCUS : ICON);
        }

        // Tooltip: perto do cursor quando o mouse está sobre uma fatia; junto da fatia com foco no teclado.
        RadialOption option = OPTIONS[focus];
        int tx;
        int ty;
        if (hover >= 0) {
            tx = mouseX + 10;
            ty = mouseY + 10;
        } else {
            double rad = Math.toRadians(RadialLayout.centerDegrees(focus));
            tx = cx + (int) Math.round(Math.cos(rad) * (OUTER + 10));
            ty = cy + (int) Math.round(Math.sin(rad) * (OUTER + 10));
        }
        drawTooltip(context, Text.translatable(option.labelKey()).getString(),
                Text.translatable(option.descriptionKey()).getString(), tx, ty);

        smallText(context, Text.translatable("buildzones.radial.hint", ModifierKeyState.name()).getString(),
                cx, height - 14, TEXT_HINT, TEXT_SCALE);
        super.render(context, mouseX, mouseY, delta);
    }

    /** Caixa de tooltip com o nome em destaque e a descrição embaixo. Fica dentro da tela. */
    private void drawTooltip(DrawContext context, String title, String description, int x, int y) {
        float titleWidth = textRenderer.getWidth(title) * HINT_SCALE;
        float descWidth = textRenderer.getWidth(description) * TEXT_SCALE;
        int w = (int) Math.ceil(Math.max(titleWidth, descWidth)) + TOOLTIP_PAD * 2;
        int h = 22;
        int left = Math.max(2, Math.min(x, width - w - 2));
        int top = Math.max(2, Math.min(y, height - h - 2));
        context.fill(left, top, left + w, top + h, TOOLTIP_BG);
        smallText(context, title, left + w / 2, top + 6, TEXT, HINT_SCALE);
        smallText(context, description, left + w / 2, top + 15, TEXT_HINT, TEXT_SCALE);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        double dx = mouseX - width / 2.0;
        double dy = mouseY - height / 2.0;
        int slice = RadialLayout.sliceAt(dx, dy, INNER);
        if (slice < 0 || Math.hypot(dx, dy) > OUTER) {
            close();
            return true;
        }
        focus = slice;
        pick();
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || ModifierKeyState.matches(keyCode, scanCode)) {
            close();
            return true;
        }
        if (keyCode >= GLFW.GLFW_KEY_1 && keyCode < GLFW.GLFW_KEY_1 + RadialLayout.SLICES) {
            focus = keyCode - GLFW.GLFW_KEY_1;
            pick();
            return true;
        }
        switch (keyCode) {
            case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_DOWN -> focus = (focus + 1) % RadialLayout.SLICES;
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_UP -> focus = (focus + RadialLayout.SLICES - 1) % RadialLayout.SLICES;
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_SPACE -> pick();
            default -> {
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
        }
        return true;
    }

    /** Fecha e entrega a opção em foco. */
    private void pick() {
        close();
        onPick.accept(OPTIONS[focus]);
    }

    /** Texto reduzido, centrado em (x, y). */
    private void smallText(DrawContext context, String text, int x, int y, int color, float scale) {
        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(scale, scale, 1.0F);
        context.drawCenteredTextWithShadow(textRenderer, text, 0, 0, color);
        context.getMatrices().pop();
    }

    /** Setor anelar da fatia, desenhado como triângulos com cor sólida. */
    private static void drawWedge(DrawContext context, int cx, int cy, int slice, int argb) {
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        double center = RadialLayout.centerDegrees(slice);
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        for (int s = 0; s < ARC_STEPS; s++) {
            double t0 = Math.toRadians(center - HALF_SLICE + 2 * HALF_SLICE * s / ARC_STEPS);
            double t1 = Math.toRadians(center - HALF_SLICE + 2 * HALF_SLICE * (s + 1) / ARC_STEPS);
            double c0 = Math.cos(t0);
            double s0 = Math.sin(t0);
            double c1 = Math.cos(t1);
            double s1 = Math.sin(t1);
            vertex(buffer, matrix, cx + c0 * INNER, cy + s0 * INNER, r, g, b, a);
            vertex(buffer, matrix, cx + c0 * OUTER, cy + s0 * OUTER, r, g, b, a);
            vertex(buffer, matrix, cx + c1 * OUTER, cy + s1 * OUTER, r, g, b, a);
            vertex(buffer, matrix, cx + c0 * INNER, cy + s0 * INNER, r, g, b, a);
            vertex(buffer, matrix, cx + c1 * OUTER, cy + s1 * OUTER, r, g, b, a);
            vertex(buffer, matrix, cx + c1 * INNER, cy + s1 * INNER, r, g, b, a);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, double x, double y, int r, int g, int b, int a) {
        buffer.vertex(matrix, (float) x, (float) y, 0.0F).color(r, g, b, a).next();
    }
}
