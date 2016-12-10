/*  
 *  WeatherMan, Minecraft bukkit plugin
 *  (c)2012-2014, fromgate, fromgate@gmail.com
 *  http://dev.bukkit.org/server-mods/weatherman/
 *    
 *  This file is part of WeatherMan.
 *  
 *  WeatherMan is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  WeatherMan is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with WeatherMan.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package me.fromgate.weatherman;


import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class WMListener implements Listener {
    WeatherMan plg;
    Util u;

    public WMListener(WeatherMan plg) {
        this.plg = plg;
        this.u = plg.u;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockForm(BlockFormEvent event) {
        if ((event.getNewState().getType() == Material.SNOW) && (!plg.unsnowBiomes.isEmpty()) && (u.isWordInList(BiomeTools.biome2Str(event.getBlock().getBiome()), plg.unsnowBiomes)))
            event.setCancelled(true);
        if ((event.getNewState().getType() == Material.ICE) && (!plg.uniceBiomes.isEmpty()) && (u.isWordInList(BiomeTools.biome2Str(event.getBlock().getBiome()), plg.uniceBiomes)))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        u.updateMsg(p);
        PlayerConfig.clearPlayerConfig(p);
        if (PlayerConfig.isWandMode(p)) {
            PlayerConfig.setWandMode(p, false);
            u.printMSG(p, "msg_wandmodedisabled", u.EnDis(false), "&6/wm wand&a");
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (plg.netherMob) return;
        if (event.getEntity().getWorld().getEnvironment() == Environment.NETHER) return;
        if (event.getSpawnReason() != SpawnReason.NATURAL) return;
        if ((event.getEntityType() == EntityType.PIG_ZOMBIE) ||
                (event.getEntityType() == EntityType.MAGMA_CUBE) ||
                (event.getEntityType() == EntityType.GHAST))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if ((event.getAction() != Action.RIGHT_CLICK_AIR) && (event.getAction() != Action.RIGHT_CLICK_BLOCK)) return;
        if (PlayerConfig.isWandMode(player)) Brush.shootWand(player);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onProjectileHitEvent(ProjectileHitEvent event) {
        if (event.getEntityType() != EntityType.SNOWBALL) return;
        Brush.processSnowball((Snowball) event.getEntity());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onSignChange(SignChangeEvent event) {
        Player p = event.getPlayer();
        if (ChatColor.stripColor(event.getLine(1)).equalsIgnoreCase("[biome]"))
            if (!p.hasPermission("weatherman.sign")) event.setLine(1, "{biome}");
            else {
                if (!BiomeTools.isBiomeExists(event.getLine(0)))
                    event.setLine(0, BiomeTools.biome2Str(event.getBlock().getBiome()));
                event.setLine(0, ChatColor.GREEN + event.getLine(0));
                event.setLine(1, ChatColor.BOLD + "" + ChatColor.DARK_AQUA + "[Biome]");
                String l2 = event.getLine(2);
                if (l2.equalsIgnoreCase("replace")) event.setLine(2, ChatColor.BLUE + "Replace");
                else if (l2.matches("[1-9]+[0-9]*")) event.setLine(2, ChatColor.BLUE + "radius=" + l2);
                else if ((l2.toLowerCase().startsWith("radius=")) && (l2.toLowerCase().replace("radius=", "").matches("[1-9]+[0-9]*")))
                    event.setLine(2, ChatColor.BLUE + "radius=" + l2.toLowerCase().replace("radius=", ""));
                else if (l2.isEmpty())
                    event.setLine(2, ChatColor.BLUE + "radius=" + Integer.toString(plg.defaultRadius));
                else {
                    event.setLine(2, ChatColor.BLUE + l2);
                    if (WMWorldEdit.isWG()) {
                        if (!WMWorldEdit.isRegionExists(event.getBlock().getWorld(), l2))
                            u.printMSG(p, "wg_unknownregion", 'c', '4', l2);
                    } else {
                        u.printMSG(p, "wg_notfound", 'c');
                        event.setLine(2, ChatColor.BLUE + "radius=" + Integer.toString(plg.defaultRadius));
                    }
                }
                if (event.getLine(3).isEmpty() ||
                        ((!event.getLine(3).isEmpty()) && (!BiomeTools.isBiomeExists(event.getLine(3)))))
                    event.setLine(3, BiomeTools.biome2Str(plg.defaultBiome));
                event.setLine(3, ChatColor.RED + event.getLine(3));
            }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockRedstoneChange(BlockRedstoneEvent event) {
        Block b = event.getBlock();
        if ((b.getType() == Material.SIGN_POST) || (b.getType() == Material.WALL_SIGN)) {
            BlockState state = b.getState();
            if (state instanceof Sign) {
                Sign sign = (Sign) state;

                if (!ChatColor.stripColor(sign.getLine(1)).equalsIgnoreCase("[biome]")) return;
                else if ((!sign.getLine(0).isEmpty()) &&
                        (!sign.getLine(2).isEmpty()) &&
                        (!sign.getLine(3).isEmpty())) {
                    String b1 = ChatColor.stripColor(sign.getLine(0));
                    String b2 = ChatColor.stripColor(sign.getLine(3));

                    if (BiomeTools.isBiomeExists(b1) && BiomeTools.isBiomeExists(b2)) {

                        int radius = -1;
                        Location loc1 = null;
                        Location loc2 = null;

                        String rs = ChatColor.stripColor(sign.getLine(2)).toLowerCase();

                        int mode = -1;
                        if (rs.startsWith("radius=")) {
                            rs = rs.replace("radius=", "");
                            if (rs.matches("[1-9]+[0-9]*")) {
                                radius = Math.min(Integer.parseInt(rs), plg.maxRadiusSign);
                                mode = 0;
                            }
                        } else if (rs.equalsIgnoreCase("replace")) mode = 1;
                        else {
                            World w = event.getBlock().getWorld();
                            if (WMWorldEdit.isRegionExists(w, rs)) {
                                loc1 = WMWorldEdit.getMinPoint(w, rs);
                                loc1.setY(0);
                                loc2 = WMWorldEdit.getMaxPoint(w, rs);
                                loc2.setY(0);
                                mode = 2;
                            }
                        }

                        if (mode >= 0) {
                            Biome biome = BiomeTools.str2Biome(b1);
                            sign.setLine(0, ChatColor.GREEN + b1);
                            sign.setLine(3, ChatColor.RED + b2);
                            if (b.isBlockIndirectlyPowered()) {
                                biome = BiomeTools.str2Biome(b2);
                                sign.setLine(0, ChatColor.RED + b1);
                                sign.setLine(3, ChatColor.GREEN + b2);
                            }
                            switch (mode) {
                                case 0:
                                    BiomeTools.setBiomeRadius(null, sign.getLocation(), biome, radius, null);
                                    break;
                                case 1:
                                    BiomeTools.floodFill(null, sign.getLocation(), biome);
                                    break;
                                case 2:
                                    BiomeTools.setBiomeArea(null, loc1, loc2, biome, null);
                                    break;
                            }
                            sign.update(true);
                        }
                    }
                } else
                    u.log("Something wrong with WeatherMan-sign: [" + ChatColor.stripColor(sign.getLine(0)) + "|" + ChatColor.stripColor(sign.getLine(1)) +
                            "|" + ChatColor.stripColor(sign.getLine(2)) + "|" + ChatColor.stripColor(sign.getLine(3)) +
                            "] " + event.getBlock().getLocation().toString());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player p = event.getPlayer();
        if (PlayerConfig.isWalkInfoMode(p)) {
            if (event.getFrom().getBlock().getBiome().equals(event.getTo().getBlock().getBiome())) return;
            Biome b1 = event.getTo().getBlock().getBiome();
            Biome b2 = NMSUtil.getOriginalBiome(event.getTo());
            if (b1.equals(b2)) u.printMSG(p, "msg_movetobiome", BiomeTools.biome2Str(b1));
            else u.printMSG(p, "msg_movetobiome2", BiomeTools.biome2Str(b1), BiomeTools.biome2Str(b2));
        }
    }
}
