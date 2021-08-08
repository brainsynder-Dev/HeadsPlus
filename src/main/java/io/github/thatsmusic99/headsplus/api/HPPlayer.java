package io.github.thatsmusic99.headsplus.api;

import io.github.thatsmusic99.headsplus.HeadsPlus;
import io.github.thatsmusic99.headsplus.api.events.LevelUpEvent;
import io.github.thatsmusic99.headsplus.config.ConfigLevels;
import io.github.thatsmusic99.headsplus.config.ConfigMobs;
import io.github.thatsmusic99.headsplus.config.HeadsPlusMessagesManager;
import io.github.thatsmusic99.headsplus.config.MainConfig;
import io.github.thatsmusic99.headsplus.managers.LevelsManager;
import io.github.thatsmusic99.headsplus.storage.PlayerScores;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.simple.JSONArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class HPPlayer {

    private UUID player;
    private int xp;
    private String level = null;
    private List<String> completeChallenges;
    private String nextLevel = null;
    private String currentMask;
    private List<PotionEffect> activeMask;
    public static HashMap<UUID, HPPlayer> players = new HashMap<>();
    private List<String> favouriteHeads;
    private List<String> pinnedChallenges;
    private boolean ignoreFallDamage;
    private String cachedLocale;
    private boolean localeForced;

    public HPPlayer(OfflinePlayer p) {
        if (p.isOnline() && p.getPlayer().hasMetadata("NPC")) return;
        HeadsPlus hp = HeadsPlus.get();
        activeMask = new ArrayList<>();
        favouriteHeads = new ArrayList<>();
        pinnedChallenges = new ArrayList<>();
        ignoreFallDamage = false;
        this.player = p.getUniqueId();
        try {
            for (Object o : (JSONArray) hp.getFavourites().getJSON().get(p.getUniqueId().toString())) {
                favouriteHeads.add(String.valueOf(o));
            }
            for (Object o : (JSONArray) hp.getPinned().getJSON().get(player.toString())) {
                pinnedChallenges.add(String.valueOf(o));
            }
        } catch (NullPointerException ignored) {
        }
        PlayerScores scores = hp.getScores();
        this.xp = scores.getXp(p.getUniqueId().toString());
        List<String> sc = new ArrayList<>();
        sc.addAll(scores.getCompletedChallenges(p.getUniqueId().toString()));
        if (MainConfig.get().getLocalisation().SMART_LOCALE && p.isOnline()) {
            Player player = p.getPlayer();
            String loc = scores.getLocale(p.getUniqueId().toString());
            if (loc != null && !loc.isEmpty() && !loc.equalsIgnoreCase("null")) {
                cachedLocale = loc.split(":")[0];
                localeForced = Boolean.parseBoolean(loc.split(":")[1]);
                HeadsPlusMessagesManager.get().setPlayerLocale(player, cachedLocale,  false);
            } else {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        HeadsPlusMessagesManager.get().setPlayerLocale(player);
                        cachedLocale = HeadsPlusMessagesManager.get().getSetLocale(player);
                        localeForced = false;
                        scores.setLocale(player.toString(), cachedLocale, false);
                    }
                }.runTaskLaterAsynchronously(hp, 100);
            }
        } else {
            cachedLocale = "";
            localeForced = false;
        }
        if (MainConfig.get().getMainFeatures().LEVELS) {
	    level = scores.getLevel(player.toString());
	    nextLevel = LevelsManager.get().getNextLevel(level).getConfigName();                
        }
        this.completeChallenges = sc;
        players.put(p.getUniqueId(), this);
    }

    public void clearMask() {
        try {
            ignoreFallDamage = false;
            currentMask = "";
            if (!getPlayer().isOnline()) return;
            for (PotionEffect p : activeMask) {
                ((Player) getPlayer()).removePotionEffect(p.getType());
            }
            activeMask.clear();
        } catch (NullPointerException ignored) { // When the mask messes up

        }
    }

    public void tempClearMasks() {
        try {
            ignoreFallDamage = false;
            if (!getPlayer().isOnline()) return;
            for (PotionEffect p : activeMask) {
                ((Player) getPlayer()).removePotionEffect(p.getType());
            }
        } catch (NullPointerException ignored) {
        }
    }

    public void addMask(String s) {
        if (s.equalsIgnoreCase("WANDERING_TRADER") || s.equalsIgnoreCase("TRADER_LLAMA")) {
            s = s.toLowerCase();
        } else {
            s = s.toLowerCase().replaceAll("_", "");
        }
        ConfigMobs hpch = ConfigMobs.get();
        List<PotionEffect> po = new ArrayList<>();
        // TODO mask rework
        for (int i = 0; i < hpch.getStringList(s + ".mask-effects").size(); i++) {
            String is = hpch.getStringList(s + ".mask-effects").get(i).toUpperCase();
            if (is.equalsIgnoreCase("ignore-fall-damage")) {
                ignoreFallDamage = true;
                continue;
            }
            int amp;
            if (ignoreFallDamage) {
                amp = (int) hpch.getList(s + ".mask-amplifiers").get(i - 1);
            } else {
                amp = (int) hpch.getList(s + ".mask-amplifiers").get(i);
            }

            try {
                PotionEffectType type = PotionEffectType.getByName(is);
                if (type == null) {
                    HeadsPlus.get().getLogger().severe("Invalid potion type detected. Please check your masks configuration in heads.yml! (" + is + ", " + s + ")");
                    continue;
                }
                PotionEffect p = new PotionEffect(type, MainConfig.get().getMasks().EFFECT_LENGTH, amp);
                ((Player) getPlayer()).addPotionEffect(p);
                po.add(p);
            } catch (IllegalArgumentException ex) {
                HeadsPlus.get().getLogger().severe("Invalid potion type detected. Please check your masks configuration in heads.yml!");
            }
        }
        activeMask = po;
        currentMask = s;
    }

    public void refreshMasks() {
        for (PotionEffect effect : activeMask) {
            Player player = (Player) getPlayer();
            player.removePotionEffect(effect.getType());
            player.addPotionEffect(new PotionEffect(effect.getType(), MainConfig.get().getMasks().EFFECT_LENGTH, effect.getAmplifier()));
        }
    }

    public void clearAllMasks() {
        for (PotionEffect effect : activeMask) {
            Player player = (Player) getPlayer();
            player.removePotionEffect(effect.getType());
        }
    }

    public int getXp() {
        return xp;
    }

    public Level getLevel() {
        return LevelsManager.get().getLevel(level);
    }

    public Level getNextLevel() {
        return LevelsManager.get().getLevel(nextLevel);
    }

    public List<String> getCompleteChallenges() {
        return completeChallenges;
    }

    public OfflinePlayer getPlayer() {
        return Bukkit.getOfflinePlayer(player);
    }

    public UUID getUuid() {
        return player;
    }

    public static HPPlayer getHPPlayer(OfflinePlayer p) {
        UUID uuid = p.getUniqueId();
        return players.get(uuid) != null ? players.get(uuid) : new HPPlayer(p);
    }

    public List<PotionEffect> getActiveMasks() {
        return activeMask;
    }

    public String getActiveMaskType() {
        return currentMask;
    }

    public String getLocale() {
        return cachedLocale;
    }

    public boolean isLocaleForced() {
        return localeForced;
    }

    public void setLocale(String locale) {
        setLocale(locale, true);
    }

    public void setLocale(String locale, boolean forced) {
        cachedLocale = locale;
        localeForced = forced;
        HeadsPlus.get().getScores().setLocale(getPlayer().getUniqueId().toString(), locale, forced);
    }

    public void addCompleteChallenge(Challenge c) {
        PlayerScores scores = HeadsPlus.get().getScores();
        scores.completeChallenge(player.toString(), c);
        completeChallenges.add(c.getConfigName());
    }

    public void addXp(int xp) {
        setXp(this.xp + xp);
    }

    public void removeXp(int xp) {
        setXp(this.xp - xp);
    }

    public void setXp(int xp) {
        HeadsPlus hp = HeadsPlus.get();
        PlayerScores scores = HeadsPlus.get().getScores();
        scores.setXp(player.toString(), xp);
        this.xp = xp;
        if (MainConfig.get().getMainFeatures().LEVELS) {
            new BukkitRunnable() {
                @Override
                public void run() {
	            Level nextLevelLocal = LevelsManager.get().getLevel(nextLevel);
                    while (nextLevelLocal != null && nextLevelLocal.getRequiredXP() <= getXp()) {
                        jumps++;
                        nextLevelLocal = LevelsManager.get().getNextLevel(nextLevelLocal.getConfigName());
                        if (MainConfig.get().getLevels().MULTIPLE_LEVEL_UPS) initLevelUp(jumps);
                        if (nextLevelLocal.isrEnabled()) nextLevelLocal.getReward().rewardPlayer(null, (Player) getPlayer());

                    }
                }
            }.runTask(hp);
        }
    }

    private void initLevelUp(int jumps) {
        Level nextLevel = LevelsManager.get().getLevel(this.level + jumps);
        LevelUpEvent event = new LevelUpEvent((Player) getPlayer(), LevelsManager.get().getLevel(level), nextLevel);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;
        this.level += jumps;
        this.nextLevel += jumps;
        Player player = (Player) getPlayer();
        if (MainConfig.get().getLevels().BROADCAST_LEVEL_UP) {
            final String name = player.isOnline() ? player.getPlayer().getDisplayName() : player.getName();
            for (Player p : Bukkit.getOnlinePlayers()) {
                MessagesManager.get().sendMessage("commands.levels.level-up", p, "{player}", name, "{name}", name, "{level}",
                        ChatColor.translateAlternateColorCodes('&', nextLevel.getDisplayName()));
            }
        }
    }

    public boolean hasHeadFavourited(String s) {
        return favouriteHeads.contains(s);
    }

    public void addFavourite(String s) {
        favouriteHeads.add(s);
        HeadsPlus.get().getFavourites().writeData(getPlayer(), s);
    }

    public void removeFavourite(String s) {
        favouriteHeads.remove(s);
        HeadsPlus.get().getFavourites().removeHead(getPlayer(), s);
    }

    public boolean hasChallengePinned(Challenge challenge) {
        return pinnedChallenges.contains(challenge.getConfigName());
    }

    public void addChallengePin(Challenge challenge) {
        String s = challenge.getConfigName();
        pinnedChallenges.add(s);
        HeadsPlus.get().getPinned().writeData(getPlayer(), s);
    }

    public void removeChallengePin(Challenge challenge) {
        String s = challenge.getConfigName();
        pinnedChallenges.remove(s);
        HeadsPlus.get().getPinned().removeChallenge(getPlayer(), s);
    }

    public List<String> getPinnedChallenges() {
        return pinnedChallenges;
    }

    public boolean isIgnoringFallDamage() {
        return ignoreFallDamage;
    }

    public List<String> getFavouriteHeads() {
        return favouriteHeads;
    }
}
