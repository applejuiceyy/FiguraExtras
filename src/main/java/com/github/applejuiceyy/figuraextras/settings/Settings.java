package com.github.applejuiceyy.figuraextras.settings;

import java.nio.file.Path;

public interface Settings {
    Settings INSTANCE = SettingsScaffoldBuilder.build(Path.of("./config/figuraextras.json").toFile(), Settings.class);


    default String[] getDefaultSelectedPostProcessors() {
        return new String[0];
    }

    void setDefaultSelectedPostProcessors(String[] postProcessors);

    SettingsValue<String[]> defaultSelectedPostProcessors();
}
