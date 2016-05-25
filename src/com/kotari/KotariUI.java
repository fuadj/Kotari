package com.kotari;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

/**
 * Created by fuad on 5/2/16.
 */
public class KotariUI extends JFrame {
    public KotariUI() {
        setTitle("Kotari");
        setMinimumSize(new Dimension(500, 500));

        setLocationRelativeTo(null);
        setContentPane(kotariMainPanel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setVisible(true);

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

    public static void main(String[] args) {
        if (!DbUtil.initDatabase()) {
            System.exit(1);
        }
        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new KotariUI().setVisible(true);
            }
        });
    }

    private JPanel kotariMainPanel;
    private JLabel textFieldCompanyTitle;
    private JButton btnPrevPeriod;
    private JButton nextReadingButton;
    private JTable customerListTable;
    private JButton infoButton;
    private JButton historyButton;
    private JButton printButton;
    private JButton setReadingButton;
    private JButton addCustomerButton;
    private JButton deleteCustomerButton;
}
