package net.njw.volumedesk.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SoundDisplayNames {
    private static final String TRANSLATION_PREFIX = "sound_tree.njw_volume_desk.";

    private SoundDisplayNames() {
    }

    public static String label(SoundTree.Node node, String nodeKey) {
        if (node.soundId().isPresent()) {
            String subtitle = subtitle(node.soundId().get());

            if (subtitle != null) {
                return subtitle;
            }
        }

        return structuralLabel(node, nodeKey);
    }

    public static String searchText(SoundTree.Node node, String nodeKey) {
        String structuralLabel = structuralLabel(node, nodeKey);

        if (node.soundId().isEmpty()) {
            return structuralLabel;
        }

        String subtitle = subtitle(node.soundId().get());
        return subtitle == null ? structuralLabel : subtitle + " " + structuralLabel;
    }

    private static String structuralLabel(SoundTree.Node node, String nodeKey) {
        int colon = nodeKey.indexOf(':');
        String namespace = colon < 0 ? nodeKey : nodeKey.substring(0, colon);
        String path = colon < 0 ? "" : nodeKey.substring(colon + 1);

        if (path.isEmpty()) {
            return translateOrFallback(TRANSLATION_PREFIX + "namespace." + namespace, humanize(node.segment()));
        }

        String exactKey = TRANSLATION_PREFIX + "path." + namespace + "." + path;

        if (I18n.exists(exactKey)) {
            return I18n.get(exactKey);
        }

        String[] segments = path.split("\\.");

        String registeredLabel = registeredLabel(namespace, segments[0], node.segment());

        if (registeredLabel != null) {
            return registeredLabel;
        }

        String segmentKey = TRANSLATION_PREFIX + "segment." + node.segment();

        if (I18n.exists(segmentKey)) {
            return I18n.get(segmentKey);
        }

        return compositeLabel(namespace, segments[0], node.segment());
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

    private static String translatedContent(String contentType, String namespace, String contentId) {
        String key = contentType + "." + namespace + "." + contentId;
        return I18n.exists(key) ? I18n.get(key) : null;
    }

    private static String compositeLabel(String namespace, String contentType, String segment) {
        String[] words = segment.split("[_-]");

        if (words.length == 1) {
            return humanize(segment);
        }

        List<String> labels = new ArrayList<>(words.length);

        for (String word : words) {
            String wordKey = TRANSLATION_PREFIX + "segment." + word;

            if (I18n.exists(wordKey)) {
                labels.add(I18n.get(wordKey));
                continue;
            }

            String registeredLabel = registeredLabel(namespace, contentType, word);
            labels.add(registeredLabel == null ? humanize(word) : registeredLabel);
        }

        return String.join(" ", labels);
    }

    private static String translateOrFallback(String key, String fallback) {
        return I18n.exists(key) ? I18n.get(key) : fallback;
    }

    private static String humanize(String segment) {
        return segment.replace('_', ' ').replace('-', ' ').toLowerCase(Locale.ROOT);
    }
}
