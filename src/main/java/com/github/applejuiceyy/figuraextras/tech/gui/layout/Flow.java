package com.github.applejuiceyy.figuraextras.tech.gui.layout;

import com.github.applejuiceyy.figuraextras.tech.gui.basics.Element;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.ParentElement;
import net.minecraft.util.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.BooleanSupplier;

public class Flow extends ParentElement<ParentElement.Settings> {
    List<Element> elementOrder = new ArrayList<>();

    @Override
    protected Settings constructSettings(BooleanSupplier willCauseReflow, Runnable reflowDetached) {
        return new Settings(willCauseReflow, reflowDetached, this);
    }

    @Override
    public Settings add(Element element) {
        elementOrder.add(element);
        return super.add(element);
    }

    @Override
    public void remove(Element element) {
        elementOrder.remove(element);
        super.remove(element);
    }

    @Override
    public void positionElements(Iterable<Tuple<Element, Settings>> elements) {

        int maxWidth = getWidth();
        if (!doConstrainX()) {
            for (Tuple<Element, Settings> tuple : elements) {
                Settings settings = tuple.getB();
                Element element = tuple.getA();
                int width = settings.isOptimalWidth() ? element.getOptimalWidth() : settings.getWidth();
                maxWidth = Math.max(maxWidth, width);
            }
        }

        // can't properly obey doConstrainY
        xViewSize.set(maxWidth);
        int finalHeight = 0;
        for (Tuple<Element, Settings> tuple : elements) {
            Settings settings = tuple.getB();
            Element element = tuple.getA();
            int width = settings.isOptimalWidth() ? maxWidth : settings.getWidth();
            finalHeight += settings.isOptimalHeight() ? element.getOptimalHeight(width) : settings.getHeight();
        }

        yViewSize.set(finalHeight);

        int y = 0;
        for (Element element : elementOrder) {
            Settings settings = getSettings(element);
            int width = settings.isOptimalWidth() ? maxWidth : settings.getWidth();
            int height = settings.isOptimalHeight() ? element.getOptimalHeight(width) : settings.getHeight();
            element.setX(getX() - xView.get());
            element.setY(getY() + y - yView.get());
            element.setWidth(width);
            element.setHeight(height);
            y += height;
        }
    }

    public Element getAtPosition(int position) {
        return elementOrder.get(position);
    }

    public OptionalInt getPosition(Element element) {
        if (hasElement(element)) {
            return OptionalInt.of(elementOrder.indexOf(element));
        }
        return OptionalInt.empty();
    }

    public void insertIntoPosition(int pos, Element element) {
        if (hasElement(element)) {
            elementOrder.remove(element);
            elementOrder.add(pos, element);
            childrenChanged();
        }
    }

    @Override
    public int computeOptimalWidth() {
        int maxWidth = getWidth();
        for (Tuple<Element, Settings> tuple : flowElements(true)) {
            Settings settings = tuple.getB();
            Element element = tuple.getA();
            int width = settings.isOptimalWidth() ? element.getOptimalWidth() : settings.getWidth();
            maxWidth = Math.max(maxWidth, width);
        }
        return maxWidth;
    }

    @Override
    public int computeOptimalHeight(int width) {
        int finalHeight = 0;
        for (Tuple<Element, Settings> tuple : flowElements(true)) {
            Settings settings = tuple.getB();
            Element element = tuple.getA();
            finalHeight += settings.isOptimalHeight() ?
                    element.getOptimalHeight(settings.isOptimalWidth() ? width : settings.getWidth()) : settings.getHeight();
        }
        return finalHeight;
    }
}
