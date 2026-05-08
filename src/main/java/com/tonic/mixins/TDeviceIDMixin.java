package com.tonic.mixins;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.injector.annotations.Inject;
import com.tonic.injector.annotations.MethodOverride;
import com.tonic.injector.annotations.Mixin;
import com.tonic.injector.annotations.Shadow;
import com.tonic.model.DeviceID;
import com.tonic.util.ReflectBuilder;

import java.util.UUID;

@Mixin("PlatformInfo")
public abstract class TDeviceIDMixin
{
    @Shadow("JX_CHARACTER_ID")
    public static String characterId;

    @MethodOverride("getHardwareUUID")
    public String getHardwareUUID(int os)
    {
        return process(os);
    }

    @Inject
    private static String process(int os)
    {
        if (!Static.getVitaConfig().shouldCacheDeviceId())
        {
            return DeviceID.vanillaGetDeviceID(os);
        }

        String username = ReflectBuilder.of(Static.getClient())
                .method("getUsername", null, null)
                .get();

        String identifier = username != null && !username.isEmpty() ? username : characterId;

        String cachedDeviceId = DeviceID.getCachedUUID(identifier);
        if (cachedDeviceId == null)
        {
            cachedDeviceId = UUID.randomUUID().toString();
            DeviceID.writeCachedUUID(identifier, cachedDeviceId);
            Logger.info("Generated new deviceId (UUID): " + cachedDeviceId + " for account: " + identifier);
        }
        Logger.info("Using cached deviceId (UUID): " + cachedDeviceId + " for account: " + identifier);
        return cachedDeviceId;
    }
}
