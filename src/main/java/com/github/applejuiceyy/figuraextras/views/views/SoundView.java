package com.github.applejuiceyy.figuraextras.views.views;

import com.github.applejuiceyy.figuraextras.components.SoundComponent;
import com.github.applejuiceyy.figuraextras.ducks.SoundBufferAccess;
import com.github.applejuiceyy.figuraextras.ducks.SoundEngineAccess;
import com.github.applejuiceyy.figuraextras.mixin.SoundBufferAccessor;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.Element;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.ParentElement;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.Surface;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Button;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Elements;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Label;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Scrollbar;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Flow;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Grid;
import com.github.applejuiceyy.figuraextras.views.InfoViews;
import com.mojang.blaze3d.audio.Channel;
import com.mojang.blaze3d.audio.Library;
import com.mojang.blaze3d.audio.OggAudioStream;
import com.mojang.blaze3d.audio.SoundBuffer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.phys.Vec3;
import org.figuramc.figura.lua.api.sound.SoundAPI;
import org.lwjgl.openal.AL10;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

public class SoundView implements InfoViews.View {
    private final Flow layout;
    private final Grid root;
    private final InfoViews.Context context;

    private final HashMap<SoundBuffer, Instance> textures = new HashMap<>();

    public SoundView(InfoViews.Context context) {
        this.context = context;

        root = new Grid();
        root.rows()
                .content()
                .cols()
                .content()
                .content();

        layout = new Flow();
        Scrollbar scrollbar = new Scrollbar();
        Elements.makeVerticalContainerScrollable(layout, scrollbar, true);
    }

    @Override
    public void tick() {
        ArrayList<SoundBuffer> seen = new ArrayList<>();
        for (Map.Entry<String, SoundBuffer> texture : context.getAvatar().customSounds.entrySet()) {
            if (!textures.containsKey(texture.getValue())) {
                Instance inst = new Instance(texture.getKey(), texture.getValue(), context);
                layout.add(inst.root);
                textures.put(texture.getValue(), inst);
                seen.add(texture.getValue());
            }
            seen.add(texture.getValue());
        }

        for (Iterator<Map.Entry<SoundBuffer, Instance>> iterator = textures.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<SoundBuffer, Instance> figuraTextureTextureComponentEntry = iterator.next();
            if (!seen.contains(figuraTextureTextureComponentEntry.getKey())) {
                iterator.remove();
                figuraTextureTextureComponentEntry.getValue().dispose();
            } else {
                figuraTextureTextureComponentEntry.getValue().tick();
            }
        }
    }

    @Override
    public Element getRoot() {
        return root;
    }

    @Override
    public void render() {
        for (Iterator<Map.Entry<SoundBuffer, Instance>> iterator = textures.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<SoundBuffer, Instance> figuraTextureTextureComponentEntry = iterator.next();
            figuraTextureTextureComponentEntry.getValue().render();
        }
    }

    @Override
    public void dispose() {
        for (Instance value : textures.values()) {
            value.dispose();
        }
    }

    static class Instance {
        private final InfoViews.Context context;
        private final SoundBuffer sound;
        private final Label label;
        public Flow root;
        SoundComponent soundComponent;
        public long millisPlay = 0;
        ParentElement<Grid.GridSettings> button;

        ChannelAccess.ChannelHandle handle = null;
        private final MutableComponent stopComponent = net.minecraft.network.chat.Component.literal("Stop");
        private final MutableComponent playComponent = net.minecraft.network.chat.Component.literal("Play");

        public Instance(String name, SoundBuffer sound, InfoViews.Context context) {
            this.context = context;
            this.sound = sound;
            root = new Flow();

            root.setSurface(Surface.contextBackground());

            root.add(name);

            StringBuilder info = new StringBuilder();
            int channels;
            float sampleRate;
            info.append(channels = ((SoundBufferAccessor) sound).getAudioFormat().getChannels());
            info.append(" channels, ");
            info.append(((SoundBufferAccessor) sound).getAudioFormat().getSampleSizeInBits());
            info.append("bit sample size, ");
            info.append(sampleRate = ((SoundBufferAccessor) sound).getAudioFormat().getSampleRate());
            info.append(" samples per second, ");
            OptionalInt buffer = ((SoundBufferAccessor) sound).invokeGetAlBuffer();
            if (buffer.isEmpty()) {
                info.append("unknown");
            } else {
                int bufferSize = AL10.alGetBufferi(buffer.getAsInt(), AL10.AL_SIZE);
                int normalisedChannel = bufferSize / channels;
                float seconds = normalisedChannel / sampleRate / ((SoundBufferAccessor) sound).getAudioFormat().getSampleSizeInBits() * 8;
                info.append(seconds);
            }
            info.append(" seconds");


            root.add(net.minecraft.network.chat.Component.literal("   (" + info + ")").withStyle(ChatFormatting.GRAY));

            ByteBuffer possibleBB = searchForByteBuffer(sound, name);

            if (possibleBB != null) {
                soundComponent = new SoundComponent(
                        possibleBB,
                        ((SoundBufferAccessor) sound).getAudioFormat()
                );
                root.add(soundComponent);
            }

            this.label = new Label();
            button = Button.minimal().addAnd(label);
            button.activation.subscribe(event -> {
                if (handle != null && !handle.isStopped()) {
                    if (soundComponent != null) {
                        long millisSince = System.currentTimeMillis() - millisPlay;
                        float perSecond = millisSince / 1000f;
                        soundComponent.sampleEnding = (int) (perSecond * ((SoundBufferAccessor) sound).getAudioFormat().getSampleRate());
                    }
                    handle.execute(Channel::stop);
                    return;
                }
                millisPlay = System.currentTimeMillis();
                if (soundComponent != null) {
                    soundComponent.sampleEnding = soundComponent.sampleCount();
                }
                handle = ((SoundEngineAccess) SoundAPI.getSoundEngine()).figuraExtrass$createHandle(Library.Pool.STATIC);
                handle.execute(channel -> {
                    channel.attachStaticBuffer(sound);
                    channel.setPitch(1);
                    channel.setVolume(1);
                    channel.disableAttenuation();
                    channel.setLooping(false);
                    channel.setSelfPosition(Vec3.ZERO);
                    channel.setRelative(true);
                    channel.play();
                });
            });

            root.add(button);
        }

        public ByteBuffer searchForByteBuffer(SoundBuffer sound, String key) {
            ByteBuffer buffer = ((SoundBufferAccess) sound).figuraExtrass$getKeptBuffer();
            if (buffer != null) {
                return buffer;
            }
            if (!context.getAvatar().nbt.contains("sounds"))
                return null;

            CompoundTag root = context.getAvatar().nbt.getCompound("sounds");
            if (!root.contains(key)) {
                return null;
            }
            byte[] bytes = root.getByteArray(key);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            OggAudioStream oggAudioStream;
            try {
                oggAudioStream = new OggAudioStream(inputStream);
            } catch (IOException e) {
                return null;
            }
            try {
                ByteBuffer buff = oggAudioStream.readAll();
                inputStream.close();
                oggAudioStream.close();
                return buff;
            } catch (IOException e) {
                return null;
            }
        }

        public void dispose() {
        }

        public void tick() {
            if (handle == null || handle.isStopped()) {
                label.setText(playComponent);
            } else {
                label.setText(stopComponent);
            }
        }

        public void render() {
            if (soundComponent != null) {
                long millisSince = System.currentTimeMillis() - millisPlay;
                float perSecond = millisSince / 1000f;
                soundComponent.sampleOffset = (int) (perSecond * ((SoundBufferAccessor) sound).getAudioFormat().getSampleRate());
            }
        }
    }
}
