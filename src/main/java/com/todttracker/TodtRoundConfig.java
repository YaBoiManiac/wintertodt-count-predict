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

}
