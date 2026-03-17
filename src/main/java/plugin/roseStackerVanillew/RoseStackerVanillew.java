package plugin.roseStackerVanillew;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

public final class RoseStackerVanillew extends JavaPlugin {

    private static final String RELOAD_PERMISSION = "rosestackervanillew.reload";
    private VanillewMobStackListener activeListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        if (getServer().getPluginManager().getPlugin("RoseStacker") == null) {
            getLogger().warning("RoseStacker nao encontrado. O bloqueio de stack de mobs nao sera aplicado.");
            return;
        }

        this.applyRuntimeConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("rsv")) {
            return false;
        }

        if (args.length != 1 || !args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage(ChatColor.YELLOW + "Uso: /" + label + " reload");
            return true;
        }

        if (!sender.hasPermission(RELOAD_PERMISSION)) {
            sender.sendMessage(ChatColor.RED + "Voce nao tem permissao para usar este comando.");
            return true;
        }

        if (getServer().getPluginManager().getPlugin("RoseStacker") == null) {
            sender.sendMessage(ChatColor.RED + "RoseStacker nao foi encontrado. Nao foi possivel aplicar o reload.");
            return true;
        }

        reloadConfig();
        this.applyRuntimeConfig();
        sender.sendMessage(ChatColor.GREEN + "Configuracao recarregada com sucesso.");
        return true;
    }

    private Set<String> loadBlockedWorlds() {
        List<String> worlds = getConfig().getStringList("blocked-worlds");
        if (worlds.isEmpty()) {
            worlds = List.of("vanillew");
            getLogger().warning("Nenhum mundo configurado em blocked-worlds. Usando fallback: [vanillew].");
        }

        return worlds.stream()
                .filter(world -> world != null && !world.isBlank())
                .map(world -> world.toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void applyRuntimeConfig() {
        Set<String> blockedWorlds = this.loadBlockedWorlds();
        boolean debugBlockedStack = getConfig().getBoolean("debug.blocked-stack", false);
        boolean chunkLimitEnabled = getConfig().getBoolean("limits.chunk-living-entities.enabled", true);
        int maxLivingEntitiesPerChunk = Math.max(1, getConfig().getInt("limits.chunk-living-entities.max", 24));

        if (this.activeListener != null) {
            HandlerList.unregisterAll(this.activeListener);
        }

        this.activeListener = new VanillewMobStackListener(
                this,
                blockedWorlds,
                debugBlockedStack,
                chunkLimitEnabled,
                maxLivingEntitiesPerChunk
        );
        getServer().getPluginManager().registerEvents(this.activeListener, this);
        getLogger().info(
                "Bloqueio de stack de mobs ativado para mundos: " + blockedWorlds
                        + ". Debug: " + debugBlockedStack
                        + ". Limite por chunk ativo: " + chunkLimitEnabled
                        + " (max=" + maxLivingEntitiesPerChunk + ")"
        );
    }

    @Override
    public void onDisable() {
        if (this.activeListener != null) {
            HandlerList.unregisterAll(this.activeListener);
            this.activeListener = null;
        }
    }
}
