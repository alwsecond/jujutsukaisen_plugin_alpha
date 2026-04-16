package abvgd.utils;

import abvgd.JJKPlugin;
import abvgd.core.JJKDomain;
import abvgd.core.JJKModel;
import abvgd.models.choso.ChosoModel;
import abvgd.models.gojo.GojoModel;
import abvgd.manage.JJKPlayer;
import abvgd.manage.PlayerManager;
import abvgd.models.hakari.HakariModel;
import abvgd.models.jogo.JogoModel;
import abvgd.models.kashimo.KashimoModel;
import abvgd.models.mahito.MahitoModel;
import abvgd.models.megumi.MegumiModel;
import abvgd.models.naoya.NaoyaModel;
import abvgd.models.sukuna.SukunaModel;
import abvgd.models.toji.TojiModel;
import abvgd.models.yuji.YujiModel;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class JJKCommands implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Только для игроков!");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§cИспользование: /jjk select <model>");
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            for (JJKDomain domain : JJKPlugin.activeDomains) {
                player.sendMessage(domain.getInfo().name());
            }
        }

        if (args[0].equalsIgnoreCase("get")) {
            player.sendMessage("gojo, toji, sukuna, naoya, megumi, mahito, hakari, yuji, kashimo, choso, jogo");
        }

        if (args[0].equalsIgnoreCase("select") && args.length == 2) {
            String choice = args[1].toLowerCase();
            JJKPlayer jjkPlayer = PlayerManager.get(player);
            JJKModel model = switch (choice) {
                case "gojo" -> new GojoModel();
                case "toji" -> new TojiModel();
                case "sukuna" -> new SukunaModel();
                case "naoya" -> new NaoyaModel();
                case "megumi" -> new MegumiModel();
                case "mahito" -> new MahitoModel();
                case "hakari" -> new HakariModel();
                case "yuji" -> new YujiModel();
                case "kashimo" -> new KashimoModel();
                case "choso" -> new ChosoModel();
                case "jogo" -> new JogoModel();
                default -> null;
            };
            if (model != null) {
                jjkPlayer.setModel(model, player);
                player.sendMessage("Вы выбрали -> " + model.getName());
                return true;
            }
        }
        return false;
    }
}
