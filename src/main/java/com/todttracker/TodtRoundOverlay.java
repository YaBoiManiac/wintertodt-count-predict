package com.todttracker;

import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;

public class TodtRoundOverlay extends OverlayPanel {
    private final Client client;
    private final TodtRoundPlugin plugin;
    private final TodtRoundConfig config;

    @Inject
    private TodtRoundOverlay(Client client, TodtRoundPlugin plugin, TodtRoundConfig config) {
        super(plugin);
        setPosition(OverlayPosition.BOTTOM_LEFT);
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        getMenuEntries().add(new OverlayMenuEntry(MenuAction.RUNELITE_OVERLAY_CONFIG, OverlayManager.OPTION_CONFIGURE, "Wintertodt Round Counter Overlay"));
    }


    @Override
    public Dimension render(Graphics2D graphics) {
        panelComponent.getChildren().add(TitleComponent.builder()
                .text("ROUND TRACKER")
                .color(config.textColor())
                .build());

        if (config.useTargetLvl()) {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Target Level:")
                    .right(String.format("%d", plugin.getTargetFiremakingLevel()))
                    .rightColor(config.textColor())
                    .build());
        }

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Rounds Left:")
                .right(String.format("%d", plugin.getRoundsRemaining()))
                .rightColor(config.textColor())
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Avg XP:")
                .right(String.format("%,.0f", plugin.getAverageRoundExperience()))
                .rightColor(config.textColor())
                .build());

        if (config.showTimeStats()) {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Time Left:")
                    .right(formatTime(plugin.getEstimatedTimeRemaining()))
                    .rightColor(config.textColor())
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Avg Time:")
                    .right(formatTime(plugin.getAverageTimePerRound()))
                    .rightColor(config.textColor())
                    .build());
        }

        return super.render(graphics);
    }

    private String formatTime(double seconds) {
        if (seconds <= 0) return "--";
        
        int hours = (int) (seconds / 3600);
        int minutes = (int) ((seconds % 3600) / 60);
        int secs = (int) (seconds % 60);

        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, secs);
        } else {
            return String.format("%ds", secs);
        }
    }
}
