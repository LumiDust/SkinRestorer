package net.lionarius.skinrestorer.skin.provider.builtin;

import com.mojang.authlib.*;
import com.mojang.authlib.services.MinecraftServicesDiscoveryService;
import com.mojang.authlib.services.response.NameAndId;
import com.mojang.authlib.services.response.discovery.Service;
import com.mojang.util.UndashedUuid;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.config.provider.CacheConfig;
import net.lionarius.skinrestorer.exception.TransparentException;
import net.lionarius.skinrestorer.skin.provider.SkinProviderContext;
import net.lionarius.skinrestorer.skin.provider.base.YggdrasilSkinProvider;
import net.lionarius.skinrestorer.util.WebUtils;
import net.minecraft.server.players.CachedUserNameToIdResolver;

import java.io.IOException;
import java.net.Proxy;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;

public final class MojangSkinProvider extends YggdrasilSkinProvider {
    public static final String PROVIDER_NAME = "mojang";
    public static final String PROFILE_CACHE_FILENAME = "mojang_profile_cache.json";

    private final MinecraftServicesDiscoveryService discoveryService;
    private final CachedUserNameToIdResolver profileCache;

    public MojangSkinProvider() {
        this.discoveryService = MinecraftServicesDiscoveryService.create(Proxy.NO_PROXY);
        this.profileCache = new CachedUserNameToIdResolver(
                new GameProfileRepository() {
                    @Override
                    public void findProfilesByNames(String[] names, ProfileLookupCallback callback) {
                        for (var name : names) {
                            try {
                                var profile = MojangSkinProvider.this.getProfile(name);
                                callback.onProfileLookupSucceeded(profile.name(), profile.id());
                            } catch (IOException e) {
                                throw new TransparentException(e);
                            }
                        }
                    }

                    @Override
                    public Optional<NameAndId> findProfileByName(String name) {
                        try {
                            var profile = MojangSkinProvider.this.getProfile(name);
                            return Optional.of(new NameAndId(profile.id(), profile.name()));
                        } catch (IOException e) {
                            throw new TransparentException(e);
                        }
                    }
                },
                SkinRestorer.getConfigDir().resolve(PROFILE_CACHE_FILENAME).toFile());
    }

    @Override
    public String getProviderName() {
        return MojangSkinProvider.PROVIDER_NAME;
    }

    @Override
    public void reload() {
        this.createSkinCache();
    }

    @Override
    protected CacheConfig getCacheConfig() {
        return SkinRestorer.getConfig().providers().mojang().cache();
    }

    public static SkinProviderContext skinProviderContextFromProfile(GameProfile gameProfile) {
        return new SkinProviderContext(MojangSkinProvider.PROVIDER_NAME, gameProfile.name(), null);
    }

    @Override
    protected UUID resolveUuid(String username) throws Exception {
        var cachedProfile = this.profileCache.get(username);
        if (cachedProfile.isEmpty()) throw new IllegalArgumentException("no profile found for " + username);

        return cachedProfile.get().id();
    }

    @Override
    protected URI profileByIdUri(UUID uuid) {
        return URI.create(this.discoveryService
                .getUrl(Service.SESSION, "getProfileById")
                .replace("{profileId}", UndashedUuid.toString(uuid)));
    }

    @Override
    protected URI profileByNameUri(String name) {
        return URI.create(this.discoveryService
                .getUrl(Service.PROFILES, "getByName")
                .replace("{name}", WebUtils.urlEncode(name)));
    }
}
