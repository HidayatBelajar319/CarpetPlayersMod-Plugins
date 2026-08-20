package com.carpetplayers.bot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Records bot actions (position snapshots + AI commands) and replays them.
 */
public final class BotRecorder {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // --- Recording state ---
    private static boolean recording = false;
    private static String recordingBotName;
    private static UUID recordingBotUuid;
    private static final List<RecordedFrame> frames = new ArrayList<>();
    private static int tickCounter;

    // --- Playback state ---
    private static boolean playing = false;
    private static String playbackBotName;
    private static UUID playbackBotUuid;
    private static int playbackIndex;
    private static int playbackTickCounter;

    // --- Configuration ---
    private static int recordIntervalTicks = 5; // record a frame every N ticks

    /**
     * A single recorded snapshot.
     */
    public static class RecordedFrame {
        public int tick;           // tick number when recorded
        public double x, y, z;    // position
        public float yaw, pitch;  // rotation
        public String action;     // AI action command (nullable)
        public String state;      // bot state name
    }

    // ========== RECORDING ==========

    public static synchronized boolean startRecording(String botName) {
        if (recording) return false;
        BotBrain brain = BotManager.findBrainByName(botName);
        if (brain == null) return false;
        recording = true;
        recordingBotName = botName;
        recordingBotUuid = brain.getBot().getUUID();
        frames.clear();
        tickCounter = 0;
        return true;
    }

    public static synchronized void recordTick() {
        if (!recording) return;
        tickCounter++;
        if (tickCounter % recordIntervalTicks != 0) return;

        BotBrain brain = BotManager.BRAINS.get(recordingBotUuid);
        if (brain == null || !brain.getBot().isAlive()) {
            stopRecording();
            return;
        }
        RecordedFrame frame = new RecordedFrame();
        frame.tick = tickCounter;
        frame.x = brain.getBot().getX();
        frame.y = brain.getBot().getY();
        frame.z = brain.getBot().getZ();
        frame.yaw = brain.getBot().yRot;
        frame.pitch = brain.getBot().xRot;
        frame.state = brain.getState().name();
        frames.add(frame);
    }

    public static synchronized boolean stopRecording() {
        if (!recording) return false;
        recording = false;
        recordingBotName = null;
        recordingBotUuid = null;
        return true;
    }

    public static boolean isRecording() { return recording; }
    public static int getFrameCount() { return frames.size(); }

    // ========== PLAYBACK ==========

    public static synchronized boolean startPlayback(String botName) {
        if (playing || frames.isEmpty()) return false;
        BotBrain brain = BotManager.findBrainByName(botName);
        if (brain == null) return false;
        playing = true;
        playbackBotName = botName;
        playbackBotUuid = brain.getBot().getUUID();
        playbackIndex = 0;
        playbackTickCounter = 0;
        return true;
    }

    public static synchronized void playbackTick() {
        if (!playing || playbackIndex >= frames.size()) {
            if (playing) stopPlayback();
            return;
        }

        BotBrain brain = BotManager.BRAINS.get(playbackBotUuid);
        if (brain == null || !brain.getBot().isAlive()) {
            stopPlayback();
            return;
        }

        playbackTickCounter++;
        RecordedFrame frame = frames.get(playbackIndex);
        if (playbackTickCounter >= frame.tick) {
            // Teleport to recorded position
            brain.getBot().setPos(frame.x, frame.y, frame.z);
            brain.getBot().yRot = frame.yaw;
            brain.getBot().xRot = frame.pitch;
            playbackIndex++;
        }
    }

    public static synchronized boolean stopPlayback() {
        if (!playing) return false;
        playing = false;
        playbackBotName = null;
        playbackBotUuid = null;
        playbackIndex = 0;
        playbackTickCounter = 0;
        return true;
    }

    public static boolean isPlaying() { return playing; }
    public static int getPlaybackProgress() { return playbackIndex; }

    // ========== PERSISTENCE ==========

    public static void saveRecording(String name) {
        Path dir = FabricLoader.getInstance().getConfigDir().resolve("carpetplayers-recordings");
        try { Files.createDirectories(dir); } catch (IOException e) { return; }
        File file = dir.resolve(name + ".json").toFile();
        try (Writer w = new FileWriter(file)) {
            GSON.toJson(frames, w);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean loadRecording(String name) {
        Path file = FabricLoader.getInstance().getConfigDir()
                .resolve("carpetplayers-recordings").resolve(name + ".json");
        if (!Files.exists(file)) return false;
        try (Reader r = Files.newBufferedReader(file)) {
            Type type = new TypeToken<List<RecordedFrame>>(){}.getType();
            List<RecordedFrame> loaded = GSON.fromJson(r, type);
            if (loaded != null && !loaded.isEmpty()) {
                frames.clear();
                frames.addAll(loaded);
                return true;
            }
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    public static List<String> listRecordings() {
        Path dir = FabricLoader.getInstance().getConfigDir().resolve("carpetplayers-recordings");
        File[] files = dir.toFile().listFiles((d, n) -> n.endsWith(".json"));
        List<String> names = new ArrayList<>();
        if (files != null) {
            for (File f : files) {
                names.add(f.getName().replace(".json", ""));
            }
        }
        return names;
    }

    private BotRecorder() {}
}
