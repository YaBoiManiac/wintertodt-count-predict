package com.todttracker;

import com.google.inject.Provides;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;

import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
	name = "Wintertodt Round Counter"
)
public class TodtRoundPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private TodtRoundConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private TodtRoundOverlay overlay;

	private boolean pluginActive = false;
	private boolean todtActive = false;
	private boolean wasTodtActive = false;
	private double avgRoundExperience = 0;
	private double roundsRemaining = 0;
	private int roundExperience = 0;
	private int lastExperience = 0;
	private boolean giveBonusXp = false;
	private ArrayList<Integer> xpList = new ArrayList<>();

	private boolean inTodtRegion()
	{
		if (client.getLocalPlayer() == null) return false;

		return client.getLocalPlayer().getWorldLocation().getRegionID() == 6462;
	}	

	private int getTodtEnergy()
	{
		if (client.getLocalPlayer() == null) return 0;

		Widget wtEnergyWidget = client.getWidget(25952282);

		if (!inTodtRegion() || wtEnergyWidget == null) return 0;

		Pattern regex = Pattern.compile("\\d+");
		Matcher bossEnergy = regex.matcher(wtEnergyWidget.getText().toString());

		if (!bossEnergy.find()) return 0;

		return Integer.parseInt(bossEnergy.group(0));
	}

	private void calcAvgExperience()
	{
		if (giveBonusXp)
		{
			roundExperience += client.getRealSkillLevel(Skill.FIREMAKING) * 100;
		}

		xpList.add(roundExperience);
		roundExperience = 0;

		if (xpList.size() > 20) xpList.remove(0);

		int sum = 0;
		for (int xp : xpList) sum += xp;

		avgRoundExperience = 0;
		if (!xpList.isEmpty())
		{
			avgRoundExperience = (double) sum / xpList.size();
		}

	}

	private void calcRoundsRemaining()
	{
		int fmLevel = client.getRealSkillLevel(Skill.FIREMAKING);
		int xpForNextLevel = Experience.getXpForLevel(fmLevel + 1);
		int xpRemaining = xpForNextLevel - client.getSkillExperience(Skill.FIREMAKING);
		roundsRemaining = xpRemaining / avgRoundExperience;
	}

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(overlay);
	}

	@Subscribe
	public void onGameTick(GameTick gameTick)
	{

		if (inTodtRegion())
		{
			int hp = getTodtEnergy();
			todtActive = hp != 0;
			pluginActive = true;
		}
		else
		{
			todtActive = false;
			pluginActive = false;
		}

		if (!todtActive && wasTodtActive)
		{
			calcAvgExperience();
			calcRoundsRemaining();
			wasTodtActive = false;
			log.debug("Round Ended");
		}
		else if (todtActive && !wasTodtActive)
		{
			wasTodtActive = true;
			log.debug("Round Started");

			// fetch xp at the start of todt incase player did fm outside
			lastExperience = client.getSkillExperience(Skill.FIREMAKING);
			giveBonusXp = false;
		}

	}

	@Subscribe
	public void onStatChanged(StatChanged statChanged)
	{
		if (!pluginActive || statChanged.getSkill() != Skill.FIREMAKING) return;

		int xp = statChanged.getXp();
		int xpGained = xp - lastExperience;

		// ignore end of round xp drop
		if (xpGained > 4500) return;

		roundExperience += xpGained;
		lastExperience = xp;

		log.debug("Gained FM XP: {}", xpGained);
	}

    @Subscribe
    public void onChatMessage(ChatMessage chatMessage)
	{
		if (!pluginActive) return;

        ChatMessageType chatMessageType = chatMessage.getType();

        if (chatMessageType != ChatMessageType.GAMEMESSAGE && chatMessageType != ChatMessageType.SPAM) {
            return;
        }

        String msg = chatMessage.getMessageNode().getValue();

		if (msg.toLowerCase().startsWith("you have helped enough to earn a supply crate"))
		{
			giveBonusXp = true;
		}

	}

	public int getRoundsRemaining()
	{
		return (int)Math.ceil(roundsRemaining);
	}

	public double getAvgRoundExperience()
	{
		return avgRoundExperience;
	}

	@Provides
    TodtRoundConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(TodtRoundConfig.class);
	}
}
