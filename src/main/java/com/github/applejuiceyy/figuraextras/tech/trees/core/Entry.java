package com.github.applejuiceyy.figuraextras.tech.trees.core;

import com.github.applejuiceyy.figuraextras.tech.trees.interfaces.ObjectExpander;
import com.github.applejuiceyy.figuraextras.util.Observers;
import net.minecraft.util.Tuple;

import java.util.Optional;

public record Entry<T, K, V>(
        Observers.Observer<Optional<T>> incoming,
        ObjectExpander<T, K, V> responsible,
        Observers.Observer<Optional<Tuple<K, V>>> value) {

}
