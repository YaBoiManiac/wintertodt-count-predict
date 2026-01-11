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

	final int WINTERTODT_REGION_ID = 6462; 
	final int WINTERTODT_ENERGY_COMP_ID = 25952282;

	private boolean isInWintertodtRegion = false;
	private boolean isRoundActive = false;
	private boolean wasRoundActive = false;
	private double averageExperiencePerRound = 0;
	private double estimatedRoundsRemaining = 0;
	private int currentRoundExperience = 0;
	private int previousTotalExperience = 0;
	private boolean shouldAddBonusExperience = false;
	private ArrayList<Integer> roundExperienceHistory = new ArrayList<>();

	private boolean isPlayerInWintertodtRegion()
	{
		if (client.getLocalPlayer() == null) return false;

		return client.getLocalPlayer().getWorldLocation().getRegionID() == WINTERTODT_REGION_ID;
	}	

	private int getWintertodtEnergy()
	{
		if (client.getLocalPlayer() == null) return 0;

		Widget energyWidget = client.getWidget(WINTERTODT_ENERGY_COMP_ID);

		if (!isPlayerInWintertodtRegion() || energyWidget == null) return 0;

		Pattern numberPattern = Pattern.compile("\\d+");
		Matcher energyMatcher = numberPattern.matcher(energyWidget.getText().toString());

		if (!energyMatcher.find()) return 0;

		return Integer.parseInt(energyMatcher.group(0));
	}

	private void calculateAverageExperience()
	{

		if (currentRoundExperience == 0) {
			log.debug("Round complete. You didn't participate so ignoring XP.");
			return;
		}

		if (shouldAddBonusExperience)
		{
			currentRoundExperience += client.getRealSkillLevel(Skill.FIREMAKING) * 100;
		}

		roundExperienceHistory.add(currentRoundExperience);
		currentRoundExperience = 0;

		if (roundExperienceHistory.size() > 20) roundExperienceHistory.remove(0);

		int totalExperience = 0;
		for (int xp : roundExperienceHistory) totalExperience += xp;

		averageExperiencePerRound = 0;
		if (!roundExperienceHistory.isEmpty())
		{
			averageExperiencePerRound = (double) totalExperience / roundExperienceHistory.size();
			log.debug("Round complete. XP this round: {}, Average XP: {:.0f} (over {} rounds)", 
				roundExperienceHistory.get(roundExperienceHistory.size() - 1), 
				averageExperiencePerRound, 
				roundExperienceHistory.size());
		}

	}

	private void calculateRoundsRemaining()
	{
		int firemakingLevel = client.getRealSkillLevel(Skill.FIREMAKING);
		int experienceForNextLevel = Experience.getXpForLevel(firemakingLevel + 1);
		int experienceRemaining = experienceForNextLevel - client.getSkillExperience(Skill.FIREMAKING);
		estimatedRoundsRemaining = experienceRemaining / averageExperiencePerRound;
		log.debug("Estimated rounds to level {}: {} ({} XP remaining)", 
			firemakingLevel + 1, 
			(int)Math.ceil(estimatedRoundsRemaining), 
			experienceRemaining);
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

		if (isPlayerInWintertodtRegion())
		{
			int bossEnergy = getWintertodtEnergy();
			isRoundActive = bossEnergy != 0;
			isInWintertodtRegion = true;
		}
		else
		{
			isRoundActive = false;
			isInWintertodtRegion = false;
		}

		if (!isRoundActive && wasRoundActive)
		{
			calculateAverageExperience();
			calculateRoundsRemaining();
			wasRoundActive = false;
		}
		else if (isRoundActive && !wasRoundActive)
		{
			wasRoundActive = true;
			log.debug("Wintertodt round started");

			// Fetch XP at the start of round in case player did firemaking outside Wintertodt
			previousTotalExperience = client.getSkillExperience(Skill.FIREMAKING);
			shouldAddBonusExperience = false;
		}

	}

	@Subscribe
	public void onStatChanged(StatChanged statChanged)
	{
		if (!isInWintertodtRegion || statChanged.getSkill() != Skill.FIREMAKING) return;

		int currentExperience = statChanged.getXp();
		int experienceGained = currentExperience - previousTotalExperience;

		// Ignore end of round XP drop
		if (experienceGained > 4500) return;

		currentRoundExperience += experienceGained;
		previousTotalExperience = currentExperience;
	}

	@Subscribe
	public void onChatMessage(ChatMessage chatMessage)
	{
		if (!isInWintertodtRegion) return;

		ChatMessageType messageType = chatMessage.getType();

		if (messageType != ChatMessageType.GAMEMESSAGE && messageType != ChatMessageType.SPAM)
		{
			return;
		}

		String message = chatMessage.getMessageNode().getValue();

		if (message.toLowerCase().startsWith("you have helped enough to earn a supply crate"))
		{
			shouldAddBonusExperience = true;
			int bonusXp = client.getRealSkillLevel(Skill.FIREMAKING) * 100;
			log.debug("Supply crate earned! Bonus XP will be added: {}", bonusXp);
		}

	}

	public int getRoundsRemaining()
	{
		return (int)Math.ceil(estimatedRoundsRemaining);
	}

	public double getAverageRoundExperience()
	{
		return averageExperiencePerRound;
	}

	@Provides
    TodtRoundConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(TodtRoundConfig.class);
	}
}
