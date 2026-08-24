package com.carpetplayers.bot;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * In-game configuration editor with syntax highlighting and autocomplete.
 * Only available on Fabric client.
 */
public class CodeEditorScreen extends Screen {

    private final String filename;
    private String fileContent;
    private String displayContent;
    private int cursorPos;
    private int selectionStart;
    private int selectionEnd;
    private int scrollOffset;
    private boolean modified;

    // Syntax highlighting colors
    private static final ChatFormatting KEY_COLOR = ChatFormatting.AQUA;
    private static final ChatFormatting STRING_COLOR = ChatFormatting.GREEN;
    private static final ChatFormatting NUMBER_COLOR = ChatFormatting.GOLD;
    private static final ChatFormatting BOOLEAN_COLOR = ChatFormatting.LIGHT_PURPLE;
    private static final ChatFormatting NULL_COLOR = ChatFormatting.GRAY;
    private static final ChatFormatting BRACKET_COLOR = ChatFormatting.WHITE;
    private static final ChatFormatting ERROR_COLOR = ChatFormatting.RED;
    private static final ChatFormatting DEFAULT_COLOR = ChatFormatting.WHITE;

    private List<String> tabSuggestions;
    private int tabSuggestionIndex;
    private String partialKey;
    private boolean expectingValue;
    private int caretBlink;
    private boolean caretVisible;

    public CodeEditorScreen(String filename) {
        super(new TextComponent("Config Editor"));
        this.filename = filename;
        this.fileContent = loadFileContent(filename);
        this.displayContent = this.fileContent != null ? this.fileContent : "";
        this.cursorPos = 0;
        this.selectionStart = -1;
        this.selectionEnd = -1;
        this.scrollOffset = 0;
        this.modified = false;
        this.tabSuggestions = new ArrayList<>();
        this.tabSuggestionIndex = 0;
        this.partialKey = "";
        this.expectingValue = false;
        this.caretBlink = 0;
        this.caretVisible = true;
    }

    private String loadFileContent(String filename) {
        try {
            Path path = FabricLoader.getInstance().getConfigDir().resolve("carpetplayers-config.json");
            // Actually load from the correct config path
            File configDir = FabricLoader.getInstance().getConfigDir().toFile();
            File file = new File(configDir, filename);
            if (!file.exists()) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }
            return sb.toString();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void init() {
        tabSuggestions.clear();
        tabSuggestionIndex = 0;
        partialKey = "";
        expectingValue = false;
        caretBlink = 0;
        caretVisible = true;
        calculateTabSuggestions();
    }

    private void calculateTabSuggestions() {
        tabSuggestions.clear();
        try {
            JsonObject json = (JsonObject) new JsonParser().parse(fileContent);
            // Simple tab completion: show next valid key
            if (!partialKey.isEmpty()) {
                for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                    if (entry.getKey().startsWith(partialKey)) {
                        tabSuggestions.add(entry.getKey());
                    }
                }
            } else {
                for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                    tabSuggestions.add(entry.getKey());
                }
            }
        } catch (Exception e) {
            // If JSON parse fails, show empty
        }
    }

    @Override
    public void tick() {
        caretBlink++;
        if (caretBlink % 30 == 0) {
            caretVisible = !caretVisible;
        }
        // Auto-save every 5 seconds if modified
        if (modified && System.currentTimeMillis() % 5000 < 100) {
            saveFile();
            modified = false;
        }
    }

    private String applySyntaxHighlighting(String input) {
        // Simple syntax highlighting - tokenize and apply formatting
        StringBuilder result = new StringBuilder();
        int i = 0;
        int len = input.length();

        while (i < len) {
            // Handle quotes and strings
            if (input.charAt(i) == '"') {
                // Find end of string
                int stringStart = i;
                i++;
                while (i < len && input.charAt(i) != '"') {
                    if (input.charAt(i) == '\\' && i + 1 < len) {
                        i += 2;
                    } else {
                        i++;
                    }
                }
                if (i < len) {
                    result.append(STRING_COLOR).append(input.substring(stringStart, i + 1));
                    i++;
                }
                continue;
            }

            // Handle brackets
            if (i < len && "\"[{".indexOf(input.charAt(i)) >= 0) {
                result.append(BRACKET_COLOR).append(input.charAt(i));
                i++;
                continue;
            }
            if (i < len && "}]".indexOf(input.charAt(i)) >= 0) {
                result.append(BRACKET_COLOR).append(input.charAt(i));
                i++;
                continue;
            }

            // Handle commas
            if (i < len && input.charAt(i) == ',') {
                result.append(ERROR_COLOR).append(input.charAt(i));
                i++;
                continue;
            }

            // Handle colons
            if (i < len && input.charAt(i) == ':') {
                result.append(ChatFormatting.YELLOW).append(input.charAt(i));
                i++;
                continue;
            }

            // Handle true/false/null
            if (i + 4 <= len && input.substring(i, i + 4).equalsIgnoreCase("true")) {
                result.append(BOOLEAN_COLOR).append(input.substring(i, i + 4));
                i += 4;
                continue;
            }
            if (i + 5 <= len && input.substring(i, i + 5).equalsIgnoreCase("false")) {
                result.append(BOOLEAN_COLOR).append(input.substring(i, i + 5));
                i += 5;
                continue;
            }
            if (i + 4 <= len && input.substring(i, i + 4).equalsIgnoreCase("null")) {
                result.append(NULL_COLOR).append(input.substring(i, i + 4));
                i += 4;
                continue;
            }

            // Handle numbers (simple)
            if (Character.isDigit(input.charAt(i)) || (i + 1 < len && input.substring(i, i + 2).equals("-"))) {
                int numStart = i;
                if (input.charAt(i) == '-') i++;
                while (i < len && (Character.isDigit(input.charAt(i)) || input.charAt(i) == '.')) {
                    i++;
                }
                result.append(NUMBER_COLOR).append(input.substring(numStart, i));
                continue;
            }

            // Default: keys and other text
            int keyStart = i;
            while (i < len && input.charAt(i) != '"' && input.charAt(i) != ',' && input.charAt(i) != ':' && input.charAt(i) != '{' && input.charAt(i) != '}' && input.charAt(i) != ' ' && input.charAt(i) != '\t') {
                i++;
            }
            String word = input.substring(keyStart, i);
            if (jsonKeyExists(word)) {
                result.append(KEY_COLOR).append(word);
            } else {
                result.append(DEFAULT_COLOR).append(word);
            }
        }

        return result.toString();
    }

    private boolean jsonKeyExists(String key) {
        try {
            JsonObject obj = (JsonObject) new JsonParser().parse(fileContent);
            return obj.has(key);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        // Render background
        renderBackground(poseStack);

        // Render file content with syntax highlighting
        String highlighted = applySyntaxHighlighting(displayContent);

        // Calculate visible area
        int w = this.width;
        int h = this.height;
        int textWidth = Math.max(highlighted.length() * 8, 200);
        int visibleLines = (h - 40) / 10;

        // Render scrollable text
        int y = 10;
        int lineCount = 0;
        String[] lines = highlighted.split("\n");
        for (String line : lines) {
            if (lineCount >= scrollOffset && lineCount < scrollOffset + visibleLines) {
                int lineY = y + (lineCount - scrollOffset) * 10;
                // Render line number prefix
                if (lineCount < 1000) {
                    font.draw(poseStack, formattedLineNumber(lineCount + 1), 5, lineY, ChatFormatting.DARK_GRAY.getColor());
                }
                // Render the line content
                font.draw(poseStack, line, 20, lineY, ChatFormatting.WHITE.getColor());
            }
            lineCount++;
        }

        // Render cursor
        if (caretVisible) {
            int cursorX = 20 + Math.max(0, cursorPos - scrollOffset) * 8;
            int cursorY = 10 + (cursorPos - scrollOffset) * 10;
            font.draw(poseStack, "|", cursorX, cursorY, ChatFormatting.WHITE.getColor());
        }

        // Render UI controls
        renderButtons(poseStack, mouseX, mouseY);
    }

    private String formattedLineNumber(int line) {
        return ChatFormatting.DARK_GRAY + String.valueOf(line) + ": ";
    }

    private void renderButtons(PoseStack poseStack, int mouseX, int mouseY) {
        int buttonY = this.height - 50;
        int buttonWidth = 60;

        // Save button
        Button saveBtn = new Button(this.width / 2 - 100 - 5 - buttonWidth, buttonY, buttonWidth, 20,
                new TextComponent("Save"), onClick -> {
            saveFile();
            modified = false;
            onClose();
        });
        addButton(saveBtn);

        // Cancel button
        Button cancelBtn = new Button(this.width / 2 + 5, buttonY, buttonWidth, 20,
                new TextComponent("Cancel"), onClick -> onClose());
        addButton(cancelBtn);

        // Reload button
        Button reloadBtn = new Button(this.width / 2 - 100 - 5 - buttonWidth, buttonY + 25, buttonWidth, 20,
                new TextComponent("Reload"), onClick -> {
            displayContent = loadFileContent(filename);
            if (displayContent != null) {
                modified = false;
                calculateTabSuggestions();
            }
        });
        addButton(reloadBtn);
    }

    private void saveFile() {
        try {
            File configDir = FabricLoader.getInstance().getConfigDir().toFile();
            File file = new File(configDir, filename);
            Files.write(file.toPath(), displayContent.getBytes());
            modified = false;
        } catch (IOException e) {
            // Show error
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // Escape
            onClose();
            return true;
        }
        if (keyCode == 261) { // Up
            // Move cursor up
            return true;
        }
        if (keyCode == 262) { // Down
            // Move cursor down
            return true;
        }
        if (keyCode == 263) { // Left
            if (cursorPos > 0) {
                cursorPos--;
                if (selectionEnd > 0) {
                    selectionEnd = Math.min(selectionEnd - 1, cursorPos);
                }
            }
            return true;
        }
        if (keyCode == 264) { // Right
            // Move cursor right
            return true;
        }
        if (keyCode == 265) { // Tab
            // Tab completion
            if (!tabSuggestions.isEmpty()) {
                partialKey = tabSuggestions.get(tabSuggestionIndex % tabSuggestions.size());
                tabSuggestionIndex++;
            }
            return true;
        }
        if (keyCode == 335) { // Enter
            // Handle enter - could commit a suggestion
            return true;
        }

        // Regular character input
        if (keyCode >= 0 && keyCode < 128) {
            char typedChar = (char) keyCode;
            if (typedChar >= 32 && typedChar <= 126) {
                // Insert character
                String text = displayContent;
                int before = cursorPos;
                displayContent = displayContent.substring(0, cursorPos) + typedChar + displayContent.substring(cursorPos + 1);
                cursorPos = before + 1;
                modified = true;
                calculateTabSuggestions();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (chr >= 32 && chr <= 126) {
            String text = displayContent;
            int before = cursorPos;
            displayContent = displayContent.substring(0, cursorPos) + chr + displayContent.substring(cursorPos + 1);
            cursorPos = before + 1;
            modified = true;
            calculateTabSuggestions();
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (mouseButton == 0) {
            // Check if clicked on text area
            if (mouseX > 15 && mouseX < this.width - 10 && mouseY > 10 && mouseY < this.height - 50) {
                int clickedLineNum = (int) ((mouseY - 10 - scrollOffset) / 10);
                String highlighted = applySyntaxHighlighting(displayContent);
                String[] lines = highlighted.split("\n");
                if (clickedLineNum < lines.length && clickedLineNum >= 0) {
                    // Calculate cursor position based on click
                    int charOffset = (int) (mouseX - 20) / 8;
                    int charPos = 0;
                    for (int i = 0; i < lines[clickedLineNum].length() && charOffset > 0; i++) {
                        char c = lines[clickedLineNum].charAt(i);
                        if (c == '\t') charOffset -= 2;
                        else charOffset--;
                    }
                    cursorPos = charPos + lines[clickedLineNum].length();
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scrollOffset = Math.max(0, Math.min(scrollOffset + (int) -delta,
            Math.max(0, countLines(displayContent) - ((this.height - 40) / 10))));
        return true;
    }

    private int countLines(String text) {
        int count = 1;
        for (char c : text.toCharArray()) {
            if (c == '\n') count++;
        }
        return count;
    }

}

/**
 * Simple widget for the code editor screen.
 */
class CodeEditorWidget extends Button {
    // Minimal widget - main editing happens in CodeEditorScreen
    public CodeEditorWidget(int x, int y, int width, int height, TextComponent message, OnPress onPress) {
        super(x, y, width, height, message, onPress);
    }
}