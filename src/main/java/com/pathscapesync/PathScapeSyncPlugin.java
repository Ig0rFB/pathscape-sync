package com.pathscapesync;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Player;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import okhttp3.OkHttpClient;

@Slf4j
@PluginDescriptor(
	name = "PathScape Sync"
)
public class PathScapeSyncPlugin extends Plugin
{
	private static final String DEVICE_TOKEN_KEY = "deviceToken";

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private PathScapeSyncConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ScheduledExecutorService executor;

	@Inject
	private OkHttpClient okHttpClient;

	@Inject
	private Gson gson;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ClientToolbar clientToolbar;

	private PathScapeSyncClient syncClient;
	private SnapshotCollector collector;
	private PathScapeSyncPanel panel;
	private NavigationButton navButton;
	private ScheduledFuture<?> uploadTask;
	private ScheduledFuture<?> equipmentDebounce;

	@Override
	protected void startUp()
	{
		syncClient = new PathScapeSyncClient(okHttpClient, gson);
		collector = new SnapshotCollector(client, itemManager);
		panel = new PathScapeSyncPanel(this::onPairClicked, this::onSyncClicked);
		panel.setStatus(config.status());

		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");
		navButton = NavigationButton.builder()
			.tooltip("PathScape Sync")
			.icon(icon)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

		log.info("PathScape Sync started");
		rescheduleUploads();
	}

	@Override
	protected void shutDown()
	{
		cancelUploads();
		cancelEquipmentDebounce();
		if (navButton != null)
		{
			clientToolbar.removeNavigation(navButton);
			navButton = null;
		}
		panel = null;
		log.info("PathScape Sync stopped");
	}

	@Provides
	PathScapeSyncConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PathScapeSyncConfig.class);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			clientThread.invokeLater(this::onLoggedIn);
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!PathScapeSyncConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}
		if (DEVICE_TOKEN_KEY.equals(event.getKey()) || "status".equals(event.getKey()))
		{
			return;
		}
		// Pairing is manual from the side panel. Config changes only retune the interval timer.
		rescheduleUploads();
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (!config.submitToPathscape() || getDeviceToken() == null)
		{
			return;
		}
		if (event.getContainerId() != InventoryID.EQUIPMENT.getId())
		{
			return;
		}
		cancelEquipmentDebounce();
		equipmentDebounce = executor.schedule(
			() -> clientThread.invoke(() -> uploadSnapshot(false)),
			2,
			TimeUnit.SECONDS
		);
	}

	/**
	 * Pair is a button in the side panel, not automatic on login or config change.
	 */
	void onPairClicked()
	{
		setStatus("Pairing\u2026");
		clientThread.invoke(this::pairNow);
	}

	/**
	 * One snapshot, now. Does not pair. Submit must already be on.
	 */
	void onSyncClicked()
	{
		setStatus("Uploading\u2026");
		clientThread.invoke(this::syncNow);
	}

	private void onLoggedIn()
	{
		if (!config.submitToPathscape() || getDeviceToken() == null)
		{
			return;
		}
		uploadSnapshot(false);
	}

	private void pairNow()
	{
		Player local = client.getLocalPlayer();
		if (local == null || local.getName() == null)
		{
			fail("Log in to Old School RuneScape first");
			return;
		}
		String linkToken = config.linkToken() == null ? "" : config.linkToken().trim();
		String legacyCode = config.pairingCode() == null ? "" : config.pairingCode().trim();
		String codeOrToken = !linkToken.isEmpty() ? linkToken : legacyCode;
		if (codeOrToken.isEmpty())
		{
			fail("Paste a link token from PathScape Preferences");
			return;
		}
		String apiKey = publicApiKey();
		if (apiKey.isEmpty())
		{
			fail("Paste the PathScape public API key");
			return;
		}

		String rsn = local.getName();
		syncClient.pair(apiKey, codeOrToken, rsn, result -> clientThread.invoke(() ->
		{
			if (!result.ok)
			{
				fail(result.error != null ? result.error : "Pairing failed");
				return;
			}
			storeDeviceToken(result.token);
			setStatus("Linked as " + rsn);
			chat("PathScape Sync linked as " + rsn + ".");
		}));
	}

	private void syncNow()
	{
		if (!config.submitToPathscape())
		{
			fail("Enable Submit to PathScape first");
			return;
		}
		if (getDeviceToken() == null)
		{
			fail("Pair this client first");
			return;
		}
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			fail("Log in to Old School RuneScape first");
			return;
		}
		uploadSnapshot(true);
	}

	private void uploadSnapshot(boolean announce)
	{
		if (!config.submitToPathscape())
		{
			return;
		}
		String token = getDeviceToken();
		if (token == null)
		{
			return;
		}
		Player local = client.getLocalPlayer();
		if (local == null || local.getName() == null)
		{
			if (announce)
			{
				fail("Log in to Old School RuneScape first");
			}
			return;
		}
		String rsn = local.getName();
		JsonObject payload = collector.collect(rsn);
		syncClient.postSnapshot(publicApiKey(), token, payload, error -> clientThread.invoke(() ->
		{
			if (error != null)
			{
				setStatus("Upload failed: " + error);
				log.debug("PathScape snapshot failed: {}", error);
				if (announce)
				{
					chat("PathScape Sync: " + error);
				}
				return;
			}
			setStatus("Uploaded " + rsn);
			log.debug("PathScape snapshot posted for {}", rsn);
			if (announce)
			{
				chat("PathScape snapshot uploaded.");
			}
		}));
	}

	private void rescheduleUploads()
	{
		cancelUploads();
		if (!config.submitToPathscape())
		{
			return;
		}
		int minutes = config.uploadInterval().getMinutes();
		uploadTask = executor.scheduleAtFixedRate(
			() -> clientThread.invoke(() -> uploadSnapshot(false)),
			minutes,
			minutes,
			TimeUnit.MINUTES
		);
	}

	private void cancelUploads()
	{
		if (uploadTask != null)
		{
			uploadTask.cancel(false);
			uploadTask = null;
		}
	}

	private void cancelEquipmentDebounce()
	{
		if (equipmentDebounce != null)
		{
			equipmentDebounce.cancel(false);
			equipmentDebounce = null;
		}
	}

	private String publicApiKey()
	{
		String key = config.publicApiKey();
		return key == null ? "" : key.trim();
	}

	private String getDeviceToken()
	{
		String token = configManager.getRSProfileConfiguration(PathScapeSyncConfig.GROUP, DEVICE_TOKEN_KEY);
		if (token == null || token.trim().isEmpty())
		{
			return null;
		}
		return token.trim();
	}

	private void storeDeviceToken(String token)
	{
		configManager.setRSProfileConfiguration(PathScapeSyncConfig.GROUP, DEVICE_TOKEN_KEY, token);
	}

	private void fail(String message)
	{
		setStatus(message);
		chat("PathScape Sync: " + message);
	}

	private void chat(String message)
	{
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
	}

	private void setStatus(String status)
	{
		configManager.setConfiguration(PathScapeSyncConfig.GROUP, "status", status);
		PathScapeSyncPanel current = panel;
		if (current != null)
		{
			SwingUtilities.invokeLater(() -> current.setStatus(status));
		}
	}
}
