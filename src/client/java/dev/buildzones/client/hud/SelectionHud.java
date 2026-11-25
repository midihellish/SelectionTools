package dev.buildzones.client.hud;

import dev.buildzones.client.area.AreaModeController;
import dev.buildzones.client.config.BuildZonesConfig;
import dev.buildzones.client.input.ModifierKeyState;
import dev.buildzones.client.radial.RadialOption;
import dev.buildzones.client.selection.SelectionBounds;
import dev.buildzones.client.selection.SelectionDraft;
import dev.buildzones.client.selection.SelectionState;
import dev.buildzones.client.selection.SelectionStateMachine;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

/**
 * Painel persistente com o modo atual (escolha armada, região em andamento ou modo área) e toasts
 * discretos para ações e resultados. Sempre mostra a tecla que cancela ou para o modo.
 * Não abre tela; respeita a opção de esconder o HUD do jogo.
 */
public final class SelectionHud {
    private static final int PANEL_X = 6;
    private static final int PANEL_Y = 6;
    private static final int PADDING = 4;
    private static final int LINE_HEIGHT = 11;
    private static final int PANEL_BG = 0xB0000000;
    private static final int TITLE_COLOR = 0x53DDF2;
    private static final int TEXT_COLOR = 0xE8E8E8;
    private static final int HINT_COLOR = 0xA8A8A8;
    private static final int AXIS_COLOR = 0xFFC66D;
    private static final int TOAST_BG = 0xB0000000;

    private record Toast(String text, long expiresAt) {
    }

    private record Line(String text, int color) {
    }

    private final SelectionStateMachine machine;
    private final Deque<Toast> toasts = new ArrayDeque<>();
    private AreaModeController area;
    private Supplier<RadialOption> pending = () -> null;

    public SelectionHud(SelectionStateMachine machine) {
        this.machine = machine;
    }

    /** Liga o HUD ao modo área e à escolha pendente. Feito depois da criação, por causa da ordem de montagem. */
    public void bind(AreaModeController area, Supplier<RadialOption> pending) {
        this.area = area;
        this.pending = pending;
    }

    public void register() {
        HudRenderCallback.EVENT.register(this::render);
    }

    /** Mostra um toast com a chave de tradução e argumentos opcionais. */
    public void show(String translationKey, Object... args) {
        String text = Text.translatable(translationKey, args).getString();
        toasts.addLast(new Toast(text, Util.getMeasuringTimeMs() + BuildZonesConfig.TOAST_DURATION_MS));
        while (toasts.size() > 3) {
            toasts.removeFirst();
        }
    }

    private void render(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options.hudHidden || client.world == null) {
            return;
        }
        TextRenderer font = client.textRenderer;
        renderPanel(context, font);
        renderToasts(context, font, client);
    }

    private void renderPanel(DrawContext context, TextRenderer font) {
        List<Line> lines = panelLines();
        if (lines.isEmpty()) {
            return;
        }
        int width = 0;
        for (Line line : lines) {
            width = Math.max(width, font.getWidth(line.text()));
        }
        int height = lines.size() * LINE_HEIGHT;
        context.fill(PANEL_X, PANEL_Y,
                PANEL_X + width + PADDING * 2, PANEL_Y + height + PADDING * 2, PANEL_BG);
        int y = PANEL_Y + PADDING;
        for (Line line : lines) {
            context.drawText(font, line.text(), PANEL_X + PADDING, y, line.color(), false);
            y += LINE_HEIGHT;
        }
    }

    /** Linhas do painel para o estado atual. Vazio quando não há nada a mostrar. */
    private List<Line> panelLines() {
        String modifier = ModifierKeyState.name();
        List<Line> lines = new ArrayList<>();
        RadialOption option = pending.get();
        SelectionState state = machine.state();

        if (state == SelectionState.ARMED && option != null) {
            lines.add(new Line(title(option), TITLE_COLOR));
            lines.add(new Line(Text.translatable("buildzones.hud.panel_armed", modifier).getString(), HINT_COLOR));
        } else if (state == SelectionState.WAITING_SECOND_CORNER && machine.draft() != null) {
            SelectionDraft draft = machine.draft();
            SelectionBounds b = draft.bounds();
            lines.add(new Line(option == null ? Text.translatable("buildzones.hud.title").getString() : title(option),
                    TITLE_COLOR));
            lines.add(new Line(String.format(Locale.ROOT, "%d × %d × %d   %s %s",
                    b.sizeX(), b.sizeY(), b.sizeZ(),
                    formatVolume(b.volume()),
                    Text.translatable("buildzones.hud.blocks").getString()), TEXT_COLOR));
            lines.add(new Line(Text.translatable("buildzones.hud.panel_waiting", modifier).getString(), HINT_COLOR));
        } else if (state == SelectionState.PREVIEW && machine.draft() != null) {
            SelectionDraft draft = machine.draft();
            SelectionBounds b = draft.bounds();
            lines.add(new Line(Text.translatable("buildzones.hud.title").getString()
                    + "  /  " + Text.translatable("buildzones.hud.preview_title").getString(), TITLE_COLOR));
            lines.add(new Line(String.format(Locale.ROOT, "%d × %d × %d   %s %s",
                    b.sizeX(), b.sizeY(), b.sizeZ(),
                    formatVolume(b.volume()),
                    Text.translatable("buildzones.hud.blocks").getString()), TEXT_COLOR));
            lines.add(new Line(axisText(draft), AXIS_COLOR));
            lines.add(new Line(Text.translatable("buildzones.hud.preview_help").getString(), HINT_COLOR));
        } else if (area != null && area.isActive()) {
            lines.add(new Line(Text.translatable("buildzones.hud.area_title", title(RadialOption.BUILD)).getString(),
                    TITLE_COLOR));
            if (area.target() == null) {
                lines.add(new Line(Text.translatable("buildzones.hud.area_no_block").getString(), TEXT_COLOR));
            } else {
                lines.add(new Line(Text.translatable("buildzones.hud.area_block", area.target().getBlock().getName())
                        .getString(), TEXT_COLOR));
            }
            lines.add(new Line(Text.translatable("buildzones.hud.area_help", modifier).getString(), HINT_COLOR));
        }
        return lines;
    }

    private static String axisText(SelectionDraft draft) {
        if (draft.activeAxis() == null) {
            return "";
        }
        String axis = draft.activeAxis().name();
        String hint = switch (draft.activeAxis()) {
            case Y -> Text.translatable("buildzones.hud.axis_y").getString();
            default -> Text.translatable("buildzones.hud.axis_horizontal").getString();
        };
        return Text.translatable("buildzones.hud.axis", axis).getString() + "  " + hint;
    }

    private static String title(RadialOption option) {
        return Text.translatable(option.labelKey()).getString();
    }

    private void renderToasts(DrawContext context, TextRenderer font, MinecraftClient client) {
        long now = Util.getMeasuringTimeMs();
        toasts.removeIf(t -> t.expiresAt() <= now);
        int y = client.getWindow().getScaledHeight() - 60;
        int screenWidth = client.getWindow().getScaledWidth();
        for (Toast toast : toasts) {
            int w = font.getWidth(toast.text());
            int x = (screenWidth - w) / 2;
            context.fill(x - PADDING, y - PADDING, x + w + PADDING, y + font.fontHeight + PADDING, TOAST_BG);
            context.drawText(font, toast.text(), x, y, TEXT_COLOR, false);
            y -= LINE_HEIGHT + PADDING * 2;
        }
    }

    private static String formatVolume(long volume) {
        return String.format(Locale.ROOT, "%,d", volume).replace(',', '.');
    }
}
