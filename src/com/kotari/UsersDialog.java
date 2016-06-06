package com.kotari;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class UsersDialog extends JDialog {
    private DialogOperationListener mListener;
    public void setListener(DialogOperationListener listener) { mListener = listener; }

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonClose;
    private JButton dialog_user_btn_add;
    private JButton dialog_user_btn_edit;
    private JButton dialog_user_btn_delete;
    private JTable dialog_member_table;

    public UsersDialog() {
        setContentPane(contentPane);
        setModal(true);
        setTitle("Set Use Roles");
        getRootPane().setDefaultButton(buttonOK);
        setMinimumSize(new Dimension(500, 400));

        buttonClose.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onClose();
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onClose();
            }
        });

        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onClose();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        dialog_user_btn_add.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                NewUserDialog dialog = new NewUserDialog();
                dialog.setListener(new NewUserDialog.UserCreateListener() {
                    @Override
                    public void userCreated(User u) { updateDisplay(); }
                    @Override
                    public void cancelSelected() { updateDisplay(); }
                });

                dialog.setVisible(true);
            }
        });
        dialog_user_btn_edit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                NewUserDialog dialog = new NewUserDialog();
                dialog.setUserDialogArgs(getSelectedUser().id, true);
                dialog.setListener(new NewUserDialog.UserCreateListener() {
                    @Override
                    public void userCreated(User u) { updateDisplay(); }
                    @Override
                    public void cancelSelected() { updateDisplay(); }
                });

                dialog.setVisible(true);
            }
        });
        dialog_user_btn_delete.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int user_id = getSelectedUser().id;
                String username = getSelectedUser().username;

                int confirm_result = JOptionPane.showConfirmDialog(null,
                        "Do you want to delete: " + username,
                        "Are you Sure? ",
                        JOptionPane.YES_NO_OPTION);
                if (confirm_result == JOptionPane.YES_OPTION) {
                    new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() throws Exception {
                            try (Connection conn = DriverManager.
                                    getConnection(DbUtil.connection_string); Statement stmt = conn.createStatement()) {
                                stmt.execute("delete from users where id = " + user_id);
                            } catch (SQLException e) {
                            }
                            return null;
                        }

                        @Override
                        protected void done() {
                            updateDisplay();
                        }
                    }.execute();
                }
            }
        });

        dialog_member_table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        dialog_member_table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                setActionButtonStatus();
            }
        });
        updateDisplay();
    }

    private User getSelectedUser() {
        int selected_row = dialog_member_table.getSelectedRow();
        int model_index = dialog_member_table.convertRowIndexToModel(selected_row);
        return row_to_user_mapping.get(model_index);
    }

    void updateDisplay() {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                updateTableModel();
                return null;
            }

            @Override
            protected void done() {
                dialog_member_table.setModel(tableModel);
                setActionButtonStatus();
            }
        }.execute();
    }

    void setActionButtonStatus() {
        int[] selected_rows = dialog_member_table.getSelectedRows();
        boolean is_selected = false;
        if (selected_rows.length > 0) {
            is_selected = true;
        }

        //dialog_user_btn_add.setEnabled(is_selected);
        dialog_user_btn_edit.setEnabled(is_selected);
        dialog_user_btn_delete.setEnabled(is_selected);
    }

    void updateTableModel() {
        // clear out previous data
        tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        row_to_user_mapping = new HashMap<>();

        try (Connection conn = DriverManager.
                getConnection(DbUtil.connection_string); Statement stmt = conn.createStatement()) {

            int current_user = User.getSingleton().id;
            // The user should see themselves in the list, could be dangerous
            ResultSet rs = stmt.executeQuery("select id, name, role from users " +
                    " where role != " + User.ROLE_ROOT + " AND " +
                    " id != " + current_user);

            Vector<Vector<Object>> data = new Vector<>();
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                User user = new User();
                user.id = rs.getInt(1);
                user.username = rs.getString(2);
                user.role = rs.getInt(3);

                row.add(user.username);
                row.add(User.GetRoleName(user.role));
                row_to_user_mapping.put(data.size(), user);
                data.add(row);
            }

            Vector<String> columnNames = new Vector<>();
            columnNames.add("Name");
            columnNames.add("Role");

            tableModel.setDataVector(data, columnNames);
        } catch (SQLException e) {
            System.err.println("Exception in loading user data: " + e.getMessage());
        }
    }

    private DefaultTableModel tableModel;
    private Map<Integer, User> row_to_user_mapping = null;

    private void onClose() {
        mListener.operationCanceled();
        dispose();
    }

    public static void main(String[] args) {
        UsersDialog dialog = new UsersDialog();
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }
}
