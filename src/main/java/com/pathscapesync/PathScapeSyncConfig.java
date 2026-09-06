package com.pathscapesync;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(PathScapeSyncConfig.GROUP)
public interface PathScapeSyncConfig extends Config
{
	String GROUP = "pathscape-sync";

	@ConfigSection(
		name = "Link",
		description = "Paste the link token and public API key from PathScape Preferences, then hit Pair in the side panel.",
		position = 0
	)
	String linkSection = "linkSection";

	@ConfigSection(
		name = "Upload",
		description = "When and how often snapshots are sent.",
		position = 1
	)
	String uploadSection = "uploadSection";

	@ConfigItem(
		keyName = "submitToPathscape",
		name = "Submit to PathScape",
		description = "Send this logged-in account's skills, quests, diaries, combat achievements, boss KC, and worn gear to PathScape.",
		warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers",
		section = uploadSection,
		position = 0
	)
	default boolean submitToPathscape()
	{
		return false;
	}

	@ConfigItem(
		keyName = "linkToken",
		name = "Link token",
		description = "64-character token from PathScape Preferences → PathScape Sync. Paste it here, then hit Pair.",
		section = linkSection,
		secret = true,
		position = 1
	)
	default String linkToken()
	{
		return "";
	}

	/**
	 * Legacy short pairing code (cloud email flow). Still accepted if link token is empty.
	 */
	@ConfigItem(
		keyName = "pairingCode",
		name = "Legacy pairing code",
		description = "Old short code from signed-in Preferences. Prefer Link token above for new setups.",
		section = linkSection,
		secret = true,
		position = 2
	)
	default String pairingCode()
	{
		return "";
	}

	@ConfigItem(
		keyName = "publicApiKey",
		name = "PathScape public API key",
		description = "The public anon key shown in PathScape Preferences. The gateway may require it; it is not a password.",
		section = linkSection,
		secret = true,
		position = 3
	)
	default String publicApiKey()
	{
		return "";
	}

	@ConfigItem(
		keyName = "uploadInterval",
		name = "Upload interval",
		description = "How often to post a snapshot after the login snapshot.",
		section = uploadSection,
		position = 1
	)
	default UploadInterval uploadInterval()
	{
		return UploadInterval.FIVE_MINUTES;
	}

	@ConfigItem(
		keyName = "status",
		name = "Status",
		description = "Last Pair or Sync result. Also shown in the PathScape Sync side panel.",
		section = linkSection,
		position = 4
	)
	default String status()
	{
		return "Not linked";
	}
}
