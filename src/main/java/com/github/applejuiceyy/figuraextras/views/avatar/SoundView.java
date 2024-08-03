package com.github.applejuiceyy.figuraextras.views.avatar;

import com.github.applejuiceyy.figuraextras.FiguraExtras;
import com.github.applejuiceyy.figuraextras.components.SoundComponent;
import com.github.applejuiceyy.figuraextras.ducks.SoundBufferAccess;
import com.github.applejuiceyy.figuraextras.ducks.SoundEngineAccess;
import com.github.applejuiceyy.figuraextras.mixin.SoundBufferAccessor;
import com.github.applejuiceyy.figuraextras.mixin.figura.lua.LuaSoundAccessor;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.ParentElement;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.Surface;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Button;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Elements;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Label;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Flow;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Grid;
import com.github.applejuiceyy.figuraextras.util.Differential;
import com.github.applejuiceyy.figuraextras.util.Lifecycle;
import com.github.applejuiceyy.figuraextras.views.View;
import com.mojang.blaze3d.audio.Channel;
import com.mojang.blaze3d.audio.Library;
import com.mojang.blaze3d.audio.OggAudioStream;
import com.mojang.blaze3d.audio.SoundBuffer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.phys.Vec3;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.ducks.ChannelHandleAccessor;
import org.figuramc.figura.lua.api.sound.LuaSound;
import org.figuramc.figura.lua.api.sound.SoundAPI;
import org.lwjgl.openal.AL10;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.stream.Collectors;

public class SoundView implements Lifecycle {

    private final View.Context<Avatar> context;

    private final Differential<Map.Entry<String, SoundBuffer>, String, CustomSoundInstance> customSoundsDifferential;
    private final Differential<Map.Entry<String, List<LuaSound>>, String, VanillaSoundInstance> vanillaSoundsDifferential;

    public SoundView(View.Context<Avatar> context, ParentElement.AdditionPoint additionPoint) {
        this.context = context;
        FiguraExtras.showSoundPositions.compute(context.getValue().owner, (k, v) -> v == null ? 1 : ++v);

        Grid grid = new Grid();
        grid.rows().percentage(7).percentage(3).cols().percentage(1);

        Flow customSoundsLayout = new Flow();
        grid.add(Elements.withVerticalScroll(customSoundsLayout, true));

        Flow vanillaSoundsLayout = new Flow();
        grid.add(Elements.withVerticalScroll(vanillaSoundsLayout, true)).setRow(1);
        additionPoint.accept(grid);

        customSoundsDifferential = new Differential<>(
                context.getValue().customSounds.entrySet(),
                Map.Entry::getKey,
                thing -> {
                    CustomSoundInstance inst = new CustomSoundInstance(thing.getKey(), thing.getValue(), context);
                    customSoundsLayout.add(inst.root);
                    return inst;
                },
                customSoundInstance -> {
                    customSoundsLayout.remove(customSoundInstance.root);
                    customSoundInstance.dispose();
                }
        );

        vanillaSoundsDifferential = new Differential<>(
                () -> ((SoundEngineAccess) SoundAPI.getSoundEngine()).figuraExtrass$getFiguraHandles()
                        .stream()
                        .filter(sound -> {
                            ChannelHandleAccessor accessor = (ChannelHandleAccessor) sound.getHandle();
                            return ((LuaSoundAccessor) sound).getSound() != null &&
                                    sound.isPlaying() && accessor != null && accessor.getOwner().equals(context.getValue().owner);
                        })
                        .collect(Collectors.groupingBy(LuaSound::getId))
                        .entrySet()
                        .iterator(),
                Map.Entry::getKey,
                thing -> {
                    VanillaSoundInstance inst = new VanillaSoundInstance(thing.getKey(), thing.getValue().size(), context);
                    vanillaSoundsLayout.add(inst.root);
                    return inst;
                },
                vanillaSoundInstance -> {
                    vanillaSoundsLayout.remove(vanillaSoundInstance.root);
                    vanillaSoundInstance.dispose();
                }
        );
    }

    @Override
    public void tick() {
        customSoundsDifferential.update(CustomSoundInstance::tick);
    }

    @Override
    public void render() {
        customSoundsDifferential.update(CustomSoundInstance::render);
        vanillaSoundsDifferential.update((inst, v) -> inst.setCount(v.getValue().size()));
    }

    @Override
    public void dispose() {
        customSoundsDifferential.dispose();
        FiguraExtras.showSoundPositions.compute(context.getValue().owner, (k, v) -> v == null || v == 1 ? null : --v);
    }

    static class CustomSoundInstance {
        private final View.Context<Avatar> context;
        private final SoundBuffer sound;
        private final Label label;
        private final Label currentPlayingSounds;
        private final String name;
        private final MutableComponent stopComponent = net.minecraft.network.chat.Component.literal("Stop");
        private final MutableComponent playComponent = net.minecraft.network.chat.Component.literal("Play");
        public Flow root;
        public long millisPlay = 0;
        SoundComponent soundComponent;
        ParentElement<Grid.GridSettings> button;
        boolean needUpdate = false;
        ChannelAccess.ChannelHandle handle = null;

        public CustomSoundInstance(String name, SoundBuffer sound, View.Context<Avatar> context) {
            this.context = context;
            this.sound = sound;
            this.name = name;

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

            Grid grid = new Grid();

            grid.rows().content().cols().content().fixed(20).content();

            grid.add(button);

            currentPlayingSounds = new Label();
            grid.add(currentPlayingSounds).setColumn(2);

            root.add(grid);

        }

        public ByteBuffer searchForByteBuffer(SoundBuffer sound, String key) {
            ByteBuffer buffer = ((SoundBufferAccess) sound).figuraExtrass$getKeptBuffer();
            if (buffer != null) {
                return buffer;
            }
            if (!context.getValue().nbt.contains("sounds"))
                return null;

            CompoundTag root = context.getValue().nbt.getCompound("sounds");
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
                boolean nowUpdate = soundComponent.sampleOffset > 0 && soundComponent.sampleOffset < soundComponent.sampleEnding + soundComponent.getWidth() * 10;
                if (needUpdate || nowUpdate) {
                    soundComponent.enqueueDirtySection(false, false);
                }

                needUpdate = nowUpdate;
            }

            int sounds = 0;
            for (LuaSound luaSound : ((SoundEngineAccess) SoundAPI.getSoundEngine()).figuraExtrass$getFiguraHandles()) {
                if (((LuaSoundAccessor) luaSound).getBuffer() == null) continue; // it's a vanilla sound
                ChannelHandleAccessor accessor = (ChannelHandleAccessor) luaSound.getHandle();
                if (luaSound.isPlaying() && accessor != null && accessor.getOwner().equals(context.getValue().owner) && luaSound.getId().equals(name)) {
                    sounds++;
                }
            }

            currentPlayingSounds.setText(Component.literal(sounds + " playing").withStyle(style -> style.withColor(0xffff5500)));
        }
    }

    static class VanillaSoundInstance {
        private final Label count;
        public Grid root;

        public VanillaSoundInstance(String name, int count, View.Context<Avatar> context) {

            root = new Grid();
            root.rows().content().cols().content().percentage(1).content();
            root.setSurface(Surface.contextBackground());

            this.count = new Label();
            setCount(count);

            root.add(name);
            root.add(this.count).setColumn(2);
        }

        public void setCount(int count) {
            this.count.setText(Component.literal(count + " playing").withStyle(style -> style.withColor(0xffff5500)));
        }

        public void dispose() {
        }
    }
}
