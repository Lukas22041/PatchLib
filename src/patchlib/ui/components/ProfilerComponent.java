package patchlib.ui.components;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.BaseTooltipCreator;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.opengl.GL11;
import patchlib.agent.log.PatchLibLogger;
import patchlib.agent.profiler.JfrProfiler;
import patchlib.ui.elements.BoxElement;
import patchlib.ui.elements.ButtonElement;

import java.awt.Color;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Controls for recording the game to a JFR file, which can be shared to look in to performance issues. */
public class ProfilerComponent extends BaseCustomUIPanelPlugin {

    private static final String PATCHLIB_ID = "patchlib";
    private static final String DESCRIPTION = "Starts a profiling recording with Java Flight Recorder at a sample rate of 2 ms. A recording can be useful for finding the cause of performance issues within mods. First start a recording, then try to replicate the situation in which the performance issue occurs. Afterwards, open this menu again and stop the recording.\n\n"
            + "The output .jfr file can be analysed in programs like JDK Mission Control or VisualVM.";
    private static final String[] DESCRIPTION_HIGHLIGHTS = { "Java Flight Recorder", "2 ms", ".jfr", "JDK Mission Control", "VisualVM" };
    private static final String SIZE_TOOLTIP = "The recording file will not grow past %s. Recording continues once that size is reached, but the oldest data is dropped to make room, so the file holds the most recent part of the recording.";

    private static final int[] SIZES_MB = { 16, 32, 64, 128 };
    private static final long BYTES_PER_MB = 1024L * 1024L;

    private static final float TAB_WIDTH = 130f;
    private static final float TAB_HEIGHT = 18f;
    private static final float BOX_PAD = 10f;
    private static final float ROW_PAD = 8f;
    //Odd, as the button font has an odd line height, which can not be centered in an even height.
    private static final float BUTTON_HEIGHT = 25f;
    private static final float BUTTON_PAD = 3f;
    private static final float SIZE_BUTTON_WIDTH = 75f;
    private static final float CONTROLS_WIDTH = (SIZE_BUTTON_WIDTH + BUTTON_PAD) * SIZES_MB.length - BUTTON_PAD;
    private static final float BUTTON_WIDTH = (CONTROLS_WIDTH - BUTTON_PAD) / 2f;
    private static final float HEADING_HEIGHT = 18f;
    private static final float HEADING_OVERHANG = 10f;
    private static final float TEXT_HEIGHT = 16f;
    private static final float CONTROLS_HEIGHT = BUTTON_HEIGHT + ROW_PAD + HEADING_HEIGHT + BUTTON_PAD + BUTTON_HEIGHT + ROW_PAD + TEXT_HEIGHT;
    private static final float TOOLTIP_WIDTH = 400f;

    private static int maxSizeMb = 32;
    private static String result = "Not recording";
    private static Color resultColor = Misc.getGrayColor();

    private final Map<Integer, ButtonElement> sizeButtons = new LinkedHashMap<>();
    private final CustomPanelAPI panel;
    private final ButtonElement recordButton;
    private final LabelAPI statusLabel;
    private final Path outputFolder;
    private final float separatorX;

    private PositionAPI position;
    private long lastElapsedSeconds = -1;

    public ProfilerComponent(UIPanelAPI parent, float width) {
        float controlsX = width - BOX_PAD - CONTROLS_WIDTH;
        separatorX = controlsX - BOX_PAD;
        float textWidth = separatorX - BOX_PAD * 2f;

        LabelAPI description = Global.getSettings().createLabel(DESCRIPTION, Fonts.DEFAULT_SMALL);
        description.setColor(Misc.getTextColor());
        description.setHighlightColor(Misc.getHighlightColor());
        description.setHighlight(DESCRIPTION_HIGHLIGHTS);
        float height = TAB_HEIGHT + BOX_PAD + Math.max(description.autoSizeToWidth(textWidth).getHeight(), CONTROLS_HEIGHT) + BOX_PAD;

        panel = Global.getSettings().createCustom(width, height, this);
        parent.addComponent(panel);

        outputFolder = Path.of(Global.getSettings().getModManager().getModSpec(PATCHLIB_ID).getPath(), "debug", "profiles");

        ButtonElement tab = new ButtonElement(panel, "Profiler", TAB_WIDTH, TAB_HEIGHT, CutStyle.TOP);
        tab.getPosition().inTL(0f, 0f);
        tab.setHighlighted(true);

        new BoxElement(panel, width, height - TAB_HEIGHT).getPosition().inTL(0f, TAB_HEIGHT);

        panel.addComponent((UIComponentAPI) description).inTL(BOX_PAD, TAB_HEIGHT + BOX_PAD);

        float y = TAB_HEIGHT + BOX_PAD;

        recordButton = new ButtonElement(panel, "", BUTTON_WIDTH, BUTTON_HEIGHT, CutStyle.NONE);
        recordButton.getPosition().inTL(controlsX, y);
        recordButton.onPressed(this::toggleRecording);

        ButtonElement folderButton = new ButtonElement(panel, "Open Profiles Folder", BUTTON_WIDTH, BUTTON_HEIGHT, CutStyle.NONE);
        folderButton.getPosition().inTL(controlsX + BUTTON_WIDTH + BUTTON_PAD, y);
        folderButton.onPressed(this::openFolder);

        y += BUTTON_HEIGHT + ROW_PAD;

        TooltipMakerAPI headingElement = panel.createUIElement(CONTROLS_WIDTH, HEADING_HEIGHT, false);
        headingElement.addSectionHeading("Max File Size", Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Alignment.MID, CONTROLS_WIDTH - HEADING_OVERHANG, 0f);
        panel.addUIElement(headingElement).inTL(controlsX, y);

        y += HEADING_HEIGHT + BUTTON_PAD;

        float sizeX = controlsX;
        for (int size : SIZES_MB) {
            ButtonElement button = new ButtonElement(panel, size + " MB", SIZE_BUTTON_WIDTH, BUTTON_HEIGHT, CutStyle.NONE);
            button.getPosition().inTL(sizeX, y);
            button.onPressed(() -> selectSize(size));
            button.addTooltip(createSizeTooltip(size), TooltipMakerAPI.TooltipLocation.BELOW);
            sizeButtons.put(size, button);
            sizeX += SIZE_BUTTON_WIDTH + BUTTON_PAD;
        }

        y += BUTTON_HEIGHT + ROW_PAD;

        statusLabel = Global.getSettings().createLabel("", Fonts.DEFAULT_SMALL);
        statusLabel.getPosition().setSize(CONTROLS_WIDTH, TEXT_HEIGHT);
        panel.addComponent((UIComponentAPI) statusLabel).inTL(controlsX, y);

        updateSizeButtons();
        updateRecordingState();
    }

    public PositionAPI getPosition() {
        return panel.getPosition();
    }

    private static TooltipMakerAPI.TooltipCreator createSizeTooltip(int size) {
        return new BaseTooltipCreator() {
            @Override
            public float getTooltipWidth(Object tooltipParam) {
                return TOOLTIP_WIDTH;
            }

            @Override
            public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
                tooltip.addPara(SIZE_TOOLTIP, 0f, Misc.getHighlightColor(), size + " MB");
            }
        };
    }

    private void toggleRecording() {
        if (JfrProfiler.isRecording()) stopRecording();
        else startRecording();
        updateRecordingState();
    }

    private void startRecording() {
        try {
            JfrProfiler.start(outputFolder, maxSizeMb * BYTES_PER_MB);
            PatchLibLogger.info("Started a JFR recording");
        } catch (Throwable ex) {
            showError("Could not start recording, see the log.", ex);
        }
    }

    private void stopRecording() {
        try {
            Path file = JfrProfiler.stop();
            PatchLibLogger.info("Saved a JFR recording to " + file);
            result = "Saved " + file.getFileName();
            resultColor = Misc.getPositiveHighlightColor();
        } catch (Throwable ex) {
            showError("Could not save recording, see the log.", ex);
        }
    }

    private void openFolder() {
        try {
            JfrProfiler.openFolder(outputFolder);
        } catch (Throwable ex) {
            showError("Could not open folder, see the log.", ex);
            updateRecordingState();
        }
    }

    private void showError(String message, Throwable ex) {
        PatchLibLogger.error(message, ex);
        result = message;
        resultColor = Misc.getNegativeHighlightColor();
    }

    private void selectSize(int size) {
        maxSizeMb = size;
        JfrProfiler.setMaxSize(size * BYTES_PER_MB);
        updateSizeButtons();
    }

    private void updateSizeButtons() {
        for (Map.Entry<Integer, ButtonElement> button : sizeButtons.entrySet()) {
            button.getValue().setHighlighted(button.getKey() == maxSizeMb);
        }
    }

    private void updateRecordingState() {
        boolean recording = JfrProfiler.isRecording();
        recordButton.setText(recording ? "Stop Recording" : "Start Recording");
        recordButton.setHighlighted(recording);

        if (recording) {
            lastElapsedSeconds = -1;
            updateElapsedTime();
        } else {
            statusLabel.setText(result);
            statusLabel.setColor(resultColor);
        }
    }

    private void updateElapsedTime() {
        long seconds = JfrProfiler.getElapsedTime().toSeconds();
        if (seconds == lastElapsedSeconds) return;

        lastElapsedSeconds = seconds;
        statusLabel.setText(String.format("Recording %02d:%02d", seconds / 60, seconds % 60));
        statusLabel.setColor(Misc.getHighlightColor());
    }

    @Override
    public void positionChanged(PositionAPI position) {
        this.position = position;
    }

    @Override
    public void renderBelow(float alphaMult) {
        if (position == null) return;

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        Misc.renderQuad(position.getX() + separatorX, position.getY() + BOX_PAD, 1f, position.getHeight() - TAB_HEIGHT - BOX_PAD * 2f, Misc.getDarkPlayerColor(), alphaMult);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    @Override
    public void advance(float amount) {
        if (JfrProfiler.isRecording()) updateElapsedTime();
    }
}
