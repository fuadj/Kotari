package com.kotari;

import java.sql.*;

/**
 * Created by fuad on 5/25/16.
 */
public class DbUtil {
    public static final String connection_string = "jdbc:sqlite:kotari.sqlite";

    // This is the id assigned to customers who don't have a meter
    public static final int DEFAULT_METER_ID = 1;

    public static final int D_TRUE = 1;
    public static final int D_FALSE = 0;

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

            stmt.execute("create table if not exists meter ( " +
                    "meter_id           integer primary key autoincrement, " +

                    // Just for simplicity, we set this to true ONLY for the DEFAULT meter
                    "is_default         integer not null, " +

                    "property_number    integer not null, " +
                    "initial_reading    integer not null, " +
                    "date_of_install    text, " +
                    "floor              text, " +
                    "shop               text, " +
                    "unique(property_number));");

            ResultSet rs = stmt.executeQuery("select meter_id from meter where meter_id = " + DEFAULT_METER_ID);
            // The default doesn't exist, create it
            if (!rs.next()) {
                rs.close();

                stmt.execute("insert into meter (is_default, property_number, initial_reading) values " +
                    String.format(" (%d, %d, %d);", DEFAULT_METER_ID, 0, 0));
            } else {
                rs.close();
            }

            stmt.execute("create table if not exists customer ( " +
                    "customer_id        integer primary key autoincrement, " +
                    "name               text not null, " +
                    "address            text, " +

                    // This is used as a boolean to distinguish b/n the deleted and the not
                    // 1 is active, 0 is deleted
                    // We keep the deleted so as to keep a backup in the readings table
                    "is_active          integer not null, " +

                    "phone_number       text, " +
                    "contract_no        text, " +
                    "type_of_business   text, " +
                    "tariff_type        integer not null, " +
                    "customer_meter_id  integer DEFAULT " + DEFAULT_METER_ID +
                    "   REFERENCES meter(meter_id) ON DELETE SET DEFAULT, " +
                    "unique(name));");

            stmt.execute("create table if not exists reading ( " +
                    "reading_id         integer primary key autoincrement, " +
                    "date               text not null);");

            stmt.execute("create table if not exists customer_reading ( " +
                    "r_id integer       references reading(reading_id) ON DELETE CASCADE, " +
                    "m_id integer       references meter(meter_id) ON DELETE CASCADE, " +

                    // We are also referring the customer here so if a meter
                    // changes owners(transferred from one customer to other) the history
                    // should still be correct with the previous customer for the previous reading
                    "c_id integer       references customer(customer_id) ON DELETE CASCADE, " +

                    "previous_reading   integer not null, " +
                    "current_reading    integer not null, " +
                    "delta_change       integer not null, " +
                    "below_50           real not null, " +
                    "above_50           real not null, " +
                    "service_charge     real not null, " +
                    "total_payment      real not null, " +
                    "unique(r_id, m_id) ON CONFLICT REPLACE);");

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
