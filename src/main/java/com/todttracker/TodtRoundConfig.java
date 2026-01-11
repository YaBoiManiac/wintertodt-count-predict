package com.todttracker;

import java.awt.Color;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("todttracker")
public interface TodtRoundConfig extends Config
{

	@ConfigItem(
		keyName = "textColor",
		name = "Text Color",
		description = "Set the text color",
		position = 0
	)
	default Color textColor() {
		return new Color(0.0f, 1.0f, 0.0f, 1.0f);
	}

	@ConfigItem(
		keyName = "targetLvl",
		name = "Target Lvl",
		description = "Set your Target Level",
		position = 1
	)
	default int targetLvl() {
		return 0;
	}

	@ConfigItem(
		keyName = "useTargetLvl",
		name = "Use Target Level",
		description = "Whether you use the target level",
		position = 2
	)
	default boolean useTargetLvl() {
		return false;
	}

	@ConfigItem(
		keyName = "showTimeStats",
		name = "Show Time Stats",
		description = "Show average time and estimated time remaining",
		position = 3
	)
	default boolean showTimeStats() {
		return true;
	}

}
