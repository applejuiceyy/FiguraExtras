package com.github.applejuiceyy.figuraextras.tech.captures;

public record ActiveOpportunity<T extends SecondaryCallHook>(CaptureOpportunity opportunity, T thing) {
}
