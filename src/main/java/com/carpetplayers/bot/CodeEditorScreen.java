package com.carpetplayers.bot;

import com.carpetplayers.config.ModConfig;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Formatting;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * In-game configuration editor with syntax highlighting and autocomplete.
 * Only available on Fabric client.
 */
public class CodeEditorScreen extends Screen {

    private static final ResourceLocation ARROW_LEFT = new ResourceLocation("minecraft", "icons/guis/container/inventory.png");
    private static final ResourceLocation ARROW_RIGHT = new ResourceLocation("minecraft", "icons/guis/container/inventory.png");

    private final String filename;
    private String fileContent;
    private String displayContent;
    private int cursorPos;
    private int selectionStart;
    private int selectionEnd;
    private int scrollOffset;
    private boolean modified;

    // Syntax highlighting colors
    private static final Formatting KEY_COLOR = Formatting.AQUA;
    private static final Formatting STRING_COLOR = Formatting.GREEN;
    private static final Formatting NUMBER_COLOR = Formatting.GOLD;
    private static final Formatting BOOLEAN_COLOR = Formatting.LIGHT_PURPLE;
    private static final Formatting NULL_COLOR = Formatting.GRAY;
    private static final Formatting BRACKET_COLOR = Formatting.WHITE;
    private static final Formatting ERROR_COLOR = Formatting.RED;
    private static final Formatting DEFAULT_COLOR = Formatting.WHITE;

    private List<String> tabSuggestions;
    private int tabSuggestionIndex;
    private String partialKey;
    private boolean expectingValue;
    private int caretBlink;
    private boolean caretVisible;

    public CodeEditorScreen(String filename) {
        super(new Component("Config Editor"));
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
            Path path = ModConfig.configFile.toPath().getParent().resolve("carpetplayers-config.json");
            // Actually load from the correct config path
            File configDir = ModConfig.configFile.getParentFile();
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
            JsonObject json = JsonParser.parseString(fileContent).getAsJsonObject();
            // Simple tab completion: show next valid key
            if (!partialKey.isEmpty()) {
                for (String key : json.keySet()) {
                    if (key.startsWith(partialKey)) {
                        tabSuggestions.add(key);
                    }
                }
            } else {
                tabSuggestions.addAll(json.keySet());
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
                result.append(Formatting.YELLOW).append(input.charAt(i));
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
            JsonObject obj = JsonParser.parseString(fileContent).getAsJsonObject();
            return obj.has(key);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // Render background
        renderBackground(guiGraphics);

        // Render file content with syntax highlighting
        String highlighted = applySyntaxHighlighting(displayContent);

        // Calculate visible area
        int width = getWidth();
        int height = getHeight();
        int textWidth = Math.max(highlighted.length() * 8, 200);
        int visibleLines = (height - 40) / 10;

        // Render scrollable text
        int y = 10;
        int lineCount = 0;
        String[] lines = highlighted.split("\n");
        for (String line : lines) {
            if (lineCount >= scrollOffset && lineCount < scrollOffset + visibleLines) {
                int lineY = y + (lineCount - scrollOffset) * 10;
                // Render line number prefix
                if (lineCount < 1000) {
                    guiGraphics.drawString(formattedLineNumber(lineCount + 1), 5, lineY, Formatting.DARK_GRAY.getColor());
                }
                // Render the line content
                guiGraphics.drawString(line, 20, lineY, Formatting.WHITE.getColor());

                // Highlight selection
                if (selectionStart >= 0 && selectionEnd >= 0) {
                    int selStart = Math.min(cursorPos, selectionEnd);
                    int selEnd = Math.max(cursorPos, selectionEnd);
                    // Simple selection highlighting
                }
            }
            lineCount++;
        }

        // Render cursor
        if (caretVisible) {
            int cursorX = 20 + Math.max(0, cursorPos - scrollOffset) * 8;
            int cursorY = 10 + (cursorPos - scrollOffset) * 10;
            guiGraphics.drawString("|", cursorX, cursorY, Formatting.WHITE.getColor());
        }

        // Render UI controls
        renderButtons(guiGraphics, mouseX, mouseY);
    }

    private String formattedLineNumber(int line) {
        return Formatting.DARK_GRAY + String.valueOf(line) + ": ";
    }

    private void renderButtons(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int buttonY = getHeight() - 50;
        int buttonWidth = 60;

        // Save button
        Button saveBtn = new Button(getWidth() / 2 - 100 - 5 - buttonWidth, buttonY, buttonWidth, 20,
                "Save", onClick -> {
            saveFile();
            modified = false;
            close();
        });
        addRenderableWidget(saveBtn);

        // Cancel button
        Button cancelBtn = new Button(getWidth() / 2 + 5, buttonY, buttonWidth, 20,
                "Cancel", onClick -> close());
        addRenderableWidget(cancelBtn);

        // Reload button
        Button reloadBtn = new Button(getWidth() / 2 - 100 - 5 - buttonWidth, buttonY + 25, buttonWidth, 20,
                "Reload", onClick -> {
            displayContent = loadFileContent(filename);
            if (displayContent != null) {
                modified = false;
                calculateTabSuggestions();
            }
        });
        addRenderableWidget(reloadBtn);
    }

    private void saveFile() {
        try {
            File configDir = ModConfig.configFile.getParentFile();
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
            close();
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
            if (mouseX > 15 && mouseX < getWidth() - 10 && mouseY > 10 && mouseY < getHeight() - 50) {
                int clickedLine = (int) ((mouseY - 10 - scrollOffset) / 10);
                String highlighted = applySyntaxHighlighting(displayContent);
                String[] lines = highlighted.split("\n");
                if (clickedLine < lines.length && clickedLine >= 0) {
                    // Calculate cursor position based on click
                    int charOffset = (int) (mouseX - 20) / 8;
                    int charPos = 0;
                    for (int i = 0; i < clickedLine.length() && charOffset > 0; i++) {
                        char c = lines[clickedLine].charAt(i);
                        if (c == '\t') charOffset -= 2;
                        else charOffset--;
                    }
                    cursorPos = charPos + clickedLine.length();
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY) {
        scrollOffset = Math.max(0, Math.min(scrollOffset + (int) mouseY, 
            Math.max(0, countLines(displayContent) - ((getHeight() - 40) / 10))));
        return true;
    }

    private int countLines(String text) {
        int count = 1;
        for (char c : text.toCharArray()) {
            if (c == '\n') count++;
        }
        return count;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        tick();
    }
}

/**
 * Simple widget for the code editor screen.
 */
class CodeEditorWidget extends Button {
    // Minimal widget - main editing happens in CodeEditorScreen
    public CodeEditorWidget(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress);
    }
}