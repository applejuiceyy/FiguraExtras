package com.github.applejuiceyy.figuraextras.views.backend;

import com.github.applejuiceyy.figuraextras.ipc.backend.ReceptionistServerBackend;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.ParentElement;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.Surface;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Button;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Spacer;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Flow;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Grid;
import com.github.applejuiceyy.figuraextras.util.Lifecycle;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.figuramc.figura.avatar.Badges;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Date;

public class PlayerInstance implements Lifecycle {
    private final ArrayList<Button> prideButtons;
    private final ArrayList<Button> specialButtons;
    private final ReceptionistServerBackend.BackendUser user;

    public PlayerInstance(ReceptionistServerBackend.BackendUser user, ParentElement.AdditionPoint additionPoint) {
        this.user = user;

        Flow root = new Flow();
        additionPoint.accept(root);

        root.setSurface(Surface.contextBackground());

        root.add(user.getUuid().toString() + " (getting deleted in " + (10 - (new Date().getTime() - user.getUpkeep().getTime()) / 1000 / 60 / 60 / 24) + " days)");

        Grid pride = new Grid();
        prideButtons = new ArrayList<>();
        pride.rows().content();

        pride.addColumn(0, Grid.SpacingKind.CONTENT);
        pride.add("Pride:");

        Badges.Pride[] values = Badges.Pride.values();
        for (int i = 0; i < values.length; i++) {
            Badges.Pride value = values[i];
            Button button = Button.minimal();
            button.setText(value.badge.copy().withStyle(Style.EMPTY.withFont(Badges.FONT)));
            int finalI = i;
            button.activation.getSource().subscribe(evt -> {
                BitSet bitSet = user.getPrideBadges();
                bitSet.set(finalI, !bitSet.get(finalI));
                user.setPrideBadges(bitSet);
                updateButtonColoring();
            });
            pride.addColumn(0, Grid.SpacingKind.CONTENT);
            pride.add(button).setColumn(i + 1);
            prideButtons.add(button);
        }

        Grid special = new Grid();
        specialButtons = new ArrayList<>();
        special.rows().content();

        special.addColumn(0, Grid.SpacingKind.CONTENT);
        special.add("Special:");

        Badges.Special[] specials = Badges.Special.values();
        for (int i = 0; i < specials.length; i++) {
            Badges.Special value = specials[i];
            Button button = Button.minimal();
            button.setText(value.badge.copy().withStyle(Style.EMPTY.withFont(Badges.FONT)));
            int finalI = i;
            button.activation.getSource().subscribe(evt -> {
                BitSet bitSet = user.getSpecialBadges();
                bitSet.set(finalI, !bitSet.get(finalI));
                user.setSpecialBadges(bitSet);
                updateButtonColoring();
            });
            special.addColumn(0, Grid.SpacingKind.CONTENT);
            special.add(button).setColumn(i + 1);
            specialButtons.add(button);
        }

        root.add(pride);
        root.add(special);

        ReceptionistServerBackend.BackendAvatar[] avatars = user.getEquippedAvatars();
        if (avatars.length == 0) {
            root.add("Not equipping any avatar");
        } else {
            root.add("Avatars:");
            Flow avatarFlow = new Flow();
            for (ReceptionistServerBackend.BackendAvatar avatar : avatars) {
                avatarFlow.add(avatar.getId() + " by " + avatar.getOwner());
            }
            root.add(avatarFlow);
        }

        ParentElement<Grid.GridSettings> deleteButton = Button.minimal().addAnd(Component.literal("Delete user").withStyle(ChatFormatting.RED));
        deleteButton.activation.getSource().subscribe(evt -> user.delete());
        ParentElement<Grid.GridSettings> crazierDeleteButton = Button.minimal().addAnd(Component.literal("Delete user and owned avatars").withStyle(ChatFormatting.RED));
        crazierDeleteButton.activation.getSource().subscribe(evt -> {
            for (ReceptionistServerBackend.BackendAvatar uploadedAvatar : user.getUploadedAvatars()) {
                uploadedAvatar.delete();
            }
            user.delete();
        });

        root.add(new Spacer(0, 10));
        root.add(deleteButton);
        root.add(crazierDeleteButton);

        updateButtonColoring();
    }

    void updateButtonColoring() {
        BitSet badges = user.getPrideBadges();
        for (int i = 0; i < prideButtons.size(); i++) {
            Button prideButton = prideButtons.get(i);
            prideButton.setActiveTexture(Surface.solid(badges.get(i) ? 0xff00ff00 : 0xffff0000));
            prideButton.setNormalTexture(Surface.solid(badges.get(i) ? 0xff00aa00 : 0xffaa0000));
        }
        badges = user.getSpecialBadges();
        for (int i = 0; i < specialButtons.size(); i++) {
            Button prideButton = specialButtons.get(i);
            prideButton.setActiveTexture(Surface.solid(badges.get(i) ? 0xff00ff00 : 0xffff0000));
            prideButton.setNormalTexture(Surface.solid(badges.get(i) ? 0xff00aa00 : 0xffaa0000));
        }
    }

    @Override
    public void tick() {

    }

    @Override
    public void render() {

    }

    @Override
    public void dispose() {

    }
}
