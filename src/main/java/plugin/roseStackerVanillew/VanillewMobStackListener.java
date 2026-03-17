package plugin.roseStackerVanillew;

import dev.rosewood.rosestacker.api.RoseStackerAPI;
import dev.rosewood.rosestacker.event.EntityStackEvent;
import dev.rosewood.rosestacker.stack.StackedEntity;
import java.util.Locale;
import java.util.Set;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

public class VanillewMobStackListener implements Listener {

    private final RoseStackerVanillew plugin;
    private final Set<String> blockedWorlds;
    private final boolean debugBlockedStack;
    private final boolean chunkLimitEnabled;
    private final int maxLivingEntitiesPerChunk;

    public VanillewMobStackListener(
            RoseStackerVanillew plugin,
            Set<String> blockedWorlds,
            boolean debugBlockedStack,
            boolean chunkLimitEnabled,
            int maxLivingEntitiesPerChunk
    ) {
        this.plugin = plugin;
        this.blockedWorlds = blockedWorlds;
        this.debugBlockedStack = debugBlockedStack;
        this.chunkLimitEnabled = chunkLimitEnabled;
        this.maxLivingEntitiesPerChunk = maxLivingEntitiesPerChunk;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityStack(EntityStackEvent event) {
        World world = event.getStack().getLocation().getWorld();
        if (world == null || !this.isBlockedWorld(world)) {
            return;
        }

        // Cancels only mob/entity merge stacking in the configured worlds.
        event.setCancelled(true);

        if (this.debugBlockedStack) {
            Entity entity = event.getStack().getEntity();
            this.plugin.getLogger().info(
                    "Stack de mob bloqueado em '" + world.getName() + "' para entidade " + entity.getType()
            );
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMobSpawn(CreatureSpawnEvent event) {
        LivingEntity entity = event.getEntity();
        if (!this.isBlockedWorld(entity.getWorld())) {
            return;
        }

        if (this.chunkLimitEnabled && this.isChunkAtOrOverLivingLimit(entity, this.maxLivingEntitiesPerChunk)) {
            event.setCancelled(true);

            if (this.debugBlockedStack) {
                this.plugin.getLogger().info(
                        "Spawn bloqueado por limite de entidades no chunk em '" + entity.getWorld().getName()
                                + "' para " + entity.getType() + " (max=" + this.maxLivingEntitiesPerChunk + ")"
                );
            }
            return;
        }

        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER) {
            // RoseStacker may assign a stack size during/after spawn; normalize next tick.
            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> this.normalizeToSingleMob(entity));
        }
    }

    private boolean isChunkAtOrOverLivingLimit(LivingEntity spawningEntity, int limit) {
        Chunk chunk = spawningEntity.getChunk();
        Entity[] entities = chunk.getEntities();
        int livingCount = 0;

        for (Entity entity : entities) {
            if (!(entity instanceof LivingEntity)) {
                continue;
            }

            // Ignore the spawning entity itself if it is already visible in this event tick.
            if (entity.getUniqueId().equals(spawningEntity.getUniqueId())) {
                continue;
            }

            livingCount++;
            if (livingCount >= limit) {
                return true;
            }
        }

        return false;
    }

    private void normalizeToSingleMob(LivingEntity entity) {
        if (!entity.isValid() || entity.isDead()) {
            return;
        }

        StackedEntity stackedEntity = RoseStackerAPI.getInstance().getStackedEntity(entity);
        if (stackedEntity == null || stackedEntity.getStackSize() <= 1) {
            return;
        }

        int removedFromStack = 0;
        while (stackedEntity.getStackSize() > 1) {
            stackedEntity.decreaseStackSize();
            removedFromStack++;
        }

        if (this.debugBlockedStack && removedFromStack > 0) {
            this.plugin.getLogger().info(
                    "Stack de mob de spawner desfeito em '" + entity.getWorld().getName() +
                            "' para entidade " + entity.getType() + " (" + removedFromStack + " removidos do stack)"
            );
        }
    }

    private boolean isBlockedWorld(World world) {
        return this.blockedWorlds.contains(world.getName().toLowerCase(Locale.ROOT));
    }
}
