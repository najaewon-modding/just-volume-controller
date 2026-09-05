package net.njw.justvolumecontroller.sound;

import net.minecraft.resources.Identifier;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;

public final class SoundTree {
    private final List<Node> namespaces;
    private final int soundCount;

    private SoundTree(List<Node> namespaces, int soundCount) {
        this.namespaces = List.copyOf(namespaces);
        this.soundCount = soundCount;
    }

    public static SoundTree from(Collection<Identifier> soundIds) {
        var uniqueSoundIds = new TreeSet<>(Objects.requireNonNull(soundIds));
        var namespaces = new TreeMap<String, MutableNode>();

        for (Identifier soundId : uniqueSoundIds) {
            MutableNode node = namespaces.computeIfAbsent(soundId.getNamespace(), MutableNode::new);

            for (String segment : soundId.getPath().split("\\.")) {
                node = node.children.computeIfAbsent(segment, MutableNode::new);
            }

            node.soundId = soundId;
        }

        return new SoundTree(
                namespaces.values().stream().map(MutableNode::freeze).toList(),
                uniqueSoundIds.size()
        );
    }

    public List<Node> namespaces() {
        return namespaces;
    }

    public int soundCount() {
        return soundCount;
    }

    public record Node(String segment, Optional<Identifier> soundId, List<Node> children, int soundCount) {
        public Node {
            Objects.requireNonNull(segment);
            Objects.requireNonNull(soundId);
            children = List.copyOf(children);
        }

        public boolean representsSound() {
            return soundId.isPresent();
        }
    }

    private static final class MutableNode {
        private final String segment;
        private final Map<String, MutableNode> children = new TreeMap<>();
        private Identifier soundId;

        private MutableNode(String segment) {
            this.segment = segment;
        }

        private Node freeze() {
            List<Node> frozenChildren = children.values().stream().map(MutableNode::freeze).toList();
            int soundCount = soundId == null ? 0 : 1;

            for (Node child : frozenChildren) {
                soundCount += child.soundCount();
            }

            return new Node(segment, Optional.ofNullable(soundId), frozenChildren, soundCount);
        }
    }
}
