package com.denizenscript.denizen.events.player;

import com.denizenscript.denizen.objects.*;
import com.denizenscript.denizen.utilities.debugging.dB;
import com.denizenscript.denizen.BukkitScriptEntryData;
import com.denizenscript.denizen.events.BukkitScriptEvent;
import com.denizenscript.denizencore.objects.*;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import com.denizenscript.denizencore.scripts.containers.ScriptContainer;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class PlayerBreaksBlockScriptEvent extends BukkitScriptEvent implements Listener {

    // TODO: de-collide with breaks item
    // <--[event]
    // @Events
    // player breaks block
    // player breaks <material>
    // player breaks block
    // player breaks <material>
    //
    // @Regex ^on player breaks [^\s]+$
    //
    // @Switch in <area>
    // @Switch with <item>
    //
    // @Cancellable true
    //
    // @Triggers when a player breaks a block.
    //
    // @Context
    // <context.location> returns the dLocation the block was broken at.
    // <context.material> returns the dMaterial of the block that was broken.
    // <context.xp> returns how much XP will be dropped.
    //
    // @Determine
    // "NOTHING" to make the block drop no items.
    // dList(dItem) to make the block drop a specified list of items.
    // Element(Number) to set the amount of xp to drop.
    //
    // -->

    public PlayerBreaksBlockScriptEvent() {
        instance = this;
    }

    public static PlayerBreaksBlockScriptEvent instance;
    public dLocation location;
    public dMaterial material;
    public Element xp;
    public BlockBreakEvent event;

    @Override
    public boolean couldMatch(ScriptContainer scriptContainer, String s) {
        return CoreUtilities.toLowerCase(s).startsWith("player breaks");
    }

    @Override
    public boolean matches(ScriptPath path) {
        String mat = path.eventArgLowerAt(2);
        if (!tryMaterial(material, mat)) {
            return false;
        }
        if (!runInCheck(path, location)) {
            return false;
        }
        if (!runWithCheck(path, new dItem(event.getPlayer().getItemInHand()))) {
            return false;
        }
        // Deprecated in favor of with: format
        if (path.eventArgLowerAt(3).equals("with")
                && !tryItem(new dItem(event.getPlayer().getItemInHand()), path.eventArgLowerAt(4))) {
            return false;
        }
        return true;
    }

    @Override
    public String getName() {
        return "PlayerBreaksBlock";
    }

    @Override
    public boolean applyDetermination(ScriptContainer container, String determination) {
        String lower = CoreUtilities.toLowerCase(determination);
        Block block = event.getBlock();
        if (lower.equals("nothing")) {
            cancelled = true;
            block.setType(Material.AIR);
        }
        else if (ArgumentHelper.matchesInteger(determination)) {
            xp = Argument.valueOf(lower).asElement();
        }
        else if (Argument.valueOf(lower).matchesArgumentList(dItem.class)) {
            cancelled = true;
            block.setType(Material.AIR);

            for (dItem newItem : dList.valueOf(determination).filter(dItem.class, container)) {
                block.getWorld().dropItemNaturally(block.getLocation(), newItem.getItemStack()); // Drop each item
            }
        }
        else {
            return super.applyDetermination(container, determination);
        }
        return true;
    }

    @Override
    public ScriptEntryData getScriptEntryData() {
        return new BukkitScriptEntryData(dPlayer.mirrorBukkitPlayer(event.getPlayer()), null);
    }

    @Override
    public dObject getContext(String name) {
        if (name.equals("location")) {
            return location;
        }
        else if (name.equals("material")) {
            return material;
        }
        else if (name.equals("cuboids")) {
            dB.echoError("context.cuboids tag is deprecated in " + getName() + " script event");
            dList cuboids = new dList();
            for (dCuboid cuboid : dCuboid.getNotableCuboidsContaining(location)) {
                cuboids.add(cuboid.identifySimple());
            }
            return cuboids;
        }
        else if (name.equals("xp")) {
            return xp;
        }
        return super.getContext(name);
    }

    @EventHandler
    public void onPlayerBreaksBlock(BlockBreakEvent event) {
        if (dEntity.isNPC(event.getPlayer())) {
            return;
        }
        material = new dMaterial(event.getBlock());
        location = new dLocation(event.getBlock().getLocation());
        xp = new Element(event.getExpToDrop());
        this.event = event;
        fire(event);
        event.setExpToDrop(xp.asInt());
    }

}
