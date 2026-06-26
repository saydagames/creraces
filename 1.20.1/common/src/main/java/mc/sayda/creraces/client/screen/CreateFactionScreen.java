package mc.sayda.creraces.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

@SuppressWarnings("null")
public class CreateFactionScreen extends Screen {

    private EditBox nameBox;
    private Button confirmButton;
    private Button cancelButton;

    public CreateFactionScreen() {
        super(Component.translatable("creraces.screen.create_faction"));
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new CreateFactionScreen());
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int cy = height / 2;

        nameBox = new EditBox(font, cx - 75, cy - 20, 150, 20,
                Component.translatable("creraces.screen.faction_name"));
        nameBox.setMaxLength(32);
        addRenderableWidget(nameBox);

        confirmButton = Button.builder(Component.translatable("creraces.screen.confirm"), btn -> confirm())
                .pos(cx - 80, cy + 10).size(75, 20).build();
        addRenderableWidget(confirmButton);

        cancelButton = Button.builder(Component.translatable("gui.cancel"), btn -> onClose())
                .pos(cx + 5, cy + 10).size(75, 20).build();
        addRenderableWidget(cancelButton);
    }

    private void confirm() {
        String name = nameBox.getValue().trim();
        if (name.isEmpty()) return;
        mc.sayda.creraces.network.BoundaryHandler.sendFactionAction(
                new mc.sayda.creraces.network.FactionActionPacket(
                        mc.sayda.creraces.network.FactionActionPacket.Action.CREATE, name));
        onClose();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float dt) {
        renderBackground(g);
        g.drawCenteredString(font, Component.translatable("creraces.screen.create_faction"),
                width / 2, height / 2 - 40, 0xFFFFFF);
        super.render(g, mx, my, dt);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
