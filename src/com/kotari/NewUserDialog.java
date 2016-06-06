package com.kotari;

import com.sun.deploy.util.StringUtils;
import javafx.util.Pair;

import javax.jws.soap.SOAPBinding;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.Vector;
import java.util.concurrent.ExecutionException;

public class NewUserDialog extends JDialog {
    public interface UserCreateListener {
        void userCreated(User u);
        void cancelSelected();
    }
    private UserCreateListener mListener;

    public void setListener(UserCreateListener listener) {
        mListener = listener;
    }

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField dialog_new_user_name;
    private JComboBox dialog_new_user_role_spinnner;
    private JLabel dialog_new_user_role_label;
    private JPasswordField dialog_new_user_password1;
    private JPasswordField dialog_new_user_password2;

    public NewUserDialog() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        setTitle("Set User Role");
        setMinimumSize(new Dimension(400, 250));
        setLocationRelativeTo(null);

        DocumentListener listener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                setOkBtnStatus();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                setOkBtnStatus();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                setOkBtnStatus();
            }
        };
        setOkBtnStatus();
        dialog_new_user_name.getDocument().addDocumentListener(listener);
        dialog_new_user_password1.getDocument().addDocumentListener(listener);
        dialog_new_user_password2.getDocument().addDocumentListener(listener);

        Vector<String> roles = new Vector<>();
        for (int i = 0; i < 3; i++) {
            roles.add(User.GetRoleName(i+1));
        }
        dialog_new_user_role_spinnner.setModel(new DefaultComboBoxModel<>(roles));

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        updateUIForConfiguration();
        setOkBtnStatus();
    }

    void updateUIForConfiguration() {
        dialog_new_user_role_label.setVisible(!mCreateRoot);
        dialog_new_user_role_spinnner.setVisible(!mCreateRoot);
    }

    boolean is_valid_password(String s) {
        return s.length() > 4;
    }

    void setOkBtnStatus() {
        boolean is_name_set = !dialog_new_user_name.getText().trim().isEmpty();
        String pass1 = new String(dialog_new_user_password1.getPassword());
        String pass2 = new String(dialog_new_user_password2.getPassword());

        boolean passwords_match = is_valid_password(pass1) && pass1.equals(pass2);

        buttonOK.setEnabled(is_name_set && passwords_match);
    }

    Pair<Boolean, String> createUser(User u) {
        try (Connection conn = DriverManager.getConnection(DbUtil.connection_string)) {
            PreparedStatement stmt = conn.prepareStatement(
                    "select * from users where name = ?");
            stmt.setQueryTimeout(5);
            stmt.setString(1, u.username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                rs.close();
                return new Pair<>(Boolean.FALSE,
                        String.format("User %s already exists", u.username));
            }
            rs.close();
            stmt.close();

            stmt = conn.prepareStatement("insert into users " +
                    "(name, pass_hash, role) values " +
                    "(?, ?, ?)");
            stmt.setQueryTimeout(5);
            stmt.setString(1, u.username);
            stmt.setString(2, u.hash);
            stmt.setInt(3, u.role);
            stmt.execute();
            stmt.close();

            return new Pair<>(Boolean.TRUE, null);
        } catch (SQLException e) {
            return new Pair<>(Boolean.FALSE, e.getMessage());
        }
    }

    Pair<Boolean, String> updateUser(User u) {
        try (Connection conn = DriverManager.getConnection(DbUtil.connection_string)) {
            PreparedStatement stmt = conn.prepareStatement(
                    "update users set pass_hash = ?, role = ? " +
                    " where id = ?");
            stmt.setQueryTimeout(5);
            stmt.setString(1, u.hash);
            stmt.setInt(2, u.role);
            stmt.setInt(3, u.id);

            return new Pair<>(stmt.executeUpdate() == 1, null);
        } catch (SQLException e) {
            return new Pair<>(Boolean.FALSE, e.getMessage());
        }
    }

    private void onOK() {
        String name = dialog_new_user_name.getText().trim().toLowerCase();
        String pass = new String(dialog_new_user_password1.getPassword());
        String hash = User.hashPassword(pass);

        int user_role;
        if (mCreateRoot) {
            user_role = User.ROLE_ROOT;
        } else {
            user_role = dialog_new_user_role_spinnner.getSelectedIndex()+1;
        }

        final User u = new User();
        u.username = name;
        u.hash = hash;
        u.role = user_role;
        u.id = mUserId;

        new SwingWorker<Pair<Boolean, String>, Void>() {
            @Override
            protected Pair<Boolean, String> doInBackground() throws Exception {
                if (mIsEditing) {
                    return updateUser(u);
                } else {
                    return createUser(u);
                }
            }

            @Override
            protected void done() {
                try {
                    Pair<Boolean, String> result = get();
                    if (result.getKey() == true) {
                        mListener.userCreated(u);
                        dispose();
                    } else {
                        mListener.cancelSelected();
                    }
                } catch (InterruptedException | ExecutionException e) {
                }
            }
        }.execute();
    }

    private void onCancel() {
        mListener.cancelSelected();
        dispose();
    }

    public void createRootUser(boolean create_root) {
        mCreateRoot = create_root;
        updateUIForConfiguration();
    }

    public void setUserDialogArgs(int user_id, boolean is_editing) {
        mUserId = user_id;
        mIsEditing = is_editing;
    }

    private int mUserId = 0;
    private boolean mIsEditing = false;
    private boolean mCreateRoot = false;

    public static void main(String[] args) {
        NewUserDialog dialog = new NewUserDialog();
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }
}
