package me.mattgd.silktouchspawners;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentWrapper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

public class SilkTouchSpawners extends JavaPlugin implements Listener {
	
    public static Economy econ = null;
    private final Set<Material> PICKAXES = new HashSet<Material>();
    private final Enchantment SILK_TOUCH = new EnchantmentWrapper(33);
	private Set<String> confirmations = new HashSet<String>();
	private double cost = 0;
	private boolean confirm = true;
    
	@Override
	public void onEnable() {
		// Vault Setup
		if (!setupEconomy()) {
			getLogger().severe("Disabled due to no Vault dependency found!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Configuration
		getConfig().options().copyDefaults(true);
		saveConfig();
		
		cost = getConfig().getDouble("silk-touch-spawner-cost");
		confirm = getConfig().getBoolean("confirm-insufficient-funds");
		
		// Add valid tools to Set based on configuration
		if (getConfig().getBoolean("allow-with-any-pickaxe")) {
			PICKAXES.add(Material.WOOD_PICKAXE);
			PICKAXES.add(Material.STONE_PICKAXE);
			PICKAXES.add(Material.IRON_PICKAXE);
			PICKAXES.add(Material.GOLD_PICKAXE);
			PICKAXES.add(Material.DIAMOND_PICKAXE);
		} else {
			PICKAXES.add(Material.DIAMOND_PICKAXE);
		}
		
		getServer().getPluginManager().registerEvents(this, this); // Block listener
		getLogger().info("Enabled!");
	}
	
	@Override
	public void onDisable() {
		this.getConfig().options().copyDefaults(true);
		getLogger().info("Disabled!");
	}
	
	/** Vault Integration **/
	private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }
    
    // BlockBreakEvent
 	@EventHandler
 	public void onBlockBreak(BlockBreakEvent e) {
 		final Player p = e.getPlayer();
 		final Block b = e.getBlock();
 		
 		if (b.getType().equals(Material.MOB_SPAWNER) && p.hasPermission("silktouchspawners.use") 
 				&& p.getGameMode().equals(GameMode.SURVIVAL)) {
 			if (confirm && confirmations.contains(p.getName())) {
 				confirmations.remove(p.getName());
 				return;
 			}
 			
 			ItemStack tool = p.getInventory().getItemInMainHand();
 			if (PICKAXES.contains(p.getInventory().getItemInMainHand())
 					&& tool.containsEnchantment(SILK_TOUCH)) {
 				if (cost > 0) {
 					EconomyResponse r = econ.withdrawPlayer((OfflinePlayer) p, cost);
 					if (r.transactionSuccess()) {
 						final short entityID = getSpawnerEntityId(b);
 						ItemStack spawner = createNewSpawnerItem(entityID);
 						p.getWorld().dropItemNaturally(b.getLocation(), spawner);
 						b.breakNaturally();
 					} else if (confirm) {
						confirmations.add(p.getName());
 						p.sendMessage("You have insufficient funds to"
 								+ " use Silk Touch on this mob spawner."
 								+ "\nBreak the spawner again if you'd like"
 								+ "to destroy it.");
 					}
 				}
 			}
 		}
 	}
 	
 	private ItemStack createNewSpawnerItem(final short entityID) {
        ItemStack item = new ItemStack(Material.MOB_SPAWNER, 1, entityID);
        item.setDurability(entityID);
        item.addUnsafeEnchantment(Enchantment.SILK_TOUCH, entityID);
        return item;
    }
 	
 	@SuppressWarnings("deprecation")
	private short getSpawnerEntityId(final Block b) {
        BlockState blockState = b.getState();
        if (!(blockState instanceof CreatureSpawner)) {
            throw new IllegalArgumentException("getSpawnerEntityId called on non-spawner block: " + b);
        }

        CreatureSpawner spawner = ((CreatureSpawner) blockState);
        return spawner.getSpawnedType().getTypeId();
    }
    
}