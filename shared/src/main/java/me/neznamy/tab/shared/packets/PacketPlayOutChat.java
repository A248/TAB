package me.neznamy.tab.shared.packets;

import me.neznamy.tab.shared.ProtocolVersion;
import me.neznamy.tab.shared.TAB;

/**
 * A class representing platform specific packet class
 */
public class PacketPlayOutChat extends UniversalPacketPlayOut {

	//the message to be sent
	public IChatBaseComponent message;
	
	//position of the message
	public ChatMessageType type;

	/**
	 * Constructs new instance of the class with given message
	 * @param message - message to be sent
	 */
	public PacketPlayOutChat(String message) {
		this.message = IChatBaseComponent.optimizedComponent(message);
		this.type = ChatMessageType.CHAT;
	}

	/**
	 * Constructs new instance of the class with given message and position
	 * @param message - message to be sent
	 * @param type - message position
	 */
	public PacketPlayOutChat(String message, ChatMessageType type) {
		this.message = IChatBaseComponent.optimizedComponent(message);
		this.type = type;
	}

	/**
	 * Constructs new instance of the class with given message
	 * @param message - message to be sent
	 */
	public PacketPlayOutChat(IChatBaseComponent message) {
		this.message = message;
		this.type = ChatMessageType.CHAT;
	}
	
	/**
	 * Constructs new instance of the class with given message and position
	 * @param message - message to be sent
	 * @param type - message position
	 */
	public PacketPlayOutChat(IChatBaseComponent message, ChatMessageType type) {
		this.message = message;
		this.type = type;
	}

	/**
	 * Calls build method of packet builder instance and returns output
	 */
	@Override
	protected Object build(ProtocolVersion clientVersion) throws Exception {
		return TAB.getInstance().getPacketBuilder().build(this, clientVersion);
	}
	
	/**
	 * An override to toString() method for better output
	 */
	@Override
	public String toString() {
		return String.format("PacketPlayOutChat{message=%s,type=%s}", message, type);
	}

	/**
	 * An enum representing positions of a chat message
	 */
	public enum ChatMessageType {

		CHAT,
		SYSTEM,
		GAME_INFO;
	}
}