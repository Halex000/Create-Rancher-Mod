package com.halex.createrancher;

import com.halex.createrancher.index.PartialModelRegistry;
import com.halex.createrancher.ponder.CreateRancherPonderPlugin;

import net.createmod.ponder.foundation.PonderIndex;

// Client setup when the mod loads
public class CreateRancherClient {

    public static void init() {
        PartialModelRegistry.register();
        PonderIndex.addPlugin(new CreateRancherPonderPlugin());
    }
}
