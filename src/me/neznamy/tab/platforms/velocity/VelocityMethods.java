package me.neznamy.tab.platforms.velocity;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import me.neznamy.tab.premium.AlignedSuffix;
import me.neznamy.tab.premium.Premium;
import me.neznamy.tab.premium.ScoreboardManager;
import me.neznamy.tab.shared.ITabPlayer;
import me.neznamy.tab.shared.PlatformMethods;
import me.neznamy.tab.shared.Shared;
import me.neznamy.tab.shared.config.Configs;
import me.neznamy.tab.shared.config.ConfigurationFile;
import me.neznamy.tab.shared.config.YamlConfigurationFile;
import me.neznamy.tab.shared.features.BelowName;
import me.neznamy.tab.shared.features.GhostPlayerFix;
import me.neznamy.tab.shared.features.GlobalPlayerlist;
import me.neznamy.tab.shared.features.GroupRefresher;
import me.neznamy.tab.shared.features.HeaderFooter;
import me.neznamy.tab.shared.features.NameTag16;
import me.neznamy.tab.shared.features.PlaceholderManager;
import me.neznamy.tab.shared.features.Playerlist;
import me.neznamy.tab.shared.features.SpectatorFix;
import me.neznamy.tab.shared.features.TabObjective;
import me.neznamy.tab.shared.features.UpdateChecker;
import me.neznamy.tab.shared.features.bossbar.BossBar;
import me.neznamy.tab.shared.packets.UniversalPacketPlayOut;
import me.neznamy.tab.shared.permission.LuckPerms;
import me.neznamy.tab.shared.permission.None;
import me.neznamy.tab.shared.permission.PermissionPlugin;
import me.neznamy.tab.shared.placeholders.Placeholders;
import me.neznamy.tab.shared.placeholders.PlayerPlaceholder;
import me.neznamy.tab.shared.placeholders.UniversalPlaceholderRegistry;
import net.kyori.adventure.text.TextComponent;

public class VelocityMethods implements PlatformMethods {

	private ProxyServer server;
	
	public VelocityMethods(ProxyServer server) {
		this.server = server;
	}
	
	@Override
	public PermissionPlugin detectPermissionPlugin() {
		if (server.getPluginManager().getPlugin("luckperms").isPresent()) {
			return new LuckPerms(server.getPluginManager().getPlugin("luckperms").get().getDescription().getVersion().get());
		} else {
			return new None();
		}
	}
	
	@Override
	public void loadFeatures(boolean inject) throws Exception{
		PlaceholderManager plm = new PlaceholderManager();
		plm.addRegistry(new VelocityPlaceholderRegistry(server));
		plm.addRegistry(new UniversalPlaceholderRegistry());
		plm.registerPlaceholders();
		Shared.registerFeature("placeholders", plm);
		if (Configs.config.getBoolean("classic-vanilla-belowname.enabled", true)) 			Shared.registerFeature("belowname", new BelowName());
		if (Configs.BossBarEnabled) 														Shared.registerFeature("bossbar", new BossBar());
		if (Configs.config.getBoolean("do-not-move-spectators", false)) 					Shared.registerFeature("spectatorfix", new SpectatorFix());
		if (Configs.config.getBoolean("global-playerlist.enabled", false)) 					Shared.registerFeature("globalplayerlist", new GlobalPlayerlist());
		if (Configs.config.getBoolean("enable-header-footer", true)) 						Shared.registerFeature("headerfooter", new HeaderFooter());
		if (Configs.config.getBoolean("change-nametag-prefix-suffix", true))				Shared.registerFeature("nametag16", new NameTag16());
		if (Configs.config.getString("yellow-number-in-tablist", "%ping%").length() > 0) 	Shared.registerFeature("tabobjective", new TabObjective());
		if (Configs.config.getBoolean("change-tablist-prefix-suffix", true)) {
			Playerlist playerlist = new Playerlist();
			Shared.registerFeature("playerlist", playerlist);
			if (Premium.alignTabsuffix) Shared.registerFeature("alignedsuffix", new AlignedSuffix(playerlist));
		}
		if (Premium.is() && Premium.premiumconfig.getBoolean("scoreboard.enabled", false)) 	Shared.registerFeature("scoreboard", new ScoreboardManager());
		if (Configs.SECRET_remove_ghost_players) 											Shared.registerFeature("ghostplayerfix", new GhostPlayerFix());
		new GroupRefresher();
		new UpdateChecker();
		
		for (Player p : server.getAllPlayers()) {
			ITabPlayer t = new TabPlayer(p, p.getCurrentServer().get().getServerInfo().getName());
			Shared.data.put(p.getUniqueId(), t);
			if (inject) Main.inject(t.getUniqueId());
		}
	}
	
	@Override
	public void sendConsoleMessage(String message) {
		server.getConsoleCommandSource().sendMessage(TextComponent.of(Placeholders.color(message)));
	}
	
	@Override
	public void sendRawConsoleMessage(String message) {
		server.getConsoleCommandSource().sendMessage(TextComponent.of(message));
	}
	
	@Override
	public Object buildPacket(UniversalPacketPlayOut packet, me.neznamy.tab.shared.ProtocolVersion protocolVersion) {
		return packet.toVelocity(protocolVersion);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public void loadConfig() throws Exception {
		Configs.config = new YamlConfigurationFile(Configs.dataFolder, "bungeeconfig.yml", "config.yml", Arrays.asList("# Detailed explanation of all options available at https://github.com/NEZNAMY/TAB/wiki/config.yml", ""));
		Configs.serverAliases = Configs.config.getConfigurationSection("server-aliases");
	}
	
	@Override
	public void registerUnknownPlaceholder(String identifier) {
		if (identifier.contains("_")) {
			String plugin = identifier.split("_")[0].replace("%", "").toLowerCase();
			if (plugin.equals("some")) return;
			Shared.debug("Detected used PlaceholderAPI placeholder " + identifier);
			PlaceholderManager pl = PlaceholderManager.getInstance();
			int cooldown = pl.DEFAULT_COOLDOWN;
			if (pl.playerPlaceholderRefreshIntervals.containsKey(identifier)) cooldown = pl.playerPlaceholderRefreshIntervals.get(identifier);
			if (pl.serverPlaceholderRefreshIntervals.containsKey(identifier)) cooldown = pl.serverPlaceholderRefreshIntervals.get(identifier);
			if (pl.serverConstantList.contains(identifier)) cooldown = 9999999;
			Placeholders.registerPlaceholder(new PlayerPlaceholder(identifier, cooldown){
				public String get(ITabPlayer p) {
					Main.plm.requestPlaceholder(p, identifier);
					String name;
					if (p == null) {
						name = "null";
					} else {
						name = p.getName();
					}
					return lastValue.get(name);
				}
			}, true);
			return;
		}
	}
	
	@Override
	public void convertConfig(ConfigurationFile config) {
		if (config.getName().equals("config.yml")) {
			if (config.hasConfigOption("belowname.refresh-interval")) {
				int value = config.getInt("belowname.refresh-interval");
				convert(config, "belowname.refresh-interval", value, "belowname.refresh-interval-milliseconds", value);
			}
			if (config.getObject("global-playerlist") instanceof Boolean) {
				rename(config, "global-playerlist", "global-playerlist.enabled");
				config.set("global-playerlist.spy-servers", Arrays.asList("spyserver1", "spyserver2"));
				Map<String, List<String>> serverGroups = new HashMap<String, List<String>>();
				serverGroups.put("lobbies", Arrays.asList("lobby1", "lobby2"));
				serverGroups.put("group2", Arrays.asList("server1", "server2"));
				config.set("global-playerlist.server-groups", serverGroups);
				config.set("global-playerlist.display-others-as-spectators", false);
				Shared.print('2', "Converted old global-playerlist section to new one in config.yml.");
			}
			rename(config, "tablist-objective-value", "yellow-number-in-tablist");
			rename(config, "belowname", "classic-vanilla-belowname");
			removeOld(config, "nametag-refresh-interval-milliseconds");
			removeOld(config, "tablist-refresh-interval-milliseconds");
			removeOld(config, "header-footer-refresh-interval-milliseconds");
			removeOld(config, "classic-vanilla-belowname.refresh-interval-milliseconds");
		}
		if (config.getName().equals("premiumconfig.yml")) {
			removeOld(config, "scoreboard.refresh-interval-ticks");
			if (!config.hasConfigOption("placeholder-output-replacements")) {
				Map<String, Map<String, String>> replacements = new HashMap<String, Map<String, String>>();
				Map<String, String> essVanished = new HashMap<String, String>();
				essVanished.put("Yes", "&7| Vanished");
				essVanished.put("No", "");
				replacements.put("%essentials_vanished%", essVanished);
				Map<String, String> tps = new HashMap<String, String>();
				tps.put("20", "&aPerfect");
				replacements.put("%tps%", tps);
				config.set("placeholder-output-replacements", replacements);
				Shared.print('2', "Added new missing \"placeholder-output-replacements\" premiumconfig.yml section.");
			}
			boolean scoreboardsConverted = false;
			for (Object scoreboard : config.getConfigurationSection("scoreboards").keySet()) {
				Boolean permReq = config.getBoolean("scoreboards." + scoreboard + ".permission-required");
				if (permReq != null) {
					if (permReq) {
						config.set("scoreboards." + scoreboard + ".display-condition", "permission:tab.scoreboard." + scoreboard);
					}
					config.set("scoreboards." + scoreboard + ".permission-required", null);
					scoreboardsConverted = true;
				}
				String childBoard = config.getString("scoreboards." + scoreboard + ".if-permission-missing");
				if (childBoard != null) {
					config.set("scoreboards." + scoreboard + ".if-permission-missing", null);
					config.set("scoreboards." + scoreboard + ".if-condition-not-met", childBoard);
					scoreboardsConverted = true;
				}
			}
			if (scoreboardsConverted) {
				Shared.print('2', "Converted old premiumconfig.yml scoreboard display condition system to new one.");
			}
			removeOld(config, "scoreboard.refresh-interval-milliseconds");
		}
		if (config.getName().equals("bossbar.yml")) {
			removeOld(config, "refresh-interval-milliseconds");
		}
	}
	
	@Override
	public String getServerVersion() {
		return server.getVersion().getName() + " v" + server.getVersion().getVersion();
	}
	
	@Override
	public void suggestPlaceholders() {
		//bungee only
		suggestPlaceholderSwitch("%premiumvanish_bungeeplayercount%", "%canseeonline%");
		suggestPlaceholderSwitch("%bungee_total%", "%online%");
		for (RegisteredServer server : server.getAllServers()) {
			suggestPlaceholderSwitch("%bungee_" + server.getServerInfo().getName() + "%", "%online_" + server.getServerInfo().getName() + "%");
		}

		//both
		suggestPlaceholderSwitch("%cmi_user_ping%", "%ping%");
		suggestPlaceholderSwitch("%player_ping%", "%ping%");
		suggestPlaceholderSwitch("%viaversion_player_protocol_version%", "%player-version%");
		suggestPlaceholderSwitch("%player_name%", "%nick%");
		suggestPlaceholderSwitch("%uperms_rank%", "%rank%");
	}
}