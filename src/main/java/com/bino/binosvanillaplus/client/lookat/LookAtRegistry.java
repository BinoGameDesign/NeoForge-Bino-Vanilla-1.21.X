package com.bino.binosvanillaplus.client.lookat;

import java.util.ArrayList;
import java.util.List;

public class LookAtRegistry {
    public static final List<LookAtProvider> PROVIDERS = new ArrayList<>();

    static {
        PROVIDERS.add(new BasicProviders.Tool());
        PROVIDERS.add(new BasicProviders.Crop());
        PROVIDERS.add(new BasicProviders.Health());
    }
}
