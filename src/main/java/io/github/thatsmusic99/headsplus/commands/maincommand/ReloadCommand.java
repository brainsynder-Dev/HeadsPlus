package io.github.thatsmusic99.headsplus.commands.maincommand;

import io.github.thatsmusic99.configurationmaster.CMFile;
import io.github.thatsmusic99.headsplus.HeadsPlus;
import io.github.thatsmusic99.headsplus.api.HPPlayer;
import io.github.thatsmusic99.headsplus.commands.CommandInfo;
import io.github.thatsmusic99.headsplus.commands.IHeadsPlusCommand;
import io.github.thatsmusic99.headsplus.config.HPConfig;
import io.github.thatsmusic99.headsplus.config.HeadsPlusMessagesManager;
import io.github.thatsmusic99.headsplus.managers.EntityDataManager;
import io.github.thatsmusic99.headsplus.managers.HeadManager;
import io.github.thatsmusic99.headsplus.managers.MaskManager;
import io.github.thatsmusic99.headsplus.managers.SellableHeadsManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@CommandInfo(
        commandname = "reload",
        permission = "headsplus.maincommand.reload",
        subcommand = "reload",
        maincommand = true,
        usage = "/hp reload"
)
public class ReloadCommand implements IHeadsPlusCommand {

    @Override
    public String getCmdDescription(CommandSender sender) {
        return HeadsPlusMessagesManager.get().getString("descriptions.hp.reload");
    }

    @Override
    public boolean fire(String[] args, CommandSender sender) {
        HeadsPlusMessagesManager.get().sendMessage("commands.reload.reloading-message", sender);
        new BukkitRunnable() {
            @Override
            public void run() {
                HeadManager.get().reset();
                MaskManager.get().reset();
                SellableHeadsManager.get().reset();
                for (HPConfig cs : HeadsPlus.get().getConfigs()) {
                    try {
                        cs.reload();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                HPPlayer.players.clear();
                EntityDataManager.init();
                HeadsPlus.get().restartMessagesManager();
                HeadsPlusMessagesManager.get().sendMessage("commands.reload.reload-message", sender);
            }
        }.runTaskLaterAsynchronously(HeadsPlus.get(), 2);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        return new ArrayList<>();
    }
}

