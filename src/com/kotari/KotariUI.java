package com.kotari;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;

/**
 * Created by fuad on 5/2/16.
 */
public class KotariUI {
    public KotariUI() {
        btnPrevPeriod.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });
        nextReadingButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });
        setReadingButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });
        infoButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });
        historyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });
        printButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });
        addCustomerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                NewCustomerDialog dialog = new NewCustomerDialog();
                dialog.setVisible(true);
            }
        });
        deleteCustomerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });
    }

    public static boolean initDatabase() {
        Connection connection = null;
        try {
            Class.forName("org.sqlite.JDBC");

            connection = DriverManager.getConnection(DbFile.connection_string);
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
                    "unique(name));");

            stmt.execute("create table if not exists reading ( " +
                    "reading_id         integer primary key autoincrement, " +
                    "date               text not null, " +
                    "name               text not null);");

            stmt.execute("create table if not exists customer_reading ( " +
                    "r_id integer       references reading(reading_id), " +
                    "c_id integer       references customer(customer_id), " +
                    "previous_reading   integer not null, " +
                    "current_reading    integer not null, " +
                    "delta_change       integer not null, " +
                    "below_50           real not null, " +
                    "above_50           real not null, " +
                    "service_change     real not null, " +
                    "total_payment      real not null);");

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

    public static void main(String[] args) {
        if (!initDatabase()) {
            System.exit(1);
        }
        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        JFrame frame = new JFrame("Kotari");
        frame.setTitle("Kotari");
        frame.setMinimumSize(new Dimension(500, 500));

        frame.setLocationRelativeTo(null);

        frame.setContentPane(new KotariUI().kotariMainPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    private JPanel kotariMainPanel;
    private JLabel textFieldCompanyTitle;
    private JButton btnPrevPeriod;
    private JButton nextReadingButton;
    private JTable table1;
    private JButton infoButton;
    private JButton historyButton;
    private JButton printButton;
    private JButton setReadingButton;
    private JButton addCustomerButton;
    private JButton deleteCustomerButton;
}
