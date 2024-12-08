package com.github.applejuiceyy.figuraextras.mixin.figura.gui.wizard;


import com.github.applejuiceyy.figuraextras.constants.Paths;
import com.github.applejuiceyy.figuraextras.ducks.AvatarWizardAccess;
import com.github.applejuiceyy.figuraextras.settings.Settings;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.figuramc.figura.gui.screens.AbstractPanelScreen;
import org.figuramc.figura.gui.screens.AvatarWizardScreen;
import org.figuramc.figura.gui.widgets.Button;
import org.figuramc.figura.gui.widgets.ContextMenu;
import org.figuramc.figura.wizards.AvatarWizard;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Mixin(AvatarWizardScreen.class)
public abstract class AvatarWizardScreenMixin extends Screen {

    @Unique
    ContextMenu menu;
    @Final
    @Shadow
    private AvatarWizard wizard;
    @Unique
    private Button setDefaultButton;

    protected AvatarWizardScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "<init>", at = @At(value = "FIELD", target = "Lorg/figuramc/figura/gui/screens/AvatarWizardScreen;wizard:Lorg/figuramc/figura/wizards/AvatarWizard;", shift = At.Shift.AFTER))
    void initializeContextMenu(AbstractPanelScreen parentScreen, CallbackInfo ci) {
        String[] list = Paths.avatarPostProcessorsDirectory.toFile().list();

        if (list != null && list.length != 0) {
            menu = new ContextMenu();
            menu.setVisible(true);

            List<String> defaultPP = new ArrayList<>(List.of(Settings.INSTANCE.getDefaultSelectedPostProcessors()));
            boolean retained = defaultPP.retainAll(Arrays.asList(list));
            String[] ppArray = defaultPP.toArray(new String[0]);
            if (retained) {
                Settings.INSTANCE.setDefaultSelectedPostProcessors(ppArray);
            }
            ((AvatarWizardAccess) wizard).figuraExtrass$setPostProcessors(ppArray);

            for (String suggestion : list) {
                int i = suggestion.lastIndexOf('.');
                Component name = Component.literal(i == -1 ? suggestion : suggestion.substring(0, i));
                Component greenName = name.copy().withStyle(ChatFormatting.GREEN);
                menu.addAction(defaultPP.contains(suggestion) ? greenName : name, null, o -> {
                    String[] strings = ((AvatarWizardAccess) wizard).figuraExtrass$getPostProcessors();
                    List<String> stringList = Arrays.asList(strings);
                    int index = stringList.indexOf(suggestion);
                    String[] modified;
                    if (index == -1) {
                        modified = new String[strings.length + 1];
                        System.arraycopy(strings, 0, modified, 0, strings.length);
                        modified[modified.length - 1] = suggestion;
                    } else {
                        modified = new String[strings.length - 1];
                        if (index > 0) {
                            System.arraycopy(strings, 0, modified, 0, index);
                        }
                        if (index < modified.length) {
                            System.arraycopy(strings, index + 1, modified, index, modified.length - index);
                        }
                    }
                    o.setMessage(index == -1 ? greenName : name);
                    ((AvatarWizardAccess) wizard).figuraExtrass$setPostProcessors(modified);
                    setDefaultButtonVisibility();
                });
            }

            menu.updateDimensions();
        } else {
            menu = null;
        }
    }

    @Unique
    void setDefaultButtonVisibility() {
        String[] strings = ((AvatarWizardAccess) wizard).figuraExtrass$getPostProcessors();
        String[] defaults = Settings.INSTANCE.getDefaultSelectedPostProcessors();
        setDefaultButton.setVisible(!Arrays.deepEquals(strings, defaults));
    }

    @Inject(method = "init", at = @At(value = "RETURN"))
    void addPostProcessorStuff(CallbackInfo ci, @Local int width) {
        if (menu == null) return;
        int x = (this.width - width) / 2 - menu.getWidth() - 2;
        menu.setX(x);
        menu.setY(30);

        addRenderableWidget(menu);

        setDefaultButton = addRenderableWidget(
                new Button(x, 30 + menu.getHeight(), menu.getWidth(), 12, Component.literal("Set Default"), null, button -> {
                    String[] strings = ((AvatarWizardAccess) wizard).figuraExtrass$getPostProcessors();
                    Settings.INSTANCE.setDefaultSelectedPostProcessors(strings);
                    setDefaultButton.setVisible(false);
                })
        );
        setDefaultButtonVisibility();
    }

    @ModifyArg(method = "lambda$init$1", at = @At(value = "INVOKE", target = "Lorg/figuramc/figura/gui/FiguraToast;sendToast(Ljava/lang/Object;)V"))
    Object cancelFunnyToast(Object title) {
        return ((AvatarWizardAccess) wizard).figuraExtrass$getPostProcessors().length == 0 ? title : "Your avatar is being post-processed";
    }
}
