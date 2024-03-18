package com.github.applejuiceyy.figuraextras.tech.gui.basics;

import com.mojang.datafixers.util.Either;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

public class ElementOrder implements Iterable<Element> {
    List<Element> order = new ArrayList<>();
    List<Node> tree = null;
    List<Element> reverseOrder = new ArrayList<>();

    public List<Node> getTree() {
        return tree;
    }

    @NotNull
    @Override
    public Iterator<Element> iterator() {
        return order.iterator();
    }

    @NotNull
    public Iterable<Node> treeIterator() {
        return () -> tree.iterator();
    }

    public void update(Element root) {
        List<Node> nodes = new ArrayList<>();
        reorder(null, root, nodes);
        nodes.add(new Node(-9999, Either.right(root)));
        nodes.sort(Comparator.comparingInt(Node::priority).reversed());
        tree = nodes;
        order = nodes.stream().flatMap(new Function<Node, Stream<Element>>() {
            @Override
            public Stream<Element> apply(Node node) {
                if (node.node.right().isPresent()) {
                    return Stream.of(node.node.right().get());
                } else if (node.node.left().isPresent()) {
                    return node.node.left().get().children.stream().flatMap(this);
                }
                throw new RuntimeException();
            }
        }).toList();
        reverseOrder = new ArrayList<>(order);
        Collections.reverse(reverseOrder);
    }

    private void reorder(@Nullable ParentElement.Settings settings, Element element, List<Node> isolationContext) {
        if (settings != null && settings.isInvisible()) return;
        boolean addSelf = element.getSurface().usesChildren();
        if (element instanceof ParentElement<?> parentElement) {
            if (addSelf) {
                List<Node> list = new ArrayList<>();
                for (Element child : parentElement.getElements()) {
                    if (parentElement.getSettings(child).isInvisible()) continue;
                    reorder(parentElement.getSettings(child), child, list);
                }
                list.sort(Comparator.comparingInt(Node::priority).reversed());
                isolationContext.add(new Node(settings == null ? 0 : settings.getPriority(), Either.left(new Inner(list, parentElement))));
            } else {

                for (Element child : parentElement.getElements()) {
                    if (parentElement.getSettings(child).isInvisible()) continue;
                    ParentElement.Settings s = parentElement.getSettings(child);
                    reorder(s, child, isolationContext);
                }
            }
        }

        if (!addSelf) {
            isolationContext.add(new Node(settings == null ? 0 : settings.getPriority(), Either.right(element)));
        }
    }

    record Node(int priority, Either<Inner, Element> node) {
    }

    record Inner(List<Node> children, ParentElement<?> owner) {
    }
}
