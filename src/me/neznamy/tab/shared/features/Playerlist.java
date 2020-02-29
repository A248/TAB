package me.neznamy.tab.shared.features;

import java.util.*;

import me.neznamy.tab.shared.*;
import me.neznamy.tab.shared.packets.PacketPlayOutPlayerInfo;
import me.neznamy.tab.shared.packets.PacketPlayOutPlayerInfo.*;
import me.neznamy.tab.shared.placeholders.Placeholders;
import me.neznamy.tab.shared.packets.UniversalPacketPlayOut;

public class Playerlist implements SimpleFeature, CustomPacketFeature{

	public int refresh;

	public void load(){
		refresh = Configs.config.getInt("tablist-refresh-interval-milliseconds", 1000);
		updateNames(true);
		Shared.cpu.startRepeatingMeasuredTask(refresh, "refreshing tablist prefix/suffix", "Tablist names 1", new Runnable() {
			public void run() {
				updateNames(false);
			}
		});
	}
	public void unload(){
		updateNames(true);
	}
	@Override
	public void onJoin(ITabPlayer p) {
	}
	@Override
	public void onQuit(ITabPlayer p) {
	}
	@Override
	public void onWorldChange(ITabPlayer p, String from, String to) {
//		if (!(isDisabledWorld(Configs.disabledTablistNames, from) && isDisabledWorld(Configs.disabledTablistNames, to))) {
			if (!Configs.disabledTablistNames.contains("NORESET")) p.updatePlayerListName();
//		}
	}
	private void updateNames(boolean force){
		if (ProtocolVersion.SERVER_VERSION.getMinorVersion() < 8) return;
		List<PlayerInfoData> updatedPlayers = new ArrayList<PlayerInfoData>();
		for (ITabPlayer p : Shared.getPlayers()) {
			if (!p.disabledTablistNames && (p.isListNameUpdateNeeded() || force)) updatedPlayers.add(p.getInfoData());
		}
		if (!updatedPlayers.isEmpty()) {
			Object packet = PacketAPI.buildPacket(new PacketPlayOutPlayerInfo(EnumPlayerInfoAction.UPDATE_DISPLAY_NAME, updatedPlayers), null);
			for (ITabPlayer all : Shared.getPlayers()) {
				if (all.getVersion().getMinorVersion() >= 8) all.sendPacket(packet);
			}
		}
	}
	@Override
	public UniversalPacketPlayOut onPacketSend(ITabPlayer receiver, UniversalPacketPlayOut packet) {
		if (!(packet instanceof PacketPlayOutPlayerInfo)) return packet;
		if (receiver.getVersion().getMinorVersion() < 8) return packet;
		PacketPlayOutPlayerInfo info = (PacketPlayOutPlayerInfo) packet;
		List<PlayerInfoData> v180PrefixBugFixList = new ArrayList<PlayerInfoData>();
		for (PlayerInfoData playerInfoData : info.players) {
			ITabPlayer packetPlayer = Shared.getPlayerByTablistUUID(playerInfoData.uniqueId);
			if (info.action == EnumPlayerInfoAction.REMOVE_PLAYER && Shared.features.containsKey("globalplayerlist")) {
				if (packetPlayer != null) { //player online
					if (!PluginHooks._isVanished(packetPlayer)) {
						//changing to random non-existing player, the easiest way to cancel the removal
						playerInfoData.uniqueId = UUID.randomUUID();
					}
				}
			}
			if (info.action == EnumPlayerInfoAction.UPDATE_DISPLAY_NAME || info.action == EnumPlayerInfoAction.ADD_PLAYER) {
				if (packetPlayer != null && !packetPlayer.disabledTablistNames) {
					playerInfoData.listName = packetPlayer.getTabFormat(receiver);
				}
			}
			if (info.action == EnumPlayerInfoAction.ADD_PLAYER) {
				if (packetPlayer == null) {
					//NPC on bukkit
					if (Shared.features.containsKey("nametagx") && Configs.modifyNPCnames) {
						if (playerInfoData.name.length() <= 15) {
							if (playerInfoData.name.length() <= 14) {
								playerInfoData.name += Placeholders.colorChar + "r";
							} else {
								playerInfoData.name += " ";
							}
						}
					}
				} else {
					v180PrefixBugFixList.add(playerInfoData.clone());
				}
			}
		}
		if (info.action == EnumPlayerInfoAction.ADD_PLAYER && receiver.getVersion() == ProtocolVersion.v1_8) {
			Shared.cpu.runTaskLater(50, "sending PacketPlayOutPlayerInfo", "Tablist Names 3", new Runnable() {

				@Override
				public void run() {
					receiver.sendCustomPacket(new PacketPlayOutPlayerInfo(EnumPlayerInfoAction.UPDATE_DISPLAY_NAME, v180PrefixBugFixList));
				}
			});
		}
		return info;
	}
	@Override
	public String getCPUName() {
		return "Tablist Names 2";
	}
}