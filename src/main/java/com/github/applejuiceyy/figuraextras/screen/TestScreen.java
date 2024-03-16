package com.github.applejuiceyy.figuraextras.screen;

import com.github.applejuiceyy.figuraextras.tech.gui.basics.GuiState;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Button;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Elements;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Scrollbar;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Flow;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Grid;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class TestScreen extends Screen {

    private final GuiState awWrapper;

    public TestScreen(Component title) {
        super(title);

        Grid root = new Grid();

        root.addRow(1, Grid.SpacingKind.CONTENT)
                .addColumn(1, Grid.SpacingKind.CONTENT)
                .addColumn(1, Grid.SpacingKind.CONTENT);

        Button button = new Button();
        root.add(button).setColumn(0).setRow(0);

        Flow flow = new Flow();
        flow.add("Long Text");
        flow.add("So many text oh my god");
        flow.add(new Button().addAnd("random button why not"));
        flow.add("big scrollbar");
        Scrollbar scrolle = new Scrollbar();
        flow.add(scrolle).setOptimalWidth(false).setWidth(10);
        flow.add("Engaging lorem ipsum");
        flow.add("""


                Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nunc volutpat libero nec nisl molestie, at dignissim nisl commodo. Quisque eget rhoncus dui, vel aliquam nisl. Quisque laoreet nibh urna, sed hendrerit libero egestas tempus. Sed nec magna in ex aliquet maximus nec at urna. Duis convallis lacinia mattis. Vivamus vel augue ac ante posuere condimentum in in ante. Donec venenatis scelerisque leo, et pretium ex feugiat vel. Pellentesque congue condimentum diam, suscipit lobortis mi bibendum in. Nunc vitae enim felis.

                Donec fermentum mollis arcu nec finibus. Ut velit enim, consectetur ac mauris vitae, posuere eleifend neque. Nullam gravida neque et nulla fringilla, vitae cursus elit venenatis. Phasellus faucibus semper odio. Phasellus ullamcorper dui in libero accumsan, nec pharetra urna rutrum. Nunc sit amet ipsum ullamcorper, lacinia nisi non, viverra sapien. Nunc feugiat ligula vel elit imperdiet congue. Morbi non rhoncus ligula. Donec at gravida justo. Ut et nunc eleifend, consequat leo luctus, elementum mauris. Pellentesque sit amet iaculis justo, id sollicitudin risus. Aenean eu erat vitae elit vestibulum gravida. Pellentesque ut arcu dolor. In leo ex, malesuada vehicula eros vitae, fringilla malesuada dolor. Proin ut orci in purus finibus convallis tempor nec nisl.

                Nam vel consectetur sem. Nulla feugiat lectus id ex consectetur, et vehicula urna bibendum. Pellentesque porttitor elit sit amet sapien auctor bibendum. Nam commodo euismod laoreet. Aliquam tincidunt, risus at lobortis pretium, lectus dolor vestibulum lectus, at placerat mauris ipsum et tortor. Vivamus pretium risus id sem cursus, vel blandit elit posuere. Integer non commodo leo, a semper orci. Vestibulum laoreet imperdiet mauris ac condimentum. Nulla sed condimentum sapien. Nullam congue nibh sem, eu mollis nisi faucibus dictum. Vestibulum ac dolor mi. Duis luctus commodo commodo. Nam leo turpis, aliquet ut fringilla eu, pellentesque eget magna.

                Nunc ultricies sagittis mattis. Donec dictum eros ac lectus placerat congue. Mauris bibendum justo quis ipsum accumsan lobortis. Nunc et lectus arcu. Nunc facilisis turpis vel erat venenatis cursus. Integer dictum magna nibh, ac iaculis mauris blandit non. Vestibulum et tempus felis. Mauris eu libero eu augue posuere aliquet vel vitae diam. Curabitur ac nisi eu enim vehicula pulvinar quis et sapien. Ut sit amet nisl purus. Praesent sollicitudin varius augue. Proin ut odio congue, fermentum felis vitae, scelerisque purus. Aenean pretium ultricies sapien, nec laoreet leo malesuada id. Vestibulum rutrum sapien sapien, sed rutrum ante volutpat et.

                Nam mollis dignissim enim vitae rutrum. Interdum et malesuada fames ac ante ipsum primis in faucibus. Cras dignissim tellus a finibus consectetur. Integer aliquet, tortor nec volutpat cursus, nisi ex finibus diam, et porta tellus ligula sit amet tellus. Duis ut posuere urna, ac dapibus nunc. Phasellus molestie turpis et odio egestas aliquam. Pellentesque magna lorem, hendrerit id odio non, ultricies molestie lacus. Maecenas id felis massa. Maecenas venenatis eleifend porttitor. Duis in justo ex. Nullam scelerisque turpis semper arcu lobortis mattis. Vivamus quam metus, ultricies sit amet massa vitae, dignissim congue nulla.\s""");
        flow.add(new Button().addAnd("Donate 1000€"));
        button.add(flow);

        Scrollbar scroll = new Scrollbar();
        root.add(scroll).setColumn(1).setRow(0);

        Elements.makeContainerScrollable(button, scrolle, null);
        Elements.makeContainerScrollable(button, scroll, null);

        awWrapper = root.getState();
    }

    @Override
    protected void init() {
        addRenderableWidget(awWrapper);
        awWrapper.setWidth(width);
        awWrapper.setHeight(height);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        this.getChildAt(mouseX, mouseY).ifPresent(element -> element.mouseMoved(mouseX, mouseY));
    }
}
