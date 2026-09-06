package com.pathscapesync;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

/**
 * Side panel for explicit Pair and Sync now. Config has the code and key;
 * this panel is the only place those actions fire.
 */
final class PathScapeSyncPanel extends PluginPanel
{
	private final JLabel statusLabel = new JLabel();

	PathScapeSyncPanel(Runnable onPair, Runnable onSync)
	{
		super(false);
		setBorder(new EmptyBorder(10, 0, 0, 0));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setLayout(new GridBagLayout());

		GridBagConstraints constraints = new GridBagConstraints();
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.weightx = 1;
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.insets = new Insets(0, 0, 8, 0);

		JLabel help = new JLabel("<html>Paste the link token and public API key in plugin settings, then Pair. Enable Submit before Sync now.</html>");
		help.setFont(FontManager.getRunescapeSmallFont());
		help.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		add(help, constraints);

		constraints.gridy++;
		JButton pair = new JButton("Pair");
		pair.setToolTipText("Link this logged-in account using the link token and key in plugin settings.");
		pair.addActionListener(event -> onPair.run());
		add(pair, constraints);

		constraints.gridy++;
		JButton sync = new JButton("Sync now");
		sync.setToolTipText("Push one snapshot to PathScape. Submit must be on, and this client must already be paired.");
		sync.addActionListener(event -> onSync.run());
		add(sync, constraints);

		constraints.gridy++;
		constraints.weighty = 1;
		constraints.anchor = GridBagConstraints.NORTH;
		statusLabel.setFont(FontManager.getRunescapeSmallFont());
		statusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		// Extra wrapper so the HTML status can sit under the buttons without stretching them.
		JPanel statusWrap = new JPanel(new GridBagLayout());
		statusWrap.setOpaque(false);
		GridBagConstraints statusConstraints = new GridBagConstraints();
		statusConstraints.anchor = GridBagConstraints.NORTHWEST;
		statusConstraints.fill = GridBagConstraints.HORIZONTAL;
		statusConstraints.weightx = 1;
		statusWrap.add(statusLabel, statusConstraints);
		add(statusWrap, constraints);

		setStatus("Not linked");
	}

	void setStatus(String status)
	{
		statusLabel.setText("<html>" + escape(status) + "</html>");
	}

	private static String escape(String text)
	{
		if (text == null || text.isEmpty())
		{
			return "Not linked";
		}
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
