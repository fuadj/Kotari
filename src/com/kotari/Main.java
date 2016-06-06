package com.kotari;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

/**
 * Created by fuad on 6/6/16.
 */
public class Main {
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

        switch (doesAnyUserExist()) {
            case YES:
                EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        LoginDialog dialog = new LoginDialog();
                        dialog.setListener(new DialogOperationListener() {
                            @Override
                            public void operationFinished() {
                                KotariUI mainUi = new KotariUI();
                                mainUi.setVisible(true);
                            }

                            @Override
                            public void operationCanceled() {
                                System.exit(1);
                            }
                        });

                        dialog.setVisible(true);
                    }
                });
                break;
            case NO:
                EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        NewUserDialog dialog = new NewUserDialog();
                        dialog.createRootUser(true);
                        dialog.setListener(new NewUserDialog.UserCreateListener() {
                            @Override
                            public void userCreated(User u) {
                                User.getSingleton().username = u.username;
                                User.getSingleton().role = u.role;
                                KotariUI mainUi = new KotariUI();
                                mainUi.setVisible(true);
                            }

                            @Override
                            public void cancelSelected() {
                                System.exit(1);
                            }
                        });

                        dialog.setVisible(true);
                    }
                });
                break;
            case ERROR:
            default:
                System.exit(1);
        }
    }

    private static final int NO = 1;
    private static final int YES = 2;
    private static final int ERROR = 3;
    private static int doesAnyUserExist() {
        try (Connection conn = DriverManager.
                getConnection(DbUtil.connection_string); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("select * from users");
            boolean exists = false;
            if (rs.next()) {
                exists = true;
            }
            rs.close();

            return exists ? YES : NO;
        } catch (SQLException e) {
            return ERROR;
        }
    }

}
