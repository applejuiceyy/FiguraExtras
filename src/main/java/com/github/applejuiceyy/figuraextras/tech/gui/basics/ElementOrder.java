package com.github.applejuiceyy.figuraextras.tech.gui.basics;

import com.mojang.datafixers.util.Either;
import org.jetbrains.annotations.NotNull;

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
        List<Node> list = reorder(root);
        list.add(new Node(-9999, Either.right(root)));
        tree = list;
        order = list.stream().flatMap(new Function<Node, Stream<Element>>() {
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

    private List<Node> reorder(Element element) {
        List<Node> e = new ArrayList<>();
        reorder(element, e);
        e.sort(Comparator.comparingInt(Node::priority).reversed());
        return e;
    }

    private void reorder(Element element, List<Node> isolationContext) {
        if (element instanceof ParentElement<?> parentElement) {
            for (Element child : parentElement.getElements()) {
                ParentElement.Settings settings = parentElement.getSettings(child);

                if (settings.isolatePriority() || parentElement.getSurface().usesChildren()) {
                    List<Node> list = reorder(child);
                    isolationContext.add(new Node(settings.getPriority(), Either.left(new Inner(list, parentElement))));
                } else {
                    reorder(child, isolationContext);
                }

                isolationContext.add(new Node(settings.getPriority(), Either.right(child)));
            }
        }
    }

    record Node(int priority, Either<Inner, Element> node) {
    }

    record Inner(List<Node> children, ParentElement<?> owner) {
    }
}
