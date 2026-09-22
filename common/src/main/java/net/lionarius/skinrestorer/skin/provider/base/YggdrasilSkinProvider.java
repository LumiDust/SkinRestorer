package net.lionarius.skinrestorer.skin.provider.base;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.services.response.MinecraftProfilePropertiesResponse;
import com.mojang.authlib.services.response.NameAndId;
import net.lionarius.skinrestorer.util.JsonUtils;
import net.lionarius.skinrestorer.util.WebUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.util.UUID;

public abstract class YggdrasilSkinProvider extends ProfileSkinProvider {
    protected abstract URI profileByIdUri(UUID uuid);

    protected abstract URI profileByNameUri(String name);

    @Override
    protected UUID lookupUuid(String username) throws Exception {
        return this.getProfile(username).id();
    }

    @Override
    protected GameProfile fetchProfileWithProperties(UUID uuid) throws IOException {
        var request = HttpRequest.newBuilder()
                .uri(URI.create(this.profileByIdUri(uuid) + "?unsigned=false"))
                .GET()
                .build();

        var response = WebUtils.executeRequest(request);
        WebUtils.throwOnClientErrors(response);

        if (response.statusCode() != 200) throw new IllegalArgumentException("no profile with uuid " + uuid);

        return JsonUtils.fromJson(response.body(), MinecraftProfilePropertiesResponse.class)
                .profile();
    }

    protected NameAndId getProfile(final String name) throws IOException {
        var request =
                HttpRequest.newBuilder().uri(this.profileByNameUri(name)).GET().build();

        var response = WebUtils.executeRequest(request);
        WebUtils.throwOnClientErrors(response);

        if (response.statusCode() != 200) throw new IllegalArgumentException("no profile with name " + name);

        return JsonUtils.fromJson(response.body(), NameAndId.class);
    }
}
