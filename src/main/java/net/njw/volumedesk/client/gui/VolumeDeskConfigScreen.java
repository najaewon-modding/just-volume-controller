package net.njw.volumedesk.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.njw.volumedesk.VolumeDesk;
import net.njw.volumedesk.config.SoundVolumeConfig;
import net.njw.volumedesk.sound.SoundCatalog;
import net.njw.volumedesk.sound.SoundDisplayNames;
import net.njw.volumedesk.sound.SoundTree;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class VolumeDeskConfigScreen extends Screen {
    private static final Component TITLE = Component.translatable("screen.njw_volume_desk.config.title");
    private static final Component SEARCH = Component.translatable("screen.njw_volume_desk.config.search")
            .withStyle(EditBox.SEARCH_HINT_STYLE);
    private static final Component RESET_ALL = Component.translatable("screen.njw_volume_desk.config.reset_all");

    private final Screen parent;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 54, 33);
    private final Set<String> expandedNodes = new HashSet<>();
    private SoundTree soundTree;
    private SoundList soundList;
    private EditBox searchBox;
    private boolean dirty;

    public VolumeDeskConfigScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        EditBox previousSearchBox = this.searchBox;

        if (this.soundTree == null) {
            this.soundTree = SoundCatalog.capture();
            this.soundTree.namespaces().forEach(node -> this.expandedNodes.add(node.segment() + ":"));
        }

        LinearLayout header = this.layout.addToHeader(LinearLayout.vertical().spacing(4));
        header.addChild(
                new StringWidget(
                        Component.translatable("screen.njw_volume_desk.config.sound_count", this.soundTree.soundCount()),
                        this.font
                ),
                LayoutSettings::alignHorizontallyCenter
        );

        this.searchBox = header.addChild(
                new EditBox(
                        this.font,
                        0,
                        0,
                        Math.min(320, this.width - 40),
                        20,
                        previousSearchBox,
                        SEARCH
                ),
                LayoutSettings::alignHorizontallyCenter
        );
        this.searchBox.setHint(SEARCH);

        this.soundList = new SoundList();
        this.searchBox.setResponder(this.soundList::updateSearch);
        this.soundList.updateSearch(this.searchBox.getValue());
        this.layout.addToContents(this.soundList);

        LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        footer.addChild(Button.builder(RESET_ALL, button -> this.resetAll()).width(120).build());
        footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose()).width(120).build());

        this.layout.visitWidgets(widget -> this.addRenderableWidget(widget));
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();

        if (this.soundList != null) {
            this.soundList.updateSize(this.width, this.layout);
        }
    }

    @Override
    protected void setInitialFocus() {
        this.setInitialFocus(this.searchBox);
    }

    @Override
    public void onClose() {
        if (this.dirty) {
            VolumeDesk.soundVolumes().save();
            this.dirty = false;
        }

        this.minecraft.setScreen(this.parent);
    }

    private void resetAll() {
        if (!VolumeDesk.soundVolumes().getChangedVolumes().isEmpty()) {
            VolumeDesk.soundVolumes().resetAll();
            this.dirty = true;
            this.soundList.refreshEntries();
            this.refreshActiveSounds();
        }
    }

    private void updateVolume(SoundTree.Node node, int percent) {
        node.soundId().ifPresent(soundId -> {
            SoundVolumeConfig config = VolumeDesk.soundVolumes();

            if (config.getVolumePercent(soundId) != percent) {
                config.setVolumePercent(soundId, percent);
                this.dirty = true;
                this.refreshActiveSounds();
            }
        });
    }

    private void refreshActiveSounds() {
        this.minecraft.getSoundManager().refreshCategoryVolume(SoundSource.MASTER);
    }

    private final class SoundList extends ContainerObjectSelectionList<SoundEntry> {
        private static final int ROW_HEIGHT = 22;
        private List<String> searchTerms = List.of();

        private SoundList() {
            super(
                    Minecraft.getInstance(),
                    VolumeDeskConfigScreen.this.width,
                    VolumeDeskConfigScreen.this.layout.getContentHeight(),
                    VolumeDeskConfigScreen.this.layout.getHeaderHeight(),
                    ROW_HEIGHT
            );
        }

        @Override
        public int getRowWidth() {
            return Math.min(440, VolumeDeskConfigScreen.this.width - 40);
        }

        private void updateSearch(String value) {
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            this.searchTerms = normalized.isEmpty()
                    ? List.of()
                    : List.of(normalized.split("\\s+"));
            this.rebuild(true);
        }

        private void refreshEntries() {
            this.rebuild(false);
        }

        private void toggle(String key) {
            if (!VolumeDeskConfigScreen.this.expandedNodes.remove(key)) {
                VolumeDeskConfigScreen.this.expandedNodes.add(key);
            }

            this.rebuild(false);
        }

        private void rebuild(boolean resetScroll) {
            double previousScroll = this.scrollAmount();
            this.setFocused(null);
            this.clearEntries();

            for (SoundTree.Node namespace : VolumeDeskConfigScreen.this.soundTree.namespaces()) {
                String key = namespace.segment() + ":";
                String searchPath = SoundDisplayNames.searchText(namespace, key);
                this.appendNode(namespace, 0, key, searchPath, false);
            }

            this.refreshScrollAmount();
            this.setScrollAmount(resetScroll ? 0.0 : previousScroll);
        }

        private void appendNode(
                SoundTree.Node node,
                int depth,
                String key,
                String searchPath,
                boolean ancestorMatches
        ) {
            boolean selfMatches = !this.searchTerms.isEmpty() && this.matchesSelf(key, searchPath);

            if (!this.searchTerms.isEmpty()
                    && !ancestorMatches
                    && !selfMatches
                    && !this.matchesDescendant(node, key, searchPath)) {
                return;
            }

            boolean expanded = !this.searchTerms.isEmpty() || VolumeDeskConfigScreen.this.expandedNodes.contains(key);
            String label = SoundDisplayNames.label(node, key);
            this.addEntry(new SoundEntry(node, depth, key, label, expanded, this.searchTerms.isEmpty()));

            if (expanded) {
                for (SoundTree.Node child : node.children()) {
                    String childKey = this.childKey(key, child);
                    this.appendNode(
                            child,
                            depth + 1,
                            childKey,
                            searchPath + " " + SoundDisplayNames.searchText(child, childKey),
                            ancestorMatches || selfMatches
                    );
                }
            }
        }

        private boolean matchesDescendant(SoundTree.Node node, String key, String searchPath) {
            for (SoundTree.Node child : node.children()) {
                String childKey = this.childKey(key, child);
                String childPath = searchPath + " " + SoundDisplayNames.searchText(child, childKey);

                if (this.matchesSelf(childKey, childPath)
                        || this.matchesDescendant(child, childKey, childPath)) {
                    return true;
                }
            }

            return false;
        }

        private boolean matchesSelf(String key, String searchPath) {
            String searchable = (key + " " + searchPath).toLowerCase(Locale.ROOT);
            return this.searchTerms.stream().allMatch(searchable::contains);
        }

        private String childKey(String parentKey, SoundTree.Node child) {
            return parentKey + (parentKey.endsWith(":") ? "" : ".") + child.segment();
        }
    }

    private final class SoundEntry extends ContainerObjectSelectionList.Entry<SoundEntry> {
        private static final int INDENT_WIDTH = 12;
        private static final int TOGGLE_WIDTH = 18;
        private static final int VOLUME_WIDTH = 44;
        private static final int INVALID_TEXT_COLOR = 0xFFFF5555;

        private final SoundTree.Node node;
        private final int depth;
        private final String localizedLabel;
        private final List<AbstractWidget> widgets = new ArrayList<>();
        private final Button toggleButton;
        private final NumericEditBox volumeBox;

        private SoundEntry(
                SoundTree.Node node,
                int depth,
                String key,
                String localizedLabel,
                boolean expanded,
                boolean allowToggle
        ) {
            this.node = node;
            this.depth = depth;
            this.localizedLabel = localizedLabel;

            if (!node.children().isEmpty() && allowToggle) {
                this.toggleButton = Button.builder(
                        Component.literal(expanded ? "-" : "+"),
                        button -> VolumeDeskConfigScreen.this.soundList.toggle(key)
                ).bounds(0, 0, TOGGLE_WIDTH, TOGGLE_WIDTH).build();
                this.widgets.add(this.toggleButton);
            } else {
                this.toggleButton = null;
            }

            if (node.soundId().isPresent()) {
                this.volumeBox = new NumericEditBox(
                        VolumeDeskConfigScreen.this.font,
                        VOLUME_WIDTH,
                        TOGGLE_WIDTH,
                        Component.translatable(
                                "screen.njw_volume_desk.config.volume",
                                localizedLabel
                        )
                );
                this.volumeBox.setMaxLength(3);
                this.volumeBox.setValue(Integer.toString(
                        VolumeDesk.soundVolumes().getVolumePercent(node.soundId().get())
                ));
                this.volumeBox.setResponder(this::updateVolume);
                this.widgets.add(this.volumeBox);
            } else {
                this.volumeBox = null;
            }
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return this.widgets;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return this.widgets;
        }

        @Override
        public void extractContent(
                GuiGraphicsExtractor graphics,
                int mouseX,
                int mouseY,
                boolean hovered,
                float partialTick
        ) {
            int y = this.getContentY();
            int branchX = this.getContentX() + Math.min(this.depth, 12) * INDENT_WIDTH;

            if (this.toggleButton != null) {
                this.toggleButton.setX(branchX);
                this.toggleButton.setY(y);
                this.toggleButton.extractRenderState(graphics, mouseX, mouseY, partialTick);
            }

            int labelX = branchX + TOGGLE_WIDTH + 4;
            String label = this.node.children().isEmpty()
                    ? this.localizedLabel
                    : this.localizedLabel + " (" + this.node.soundCount() + ")";
            int volumeX = this.volumeBox == null
                    ? this.getContentRight()
                    : this.getContentRight() - VOLUME_WIDTH - 12;
            int availableLabelWidth = Math.max(0, volumeX - labelX - 4);
            String displayedLabel = label;

            if (VolumeDeskConfigScreen.this.font.width(label) > availableLabelWidth) {
                int ellipsisWidth = VolumeDeskConfigScreen.this.font.width("...");
                displayedLabel = VolumeDeskConfigScreen.this.font.plainSubstrByWidth(
                        label,
                        Math.max(0, availableLabelWidth - ellipsisWidth)
                ) + "...";
            }

            graphics.text(
                    VolumeDeskConfigScreen.this.font,
                    displayedLabel,
                    labelX,
                    y + 5,
                    this.node.soundId().isPresent() ? EditBox.DEFAULT_TEXT_COLOR : 0xFFFFFFFF
            );

            if (this.volumeBox != null) {
                this.volumeBox.setX(volumeX);
                this.volumeBox.setY(y);
                this.volumeBox.extractRenderState(graphics, mouseX, mouseY, partialTick);
                graphics.text(
                        VolumeDeskConfigScreen.this.font,
                        "%",
                        this.getContentRight() - 8,
                        y + 5,
                        0xFFFFFFFF
                );

                if (hovered && mouseX >= labelX && mouseX < volumeX) {
                    graphics.setTooltipForNextFrame(
                            Component.literal(this.node.soundId().get().toString()),
                            mouseX,
                            mouseY
                    );
                }
            }
        }

        private void updateVolume(String value) {
            try {
                int percent = Integer.parseInt(value);

                if (percent < SoundVolumeConfig.MIN_VOLUME_PERCENT
                        || percent > SoundVolumeConfig.MAX_VOLUME_PERCENT) {
                    this.volumeBox.setTextColor(INVALID_TEXT_COLOR);
                    return;
                }

                this.volumeBox.setTextColor(EditBox.DEFAULT_TEXT_COLOR);
                VolumeDeskConfigScreen.this.updateVolume(this.node, percent);
            } catch (NumberFormatException exception) {
                this.volumeBox.setTextColor(INVALID_TEXT_COLOR);
            }
        }
    }

    private static final class NumericEditBox extends EditBox {
        private NumericEditBox(
                Font font,
                int width,
                int height,
                Component narration
        ) {
            super(font, width, height, narration);
        }

        @Override
        public void insertText(String input) {
            if (input.codePoints().allMatch(codePoint -> codePoint >= '0' && codePoint <= '9')) {
                super.insertText(input);
            }
        }

        @Override
        public boolean charTyped(CharacterEvent event) {
            int codePoint = event.codepoint();
            return codePoint >= '0' && codePoint <= '9' && super.charTyped(event);
        }
    }
}
