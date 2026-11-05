package ysmstep;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 服务端存档：谁的脚步档案是什么。
 *
 * 两份表：
 *   byPlayer —— 玩家 UUID → 档案（谁调谁上传，别人听他的就对）
 *   byModel  —— 模型名 → 档案（同一模型换个人用，自动有默认值）
 *
 * 存在世界存档里（overworld 的 data/ysmstep_profiles.dat）。
 */
public class ProfileStore extends SavedData {

    private static final String NAME = "ysmstep_profiles";

    private final Map<UUID, Profile> byPlayer = new HashMap<>();
    /** 键统一转小写，免得「黑山羊之母」和「黑山羊之母 」算两个。 */
    private final Map<String, Profile> byModel = new HashMap<>();

    public static ProfileStore get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(new Factory<>(ProfileStore::new, ProfileStore::load), NAME);
    }

    public Map<UUID, Profile> players() {
        return byPlayer;
    }

    public Profile model(String name) {
        return name == null || name.isBlank() ? null : byModel.get(key(name));
    }

    public void putPlayer(UUID uuid, Profile profile) {
        byPlayer.put(uuid, profile.sanitized());
        if (!profile.modelName().isBlank()) {
            byModel.put(key(profile.modelName()), profile.sanitized());
        }
        setDirty();
    }

    public void removePlayer(UUID uuid) {
        if (byPlayer.remove(uuid) != null) {
            setDirty();
        }
    }

    private static String key(String name) {
        return name.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public List<Map.Entry<UUID, Profile>> entries() {
        return new ArrayList<>(byPlayer.entrySet());
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        byPlayer.forEach((uuid, p) -> {
            CompoundTag e = new CompoundTag();
            e.putUUID("uuid", uuid);
            writeProfile(e, p);
            list.add(e);
        });
        tag.put("players", list);

        ListTag models = new ListTag();
        byModel.forEach((name, p) -> {
            CompoundTag e = new CompoundTag();
            e.putString("name", name);
            writeProfile(e, p);
            models.add(e);
        });
        tag.put("models", models);
        return tag;
    }

    public static ProfileStore load(CompoundTag tag, HolderLookup.Provider registries) {
        ProfileStore store = new ProfileStore();
        ListTag list = tag.getList("players", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            if (e.hasUUID("uuid")) {
                store.byPlayer.put(e.getUUID("uuid"), readProfile(e));
            }
        }
        ListTag models = tag.getList("models", Tag.TAG_COMPOUND);
        for (int i = 0; i < models.size(); i++) {
            CompoundTag e = models.getCompound(i);
            String name = e.getString("name");
            if (!name.isBlank()) {
                store.byModel.put(key(name), readProfile(e));
            }
        }
        return store;
    }

    private static void writeProfile(CompoundTag tag, Profile p) {
        tag.putFloat("cycle", p.cycle());
        tag.putFloat("offset", p.offset());
        tag.putFloat("runFactor", p.runFactor());
        tag.putFloat("sneakFactor", p.sneakFactor());
        tag.putString("walkMode", p.walkMode());
        tag.putString("runMode", p.runMode());
        tag.putString("sneakMode", p.sneakMode());
        tag.putString("modelName", p.modelName());
    }

    private static Profile readProfile(CompoundTag tag) {
        return new Profile(
                tag.getFloat("cycle"), tag.getFloat("offset"),
                tag.getFloat("runFactor"), tag.getFloat("sneakFactor"),
                tag.getString("walkMode"), tag.getString("runMode"), tag.getString("sneakMode"),
                tag.getString("modelName")).sanitized();
    }
}
