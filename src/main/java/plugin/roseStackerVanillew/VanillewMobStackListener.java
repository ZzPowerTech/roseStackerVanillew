package plugin.roseStackerVanillew;

import dev.rosewood.rosestacker.event.EntityStackEvent;
import java.util.Locale;
import java.util.Set;
import org.bukkit.entity.Entity;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class VanillewMobStackListener implements Listener {

    private final RoseStackerVanillew plugin;
    private final Set<String> blockedWorlds;
    private final boolean debugBlockedStack;

    public VanillewMobStackListener(RoseStackerVanillew plugin, Set<String> blockedWorlds, boolean debugBlockedStack) {
        this.plugin = plugin;
        this.blockedWorlds = blockedWorlds;
        this.debugBlockedStack = debugBlockedStack;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityStack(EntityStackEvent event) {
        World world = event.getStack().getLocation().getWorld();
        if (world == null) {
            return;
        }

        if (this.blockedWorlds.contains(world.getName().toLowerCase(Locale.ROOT))) {
            // Cancels only mob/entity stacking in the configured worlds.
            event.setCancelled(true);

            if (this.debugBlockedStack) {
                Entity entity = event.getStack().getEntity();
                this.plugin.getLogger().info(
                        "Stack de mob bloqueado em '" + world.getName() + "' para entidade " + entity.getType()
                );
            }
        }
    }
}

