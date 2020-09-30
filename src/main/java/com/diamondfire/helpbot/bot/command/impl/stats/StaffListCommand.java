package com.diamondfire.helpbot.bot.command.impl.stats;

import com.diamondfire.helpbot.bot.command.argument.ArgumentSet;
import com.diamondfire.helpbot.bot.command.help.*;
import com.diamondfire.helpbot.bot.command.impl.Command;
import com.diamondfire.helpbot.bot.command.permissions.Permission;
import com.diamondfire.helpbot.bot.events.CommandEvent;
import com.diamondfire.helpbot.bot.reactions.multiselector.MultiSelectorBuilder;
import com.diamondfire.helpbot.df.ranks.*;
import com.diamondfire.helpbot.sys.database.impl.DatabaseQuery;
import com.diamondfire.helpbot.sys.database.impl.queries.BasicQuery;
import com.diamondfire.helpbot.util.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;

import java.sql.ResultSet;
import java.util.*;

public class StaffListCommand extends Command {

    @Override
    public String getName() {
        return "stafflist";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"staff"};
    }

    @Override
    public HelpContext getHelpContext() {
        return new HelpContext()
                .description("Gets current staff members.")
                .category(CommandCategory.GENERAL_STATS);
    }

    @Override
    public ArgumentSet getArguments() {
        return new ArgumentSet();
    }

    @Override
    public Permission getPermission() {
        return Permission.USER;
    }

    @Override
    public void run(CommandEvent event) {
        MultiSelectorBuilder builder = new MultiSelectorBuilder();
        builder.setChannel(event.getChannel().getIdLong());
        builder.setUser(event.getMember().getIdLong());

        new DatabaseQuery()
                .query(new BasicQuery("SELECT * FROM ranks, players " +
                        "WHERE ranks.uuid = players.uuid " +
                        "AND (ranks.support > 0 || ranks.moderation > 0 || ranks.developer = 1 || ranks.builder = 1) AND ranks.retirement = 0"))
                .compile()
                .runAsync((result) -> {
                    Map<Rank, List<String>> ranks = new HashMap<>();
                    registerRank(ranks,
                            Rank.JRHELPER,
                            Rank.HELPER,
                            Rank.EXPERT,
                            Rank.JRMOD,
                            Rank.MOD,
                            Rank.ADMIN,
                            Rank.OWNER,
                            Rank.DEVELOPER);

                    for (ResultSet set : result) {
                        String name = StringUtil.display(set.getString("name"));
                        int supportNum = set.getInt("support");
                        int moderationNum = set.getInt("moderation");
                        int developerNum = set.getInt("developer");

                        if (developerNum != 0) {
                            ranks.get(Rank.DEVELOPER).add(name);
                        }

                        if (moderationNum == 0 && supportNum > 0) {
                            ranks.get(Rank.fromBranch(RankBranch.SUPPORT, supportNum)).add(name);
                        } else if (moderationNum > 0) {
                            ranks.get(Rank.fromBranch(RankBranch.MODERATION, moderationNum)).add(name);
                        }

                    }

                    EmbedBuilder supportPage = new EmbedBuilder();
                    EmbedUtils.addFields(supportPage, ranks.get(Rank.EXPERT), "", "Experts");
                    EmbedUtils.addFields(supportPage, ranks.get(Rank.HELPER), "", "Helpers");
                    EmbedUtils.addFields(supportPage, ranks.get(Rank.JRHELPER), "", "JrHelpers");
                    builder.addPage("Support", supportPage);

                    EmbedBuilder modPage = new EmbedBuilder();
                    EmbedUtils.addFields(modPage, ranks.get(Rank.MOD), "", "Mods");
                    EmbedUtils.addFields(modPage, ranks.get(Rank.JRMOD), "", "JrMods");
                    builder.addPage("Moderation", modPage);

                    EmbedBuilder adminPage = new EmbedBuilder();
                    EmbedUtils.addFields(adminPage, ranks.get(Rank.OWNER), "", "Owner");
                    EmbedUtils.addFields(adminPage, ranks.get(Rank.ADMIN), "", "Admins");
                    builder.addPage("Administration", adminPage);

                    EmbedBuilder devEmbed = new EmbedBuilder();
                    EmbedUtils.addFields(devEmbed, ranks.get(Rank.DEVELOPER), "", "DiamondFire Developers");

                    Guild guild = event.getGuild();
                    Role botDev = guild.getRoleById(589238520145510400L);
                    guild.findMembers((member -> member.getRoles().contains(botDev)))
                            .onSuccess((members) -> {
                                List<String> memberNames = new ArrayList<>();
                                for (Member member : members) {
                                    if (!member.getUser().isBot()) {
                                        memberNames.add(member.getEffectiveName());
                                    }
                                }

                                EmbedUtils.addFields(devEmbed, memberNames, "", "Bot Developers");
                                guild.pruneMemberCache();
                                builder.addPage("Developers", devEmbed);
                                builder.build().send(event.getJDA());
                            });
                });

    }

    private void registerRank(Map<Rank,List<String>> map, Rank... ranks) {
        for (Rank rank : ranks) {
            map.put(rank, new ArrayList<>());
        }
    }

}


