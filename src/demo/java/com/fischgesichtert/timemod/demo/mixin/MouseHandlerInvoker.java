package com.fischgesichtert.timemod.demo.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** GLFW meldet selbst gesetzte Mauspositionen unter X11 nicht zurück – also direkt einspeisen. */
@Mixin(MouseHandler.class)
public interface MouseHandlerInvoker {
    @Invoker("onMove")
    void timemod$onMove(long window, double x, double y);
}
