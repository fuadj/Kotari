package com.kotari;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by fuad on 5/25/16.
 */
public class DbUtil {
    public static final String connection_string = "jdbc:sqlite:kotari.sqlite";

    public static boolean initDatabase() {
        Connection connection = null;
        try {
            Class.forName("org.sqlite.JDBC");

            connection = DriverManager.getConnection(DbUtil.connection_string);
            Statement stmt = connection.createStatement();
            stmt.setQueryTimeout(10);       // this is seconds

            stmt.execute("PRAGMA foreign_keys = ON");

            // this table is only used to store the building owner company's details
            stmt.execute("create table if not exists company ( " +
                    // this id is used to check if the company exists(we always set it to 1 for the company)
                    "id                 integer, " +

                    "name               text not null, " +
                    "contact_info       text, " +
                    "location           text);");

            stmt.execute("create table if not exists customer ( " +
                    "customer_id        integer primary key autoincrement, " +
                    "name               text not null, " +
                    "floor              text, " +
                    "shop_location      text, " +
                    "date_of_install    text, " +
                    "contract_no        text, " +
                    "type_of_business   text, " +
                    "initial_reading    integer not null, " +
                    "tariff_type        integer not null, " +
                    "unique(name));");

            stmt.execute("create table if not exists reading ( " +
                    "reading_id         integer primary key autoincrement, " +
                    "date               text not null);");
                    /*
                    "date               text not null, " +
                    "name               text not null);");
                    */

            stmt.execute("create table if not exists customer_reading ( " +
                    "r_id integer       references reading(reading_id) ON DELETE CASCADE, " +
                    "c_id integer       references customer(customer_id) ON DELETE CASCADE, " +
                    "previous_reading   integer not null, " +
                    "current_reading    integer not null, " +
                    "delta_change       integer not null, " +
                    "below_50           real not null, " +
                    "above_50           real not null, " +
                    "service_charge     real not null, " +
                    "total_payment      real not null, " +
                    "unique(r_id, c_id) ON CONFLICT REPLACE);");

        } catch (ClassNotFoundException e) {
            System.err.println(e);
            return false;
        } catch (SQLException e) {
            System.err.println(e);
            return false;
        } finally {
            try {
                if (connection != null) connection.close();
            } catch (SQLException e) {
                System.err.println(e);
                return false;
            }
        }
        return true;
    }
}
