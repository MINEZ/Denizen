package com.denizenscript.denizen.utilities.entity;

import com.denizenscript.denizen.Denizen;
import com.denizenscript.denizen.nms.NMSHandler;
import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizen.utilities.packets.NetworkInterceptHelper;
import com.denizenscript.denizencore.objects.core.DurationTag;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.function.Consumer;

public class FakeEntity {

    public static class FakeEntityMap {

        public Map<Integer, FakeEntity> byId = new HashMap<>();

        public void remove(FakeEntity entity) {
            byId.remove(entity.id);
        }
    }

    public final static Map<UUID, FakeEntityMap> playersToEntities = new HashMap<>();
    public final static Map<UUID, FakeEntity> idsToEntities = new HashMap<>();

    public static FakeEntity getFakeEntityFor(UUID uuid, int id) {
        FakeEntityMap map = playersToEntities.get(uuid);
        if (map == null) {
            return null;
        }
        return map.byId.get(id);
    }

    public static boolean refreshListenersRegistered = false;

    /**
     * 假实体依赖为每个观看者单独建立的跟踪器，而跟踪器绑定于建立时的那条连接。
     * 玩家重新加入或切换世界后，其客户端不再持有这些实体，原先的连接也已作废，
     * 须重新建立跟踪，否则假实体只会停在原处不再更新。
     */
    public static void enableRefreshListeners() {
        if (refreshListenersRegistered) {
            return;
        }
        refreshListenersRegistered = true;
        Bukkit.getPluginManager().registerEvents(new RefreshListener(), Denizen.getInstance());
    }

    public static class RefreshListener implements Listener {

        @EventHandler(priority = EventPriority.MONITOR)
        public void onPlayerJoin(PlayerJoinEvent event) {
            scheduleRefresh(new PlayerTag(event.getPlayer()));
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
            scheduleRefresh(new PlayerTag(event.getPlayer()));
        }
    }

    /** 玩家加入或切换世界之际客户端尚未就绪，故延后两 tick 再补发。 */
    public static void scheduleRefresh(PlayerTag player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    refreshFor(player);
                }
            }
        }.runTaskLater(Denizen.getInstance(), 2);
    }

    public static void refreshFor(PlayerTag player) {
        World world = player.getPlayerEntity().getWorld();
        for (FakeEntity fake : new ArrayList<>(idsToEntities.values())) {
            if (!fake.refreshOnJoin || fake.triggerSpawnPacket == null || fake.entity == null || !fake.entity.isFakeValid) {
                continue;
            }
            boolean known = fake.hasPlayer(player.getUUID());
            if (!known && !fake.forAllPlayers) {
                continue;
            }
            if (!world.equals(fake.entity.getBukkitEntity().getWorld())) {
                continue;
            }
            if (!known) {
                fake.addPlayer(player);
            }
            fake.triggerSpawnPacket.accept(player);
        }
    }

    public List<PlayerTag> players;
    public int id;
    public EntityTag entity;
    public LocationTag location;
    public BukkitTask currentTask = null;
    public Consumer<PlayerTag> triggerSpawnPacket;
    public Runnable triggerUpdatePacket;
    public Runnable triggerDestroyPacket;
    public UUID overrideUUID;
    /** 是否在观看者重新加入或切换世界后重新建立跟踪，目前仅 fakespawn 生成的假实体需要。 */
    public boolean refreshOnJoin = false;
    /** 是否展示给所在世界的每一位玩家，包括其后才加入或进入该世界的玩家。 */
    public boolean forAllPlayers = false;

    public FakeEntity(List<PlayerTag> player, LocationTag location, int id) {
        // 观看者可在其后增补，故此处另存一份可变的列表。
        this.players = new ArrayList<>(player);
        this.location = location;
        this.id = id;
    }

    public static FakeEntity showFakeEntityTo(List<PlayerTag> players, EntityTag typeToSpawn, LocationTag location, DurationTag duration, EntityTag vehicle) {
        NetworkInterceptHelper.enable();
        enableRefreshListeners();
        FakeEntity fakeEntity = NMSHandler.playerHelper.sendEntitySpawn(players, typeToSpawn.getEntityType(), location, typeToSpawn.mechanisms == null ? null : new ArrayList<>(typeToSpawn.mechanisms), -1, null, true);
        if (vehicle != null) {
            NMSHandler.playerHelper.addFakePassenger(players, vehicle.getBukkitEntity(), fakeEntity);
        }
        idsToEntities.put(fakeEntity.overrideUUID == null ? fakeEntity.entity.getUUID() : fakeEntity.overrideUUID, fakeEntity);
        for (PlayerTag player : players) {
            UUID uuid = player.getPlayerEntity().getUniqueId();
            FakeEntity.FakeEntityMap playerEntities = playersToEntities.get(uuid);
            if (playerEntities == null) {
                playerEntities = new FakeEntity.FakeEntityMap();
                playersToEntities.put(uuid, playerEntities);
            }
            playerEntities.byId.put(fakeEntity.id, fakeEntity);
        }
        fakeEntity.updateEntity(fakeEntity.entity, duration);
        return fakeEntity;
    }

    public void cancelEntity() {
        if (currentTask != null) {
            currentTask.cancel();
            currentTask = null;
        }
        idsToEntities.remove(overrideUUID == null ? entity.getUUID() : overrideUUID);
        if (triggerDestroyPacket != null) {
            triggerDestroyPacket.run();
        }
        else {
            for (PlayerTag player : players) {
                if (player.isOnline()) {
                    NMSHandler.playerHelper.sendEntityDestroy(player.getPlayerEntity(), entity.getBukkitEntity());
                }
            }
        }
        for (PlayerTag player : players) {
            FakeEntity.FakeEntityMap mapping = playersToEntities.get(player.getUUID());
            if (mapping != null) {
                mapping.remove(this);
            }
        }
        entity.isFakeValid = false;
    }

    public void addPlayer(PlayerTag player) {
        players.add(player);
        FakeEntityMap playerEntities = playersToEntities.get(player.getUUID());
        if (playerEntities == null) {
            playerEntities = new FakeEntityMap();
            playersToEntities.put(player.getUUID(), playerEntities);
        }
        playerEntities.byId.put(id, this);
    }

    public boolean hasPlayer(UUID uuid) {
        for (PlayerTag player : players) {
            if (uuid.equals(player.getUUID())) {
                return true;
            }
        }
        return false;
    }

    private void updateEntity(EntityTag entity, DurationTag duration) {
        if (currentTask != null) {
            currentTask.cancel();
        }
        this.entity = entity;
        if (duration != null && duration.getTicks() > 0) {
            currentTask = new BukkitRunnable() {
                @Override
                public void run() {
                    currentTask = null;
                    cancelEntity();
                }
            }.runTaskLater(Denizen.getInstance(), duration.getTicks());
        }
    }
}
