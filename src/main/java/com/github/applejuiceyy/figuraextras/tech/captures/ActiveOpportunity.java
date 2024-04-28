package com.github.applejuiceyy.figuraextras.tech.captures;

public record ActiveOpportunity<T extends Hook>(PossibleCapture opportunity, T thing) {
}
