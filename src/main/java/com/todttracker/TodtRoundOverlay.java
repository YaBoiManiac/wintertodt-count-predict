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
                .text("Wintertodt Tracker")
                .color(config.textColor())
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Rounds:")
                .right(String.format("%.0f", plugin.getRoundsRemaining()))
                .rightColor(config.textColor())
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Avg XP:")
                .right(String.format("%.0f", plugin.getAvgRoundExperience()))
                .rightColor(config.textColor())
                .build());

        return super.render(graphics);
    }
}
