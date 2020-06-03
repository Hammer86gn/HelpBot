package com.diamondfire.helpbot.command.impl.stats;

import com.diamondfire.helpbot.command.arguments.Argument;
import com.diamondfire.helpbot.command.arguments.value.StringArg;
import com.diamondfire.helpbot.command.impl.Command;
import com.diamondfire.helpbot.command.permissions.Permission;
import com.diamondfire.helpbot.events.CommandEvent;
import com.diamondfire.helpbot.util.ConnectionGiver;
import net.dv8tion.jda.api.EmbedBuilder;

import java.sql.*;
import java.util.concurrent.TimeUnit;

public class StatsCommand extends Command {

    @Override
    public String getName() {
        return "stats";
    }

    @Override
    public String getDescription() {
        return "Get info on the a certain user's support stats.";
    }

    @Override
    public Argument getArgument() {
        return new StringArg("Username", false);
    }

    @Override
    public Permission getPermission() {
        return Permission.SUPPORT;
    }

    @Override
    public void run(CommandEvent event) {
        EmbedBuilder builder = new EmbedBuilder();

        String name = event.getParsedArgs().isEmpty() ? event.getMember().getEffectiveName() : event.getArguments()[0];


        try (Connection connection = ConnectionGiver.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) AS count, (COUNT(*) < 5) AS bad FROM support_sessions " +
                     "WHERE staff = ? AND time > CURRENT_TIMESTAMP() - INTERVAL 30 DAY;")) {


            statement.setString(1, name);

            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                builder.addField("Bad?", rs.getInt("bad") == 1 ? "Yes!" : "No", true);
                builder.addField("Monthly Sessions", rs.getInt("count") + "", true);

            }
            rs.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        try (Connection connection = ConnectionGiver.getConnection();
             PreparedStatement statementStats = connection.prepareStatement("" +
                     "SELECT COUNT(*) AS count," +
                     "MIN(time) as earliest_time," +
                     "MAX(time) AS latest_time," +
                     "COUNT(DISTINCT name) AS unique_helped FROM support_sessions WHERE staff = ?;")) {


            statementStats.setString(1, name);

            ResultSet rsStats = statementStats.executeQuery();

            if (rsStats.next()) {
                if (rsStats.getInt("count") == 0) {

                    builder.clearFields();
                    builder.setTitle("Player has no stats!");
                    event.getChannel().sendMessage(builder.build()).queue();
                    return;
                }

                builder.addField("Total Sessions", rsStats.getInt("count") + "", true);
                builder.addField("Unique Players", rsStats.getInt("unique_helped") + "", true);
                builder.addField("Earliest Session", formatDate(rsStats.getDate("earliest_time")), true);
                builder.addField("Latest Session", formatDate(rsStats.getDate("latest_time")), true);

            }
            rsStats.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try (Connection connection = ConnectionGiver.getConnection();
             PreparedStatement statementStats = connection.prepareStatement("" +
                     "SELECT AVG(duration) AS average_duration," +
                     "MIN(duration) AS shortest_duration," +
                     "MAX(duration) AS longest_duration " +
                     "FROM support_sessions WHERE duration != 0 AND staff = ?;")) {


            statementStats.setString(1, name);

            ResultSet rsStats = statementStats.executeQuery();

            if (rsStats.next()) {
                builder.addField("Average Session Time", format(rsStats.getInt(("average_duration"))), true);
                builder.addField("Shortest Session Time", format(rsStats.getInt("shortest_duration")), true);
                builder.addField("Longest Session Time", format(rsStats.getInt("longest_duration")), true);
            }
            rsStats.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }


        builder.setAuthor(name);
        builder.setTitle("Support Stats");

        event.getChannel().sendMessage(builder.build()).queue();


    }

    public String format(int millis) {
        StringBuilder builder = new StringBuilder();

        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        if (hours > 0) {
            builder.append(hours + "h");
        }

        long mins = TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis));
        if (mins > 0) {
            builder.append(" " + mins + "m");
        }
        long secs = TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis));
        if (secs != 0) {
            builder.append(" " + secs + "s");
        } else {
            builder.append("0." + TimeUnit.MILLISECONDS.toMillis(millis) + "s");
        }

        return builder.toString();

    }

    @SuppressWarnings("deprecation")
    public String formatDate(Date date) {
        return date.toLocalDate().getDayOfMonth() + "/" + (date.getMonth() + 1) + "/" + (date.getYear() + 1900);
    }

}
