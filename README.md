# PathScape Sync

RuneLite plugin that connects your Old School RuneScape account to
[PathScape](https://pathscape.vercel.app) so its tracker can use live skills
(including XP ahead of the Jagex hiscores), quests, diaries, combat achievements,
boss KC, and currently worn gear.

## How it works

1. On [pathscape.vercel.app](https://pathscape.vercel.app), look up your character and open **Preferences → PathScape Sync**.
2. **Generate link token** and copy the **public API key**.
3. Install this plugin, open its settings, and paste both into **Link token** and **PathScape public API key**.
4. Open the **PathScape Sync** side panel and hit **Pair** while logged into the matching account.
5. Enable **Submit to PathScape** (read the warning), then **Sync now**.
6. Refresh PathScape. On another device (for example your phone), paste the same link token under Preferences so Refresh can read the snapshot there too.

No PathScape cloud email sign-in is required for linking.

## What it sends

While Submit is on and this client is paired, the plugin uploads **your logged-in account’s game state only**:

- Skills (levels and XP)
- Quest states
- Achievement diary progress
- Combat achievement progress
- Boss kill counts
- Currently worn equipment

It does **not** read or send your password, email, payment details, or Jagex account credentials — RuneLite does not expose those to plugins.

Uploads happen only while the plugin is enabled, Submit is on, and Pair has succeeded. Clear the link token in plugin settings, disable Submit, or revoke the token in PathScape Preferences to stop. Your IP address is sent to PathScape’s sync server with each upload.

## What PathScape does with it

PathScape keeps Jagex hiscores as the baseline. On Refresh it merges the snapshot:

- Skills and boss KC: higher value wins (an old sync cannot drag you backwards)
- Quests, diaries, and combat tasks: more complete state wins
- Worn gear: plugin only

## Privacy

Read-only export from the game client. No automation or input injection. Only the account you are logged into is uploaded.
