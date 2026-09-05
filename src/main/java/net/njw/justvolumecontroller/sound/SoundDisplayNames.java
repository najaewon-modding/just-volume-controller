package net.njw.justvolumecontroller.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Arrays;

public final class SoundDisplayNames {
    private static final String TRANSLATION_PREFIX = "sound_tree.njw_just_volume_controller.";

    private SoundDisplayNames() {
    }

    public static boolean shouldDisplay(SoundTree.Node node, String nodeKey) {
        return node.soundId().isPresent() || categoryLabel(node, nodeKey) != null;
    }

    public static String label(SoundTree.Node node, String nodeKey) {
        if (node.soundId().isPresent()) {
            String subtitle = subtitle(node.soundId().get());

            if (subtitle != null) {
                return subtitle;
            }
        }

        String categoryLabel = categoryLabel(node, nodeKey);
        return categoryLabel == null
                ? node.soundId().map(Identifier::toString).orElse(node.segment())
                : categoryLabel;
    }

    public static String searchText(SoundTree.Node node, String nodeKey) {
        String categoryLabel = categoryLabel(node, nodeKey);
        String subtitle = node.soundId().map(SoundDisplayNames::subtitle).orElse(null);

        if (categoryLabel == null) {
            return subtitle == null ? "" : subtitle;
        }

        return subtitle == null ? categoryLabel : subtitle + " " + categoryLabel;
    }

    private static String categoryLabel(SoundTree.Node node, String nodeKey) {
        int colon = nodeKey.indexOf(':');
        String namespace = colon < 0 ? nodeKey : nodeKey.substring(0, colon);
        String path = colon < 0 ? "" : nodeKey.substring(colon + 1);

        if (path.isEmpty()) {
            String namespaceKey = TRANSLATION_PREFIX + "namespace." + namespace;
            return I18n.exists(namespaceKey) ? I18n.get(namespaceKey) : node.segment();
        }

        String exactKey = TRANSLATION_PREFIX + "path." + namespace + "." + path;

        if (I18n.exists(exactKey)) {
            return I18n.get(exactKey);
        }

        String[] segments = path.split("\\.");

        if (segments.length == 1) {
            String categoryKey = TRANSLATION_PREFIX + "category." + segments[0];
            return I18n.exists(categoryKey) ? I18n.get(categoryKey) : null;
        }

        return registeredPathLabel(namespace, segments);
    }

    private static String subtitle(Identifier soundId) {
        WeighedSoundEvents soundEvent = Minecraft.getInstance().getSoundManager().getSoundEvent(soundId);

        if (soundEvent == null) {
            return null;
        }

        Component subtitle = soundEvent.getSubtitle();

        if (subtitle == null) {
            return null;
        }

        String translated = subtitle.getString().trim();
        return translated.isEmpty() || translated.startsWith("subtitles.") ? null : translated;
    }

    private static boolean isContentType(String segment) {
        return segment.equals("block") || segment.equals("entity") || segment.equals("item");
    }

    private static String registeredLabel(String namespace, String contentType, String contentId) {
        if (isContentType(contentType)) {
            return translatedContent(contentType, namespace, contentId);
        }

        if (contentType.equals("ambient") || contentType.equals("music")) {
            return translatedContent("biome", namespace, contentId);
        }

        if (contentType.equals("music_disc")) {
            return translatedContent("item", namespace, "music_disc_" + contentId);
        }

        if (contentType.equals("ui")) {
            String blockLabel = translatedContent("block", namespace, contentId);
            return blockLabel == null ? translatedContent("item", namespace, contentId) : blockLabel;
        }

        return null;
    }

    private static String registeredPathLabel(String namespace, String[] segments) {
        for (int start = 1; start < segments.length; start++) {
            String contentId = String.join("_", Arrays.copyOfRange(segments, start, segments.length));
            String label = registeredLabel(namespace, segments[0], contentId);

            if (label != null) {
                return label;
            }
        }

        return null;
    }

    private static String translatedContent(String contentType, String namespace, String contentId) {
        String key = contentType + "." + namespace + "." + contentId;
        return I18n.exists(key) ? I18n.get(key) : null;
    }
}
