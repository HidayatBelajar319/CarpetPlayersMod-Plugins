package com.carpetplayers.network;

import net.minecraft.resources.ResourceLocation;

public final class ModPackets {
    public static final ResourceLocation OPEN_MENU = new ResourceLocation("carpetplayers", "open_menu");
    public static final ResourceLocation BOT_ACTION = new ResourceLocation("carpetplayers", "bot_action");
    public static final ResourceLocation REQUEST_BOTS = new ResourceLocation("carpetplayers", "request_bots");
    public static final ResourceLocation BOT_LIST = new ResourceLocation("carpetplayers", "bot_list");

    // Waypoint packets
    public static final ResourceLocation DEATH_WAYPOINT = new ResourceLocation("carpetplayers", "death_waypoint");
    public static final ResourceLocation WAYPOINT_SYNC = new ResourceLocation("carpetplayers", "waypoint_sync");

    private ModPackets() {}
}
