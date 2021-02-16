/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cien.client;

import com.cien.securesocket.Client;
import com.cien.server.ChatServer;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 *
 * @author Cien
 */
public class Main {

    static {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static final Main MAIN = new Main();

    public static void main(String[] args) throws Exception {
        boolean startServer = false;
        for (String s : args) {
            if (s.equalsIgnoreCase("--server")) {
                startServer = true;
            }
        }
        if (startServer) {
            System.out.println("Starting server at " + AddressConfig.getIp() + ":" + AddressConfig.getPort());
            System.out.println("To change the ip or port open the jar, go to com/cien/client and change address.txt");
            ChatServer.main(args);
        } else {
            MAIN.run();
        }
    }

    private final InetSocketAddress server = AddressConfig.getAddress();
    private final CreateGroup create = new CreateGroup();
    private final Chat chat = new Chat();
    private final EnterGroup enter = new EnterGroup();
    private final Login login = new Login();
    private final List<String> groups = Collections.synchronizedList(new ArrayList<>());
    private final ExecutorService service = Executors.newSingleThreadExecutor();
    private final ExecutorService imageService = Executors.newSingleThreadExecutor();
    private final File cacheFolder = new File("cache");
    private final Map<String, List<Message>> messages = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, String[]> users = Collections.synchronizedMap(new HashMap<>());
    private String selectedGroup = null;
    private String sessionId = null;
    private String user = null;
    private final Client client = new Client("chat", server, true);
    private final Client receiveChatEvents = new Client("chatReceiver", server, true);
    private final Client receiveImageClient = new Client("receiveImage", server, false);
    private final Client imageClient = new Client("image", server, false);
    private boolean shutdown = false;

    private Main() {

    }

    private void onMessageReceived(Client c) throws Exception {
        boolean image = c.getInput().readBoolean();
        String group = c.getInput().readUTF();
        String user = c.getInput().readUTF();
        String text = c.getInput().readUTF();

        if (image) {
            text = "request:" + text;
        }

        Message m = new Message(user, text, image);
        addMessageToGroup(group, m);
        if (group.equals(selectedGroup)) {
            updateCurrentGroupMessageList();
        }
    }

    private void onGroupListUpdate(Client c) throws Exception {
        int size = c.getInput().readInt();
        String[] array = new String[size];
        for (int i = 0; i < size; i++) {
            array[i] = c.getInput().readUTF();
        }
        groups.clear();
        groups.addAll(Arrays.asList(array));
        updateGroupList();
    }

    private void onUserListUpdate(Client c) throws Exception {

        String group = c.getInput().readUTF();

        int usersNumber = c.getInput().readInt();

        String[] array = new String[usersNumber];

        for (int j = 0; j < usersNumber; j++) {

            array[j] = c.getInput().readUTF();

        }

        users.put(group, array);

        updateCurrentUserList();

    }

    private void onDataReceived(Client c) throws Exception {
        String sub = c.getInput().readUTF();
        switch (sub) {
            case "messageReceived":
                onMessageReceived(c);
                break;
            case "updateGroups":
                onGroupListUpdate(c);
                break;
            case "updateUsers":
                onUserListUpdate(c);
                break;
        }
    }

    private void updateCurrentUserList() {
        if (selectedGroup != null) {
            String[] s = users.get(selectedGroup);

            if (s == null) {
                SwingUtilities.invokeLater(() -> {
                    chat.getGroupUserList().setModel(new DefaultListModel<>());
                });
                return;
            }

            DefaultListModel<String> list = new DefaultListModel<>();
            for (String e : s) {
                list.addElement(e);
            }

            SwingUtilities.invokeLater(() -> {
                chat.getGroupUserList().setModel(list);
            });

        } else {
            SwingUtilities.invokeLater(() -> {
                chat.getGroupUserList().setModel(new DefaultListModel<>());
            });
        }
    }

    private void runChatReceiver() {
        try {

            int sleeps = 0;
            boolean sendId = true;
            while (!shutdown) {
                if (sessionId == null) {
                    Thread.sleep(50);
                    continue;
                }

                try {

                    DataOutputStream out = receiveChatEvents.getOutput();
                    DataInputStream in = receiveChatEvents.getInput();

                    if (sendId) {
                        sendId = false;

                        out.writeUTF("login");
                        out.writeUTF(sessionId);
                        out.flush();

                        int response = in.readInt();

                        if (response != 0) {
                            login.getInformationLabel().setText("Faça login novamente.");
                            groups.clear();
                            updateGroupList();
                            login.setVisible(true);
                            chat.setVisible(false);
                            create.setVisible(false);
                            enter.setVisible(false);
                            sessionId = null;
                            sendId = true;
                            continue;
                        }

                    }

                    if (in.available() != 0) {
                        try {
                            onDataReceived(receiveChatEvents);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    } else {
                        Thread.sleep(10);
                        sleeps++;
                    }

                    if (sleeps == 100) {
                        sleeps = 0;

                        out.writeUTF("ping");
                        out.flush();

                    }

                } catch (IOException ex) {
                    try {
                        receiveChatEvents.close();
                        receiveChatEvents.connect();
                        sendId = true;
                    } catch (IOException e) {
                        Thread.sleep(250);
                    }
                }
            }
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void updateCurrentGroupMessageList() {
        if (selectedGroup == null) {
            return;
        }

        List<Message> msgs = messages.get(selectedGroup);
        String result;
        if (msgs != null) {
            result = Message.generateHTML(msgs.toArray(new Message[msgs.size()]));
        } else {
            result = "";
        }
        SwingUtilities.invokeLater(() -> {
            chat.getTextPane().setText(result);
        });
    }

    private void addMessageToGroup(String group, Message m) {
        List<Message> msgs = messages.get(group);
        if (msgs == null) {
            msgs = Collections.synchronizedList(new ArrayList<>());
            messages.put(group, msgs);
        }
        msgs.add(m);
    }

    public void beep() {
        Toolkit.getDefaultToolkit().beep();
    }

    public InetSocketAddress getServerAddress() {
        return server;
    }

    public Chat getChat() {
        return chat;
    }

    public CreateGroup getCreate() {
        return create;
    }

    public EnterGroup getEnter() {
        return enter;
    }

    public Login getLogin() {
        return login;
    }

    private void updateGroupList() {
        DefaultListModel<String> model = new DefaultListModel<>();
        for (String s : groups) {
            model.addElement(s);
        }
        try {
            SwingUtilities.invokeAndWait(() -> {
                chat.getGroupList().setModel(model);
                chat.getGroupList().setSelectedValue(selectedGroup, true);
            });
        } catch (InterruptedException | InvocationTargetException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void runImageDownloader() {
        while (!shutdown) {
            try {

                for (Entry<String, List<Message>> e : messages.entrySet()) {
                    String group = e.getKey();
                    List<Message> messages = e.getValue();

                    if (group != null && messages != null) {
                        for (Message m : messages.toArray(new Message[messages.size()])) {
                            if (m.isImage() && m.getData().startsWith("request:")) {
                                String name = m.getData().substring("request:".length());

                                try {
                                    receiveImageClient.setWorking(true);

                                    try {
                                        receiveImageClient.getOutput().writeUTF("downloadImage");
                                        receiveImageClient.getOutput().flush();
                                    } catch (IOException ex) {
                                        receiveImageClient.connect();
                                        receiveImageClient.setWorking(true);
                                        receiveImageClient.getOutput().writeUTF("downloadImage");
                                    }

                                    receiveImageClient.getOutput().writeUTF(name);
                                    receiveImageClient.getOutput().flush();

                                    int response = receiveImageClient.getInput().readInt();

                                    switch (response) {
                                        case 0:
                                            m.setData("notfound:" + name);
                                            if (group.equals(selectedGroup)) {
                                                updateCurrentGroupMessageList();
                                            }
                                            continue;
                                        case 1:
                                            continue;
                                    }

                                    File write = new File(cacheFolder, name);

                                    write.getParentFile().mkdirs();

                                    long size = receiveImageClient.getInput().readLong();

                                    try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(write))) {
                                        for (long i = 0; i < size; i++) {
                                            out.write(receiveImageClient.getInput().read());
                                        }
                                    }

                                    m.setData(write.getAbsolutePath());

                                    if (group.equals(selectedGroup)) {
                                        updateCurrentGroupMessageList();
                                    }

                                    receiveImageClient.setWorking(false);
                                    write.deleteOnExit();
                                } catch (IOException ex) {
                                    receiveImageClient.close();
                                    ex.printStackTrace();
                                }

                            }
                        }
                    }
                }

                Thread.sleep(1000);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void run() throws Exception {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            cacheFolder.delete();

            shutdown = true;

            if (sessionId != null) {
                try {
                    receiveChatEvents.getOutput().writeUTF("shutdown");
                    receiveChatEvents.getOutput().writeUTF(sessionId);
                    receiveChatEvents.getOutput().flush();
                    receiveChatEvents.getInput().readInt();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }

            client.close();
            receiveImageClient.close();
            imageClient.close();
            receiveChatEvents.close();
        }, "Kill Connections"));

        new Thread(this::runImageDownloader, "Image Downloader").start();
        new Thread(this::runChatReceiver, "Chat Receiver").start();

        Function<char[], Void> clearArray = (c) -> {
            for (int i = 0; i < c.length; i++) {
                c[i] = '\0';
            }
            return null;
        };

        Function<char[], byte[]> encrypt = (c) -> {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] alloc = new byte[c.length * 2];
                ByteBuffer buffer = ByteBuffer.wrap(alloc);
                for (int i = 0; i < c.length; i++) {
                    buffer.putChar(c[i]);
                }
                byte[] digested = digest.digest(buffer.array());
                for (int i = 0; i < alloc.length; i++) {
                    alloc[i] = '\0';
                }
                return digested;
            } catch (NoSuchAlgorithmException ex) {
                return null;
            }
        };

        chat.getUploadImageButton().addActionListener((ActionEvent e) -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
            fileChooser.setFileFilter(new FileNameExtensionFilter("Image (png, jpeg, jpg)", "png", "jpeg", "jpg"));
            fileChooser.addActionListener((ActionEvent f) -> {
                if (!f.getActionCommand().equals(JFileChooser.APPROVE_SELECTION)) {
                    return;
                }
                File file = fileChooser.getSelectedFile();

                String group = selectedGroup;
                imageService.submit(() -> {
                    try {
                        imageClient.setWorking(true);

                        try {
                            imageClient.getOutput().writeUTF("uploadImage");
                            imageClient.getOutput().flush();
                        } catch (IOException ex) {
                            imageClient.connect();
                            imageClient.setWorking(true);
                            imageClient.getOutput().writeUTF("uploadImage");
                        }

                        imageClient.getOutput().writeUTF(sessionId);
                        imageClient.getOutput().writeUTF(group);
                        imageClient.getOutput().writeLong(file.length());
                        imageClient.getOutput().flush();

                        int response = imageClient.getInput().readInt();

                        if (response == 0) {
                            login.getInformationLabel().setText("Faça login novamente.");
                            groups.clear();
                            updateGroupList();
                            login.setVisible(true);
                            chat.setVisible(false);
                            create.setVisible(false);
                            enter.setVisible(false);
                            return;
                        }

                        switch (response) {
                            case 1:
                                try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file))) {
                                int r;
                                while ((r = in.read()) != -1) {
                                    imageClient.getOutput().write(r);
                                }
                            }
                            imageClient.getOutput().flush();
                            break;
                            case 3:
                                groups.remove(group);
                                updateGroupList();
                                beep();
                                break;
                            default:
                                beep();
                                break;
                        }

                        imageClient.setWorking(false);

                    } catch (IOException ex) {
                        imageClient.close();
                        beep();
                    }
                });
            });
            fileChooser.showDialog(chat, "Upload");
        });

        chat.getTextField().addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {

                    String text = chat.getTextField().getText();

                    if (!text.isEmpty()) {

                        service.submit(() -> {
                            String group = selectedGroup;
                            try {
                                client.setWorking(true);

                                try {
                                    client.getOutput().writeUTF("sendMessage");
                                    client.getOutput().flush();
                                } catch (IOException ex) {
                                    client.connect();
                                    client.setWorking(true);
                                    client.getOutput().writeUTF("sendMessage");
                                }

                                client.getOutput().writeUTF(sessionId);
                                client.getOutput().writeUTF(group);
                                client.getOutput().writeUTF(text);
                                client.getOutput().flush();

                                int response = client.getInput().readInt();

                                switch (response) {
                                    case 0:
                                        login.getInformationLabel().setText("Faça login novamente.");
                                        groups.clear();
                                        updateGroupList();
                                        login.setVisible(true);
                                        chat.setVisible(false);
                                        create.setVisible(false);
                                        enter.setVisible(false);
                                        break;
                                    case 1:
                                        groups.remove(group);
                                        updateGroupList();
                                        break;
                                    case 2:
                                        chat.getTextField().setText("");
                                        break;
                                }

                                client.setWorking(false);
                            } catch (IOException ex) {
                                client.close();
                                beep();
                            }
                        });

                    } else {
                        beep();
                    }

                    e.consume();
                }
            }
        });

        chat.getExitGroup().addActionListener((e) -> {
            chat.getExitGroup().setEnabled(false);
            service.submit(() -> {
                String sel = selectedGroup;
                try {
                    client.setWorking(true);

                    try {
                        client.getOutput().writeUTF("exitGroup");
                        client.getOutput().flush();
                    } catch (IOException ex) {
                        client.connect();
                        client.setWorking(true);
                        client.getOutput().writeUTF("exitGroup");
                    }

                    client.getOutput().writeUTF(sessionId);
                    client.getOutput().writeUTF(sel);
                    client.getOutput().flush();

                    int response = client.getInput().readInt();

                    switch (response) {
                        case 0: // need to login again
                            login.getInformationLabel().setText("Faça login novamente.");
                            groups.clear();
                            updateGroupList();
                            login.setVisible(true);
                            chat.setVisible(false);
                            create.setVisible(false);
                            enter.setVisible(false);
                            beep();
                            break;
                        case 1:
                            groups.remove(sel);
                            updateGroupList();
                            break;
                    }

                    client.setWorking(false);
                } catch (IOException ex) {
                    client.close();
                    beep();
                }
                chat.getExitGroup().setEnabled(true);
            });

        });

        chat.getGroupList().addListSelectionListener((e) -> {
            selectedGroup = chat.getGroupList().getSelectedValue();

            if (selectedGroup != null) {
                chat.getExitGroup().setEnabled(true);
                updateCurrentGroupMessageList();
                chat.getTextField().setEnabled(true);
                chat.getUploadImageButton().setEnabled(true);
            } else {
                chat.getTextPane().setText("<p style=\"margin-top: 0;\">Entre, crie ou escolha um grupo!</p>");
                chat.getTextField().setEnabled(false);
                chat.getUploadImageButton().setEnabled(false);
            }

            updateCurrentUserList();
        });

        enter.getEnterGroupButton().addActionListener((e) -> {
            String name = enter.getGroupNameField().getText();

            if (name.isEmpty()) {
                enter.getErrorLabel().setText("Digite um nome.");
                beep();
                return;
            }

            char[] pass = enter.getGroupPassField().getPassword();
            if (pass.length == 0) {
                enter.getErrorLabel().setText("Digite uma senha.");
                beep();
                return;
            }

            enter.getEnterGroupButton().setEnabled(false);
            service.submit(() -> {
                try {
                    client.setWorking(true);

                    try {
                        client.getOutput().writeUTF("enterGroup");
                        client.getOutput().flush();
                    } catch (IOException ex) {
                        client.connect();
                        client.setWorking(true);
                        client.getOutput().writeUTF("enterGroup");
                    }

                    client.getOutput().writeUTF(sessionId);
                    client.getOutput().writeUTF(name);
                    byte[] encrypted = encrypt.apply(pass);
                    clearArray.apply(pass);
                    client.getOutput().writeShort(encrypted.length);
                    client.getOutput().write(encrypted);
                    for (int i = 0; i < encrypted.length; i++) {
                        encrypted[i] = '\0';
                    }
                    client.getOutput().flush();

                    int response = client.getInput().readInt();

                    switch (response) {
                        case 0: // need to login again
                            login.getInformationLabel().setText("Faça login novamente.");
                            groups.clear();
                            updateGroupList();
                            login.setVisible(true);
                            chat.setVisible(false);
                            create.setVisible(false);
                            enter.setVisible(false);
                            enter.getGroupNameField().setText("");
                            enter.getGroupPassField().setText("");
                            beep();
                            break;
                        case 1: // name/pass incorrect
                            enter.getErrorLabel().setText("Nome ou senha incorretos.");
                            beep();
                            break;
                        case 2: // already on group
                            enter.getErrorLabel().setText("Você já está nesse grupo.");
                            beep();
                            break;
                        case 3: // ok
                            enter.setVisible(false);
                            enter.getGroupNameField().setText("");
                            enter.getGroupPassField().setText("");
                            break;
                    }

                    client.setWorking(false);
                } catch (IOException ex) {
                    client.close();
                    enter.getErrorLabel().setText("Não foi possível conectar ao servidor.");
                    beep();
                }
                enter.getEnterGroupButton().setEnabled(true);
            });
        });

        chat.getEnterGroupButton().addActionListener((e) -> {
            enter.setVisible(true);
        });

        create.getCreateGroupButton().addActionListener((e) -> {
            String name = create.getGroupNameField().getText();

            if (name.isEmpty()) {
                create.getErrorLabel().setText("Digite um nome.");
                beep();
                return;
            }

            char[] pass1 = create.getPasswordField().getPassword();
            if (pass1.length == 0) {
                create.getErrorLabel().setText("Digite uma senha.");
                beep();
                return;
            }

            char[] pass2 = create.getPasswordCheckField().getPassword();
            if (pass2.length == 0) {
                create.getErrorLabel().setText("Digite a senha novamente.");
                beep();
                clearArray.apply(pass1);
                return;
            }

            if (!Arrays.equals(pass1, pass2)) {
                create.getErrorLabel().setText("As senhas não coincidem.");
                beep();
                clearArray.apply(pass1);
                clearArray.apply(pass2);
                return;
            }

            clearArray.apply(pass2);

            create.getCreateGroupButton().setEnabled(false);
            service.submit(() -> {
                try {
                    client.setWorking(true);

                    try {
                        client.getOutput().writeUTF("createGroup");
                        client.getOutput().flush();
                    } catch (IOException ex) {
                        client.connect();
                        client.setWorking(true);
                        client.getOutput().writeUTF("createGroup");
                    }

                    client.getOutput().writeUTF(sessionId);
                    client.getOutput().writeUTF(name);
                    byte[] encrypted = encrypt.apply(pass1);
                    clearArray.apply(pass1);
                    client.getOutput().writeShort(encrypted.length);
                    client.getOutput().write(encrypted);
                    for (int i = 0; i < encrypted.length; i++) {
                        encrypted[i] = '\0';
                    }
                    client.getOutput().flush();

                    int response = client.getInput().readInt();

                    switch (response) {
                        case 0: // need to login again
                            login.getInformationLabel().setText("Faça login novamente.");
                            groups.clear();
                            updateGroupList();
                            login.setVisible(true);
                            chat.setVisible(false);
                            create.setVisible(false);
                            create.getErrorLabel().setText("");
                            create.getGroupNameField().setText("");
                            create.getPasswordCheckField().setText("");
                            create.getPasswordField().setText("");
                            enter.setVisible(false);
                            beep();
                            break;
                        case 1: // group already exists
                            create.getErrorLabel().setText("Grupo já existente");
                            beep();
                            break;
                        case 2: // ok
                            create.setVisible(false);
                            create.getErrorLabel().setText("");
                            create.getGroupNameField().setText("");
                            create.getPasswordCheckField().setText("");
                            create.getPasswordField().setText("");
                            chat.getGroupList().setSelectedValue(name, true);
                            break;
                    }

                    client.setWorking(false);
                } catch (IOException ex) {
                    client.close();
                    create.getErrorLabel().setText("Não foi possível conectar ao servidor.");
                    beep();
                }
                create.getCreateGroupButton().setEnabled(true);
            });
        });

        chat.getNewGroupButton().addActionListener((e) -> {
            create.setVisible(true);
        });

        login.getStartSession().addActionListener((e) -> {
            user = login.getUsernameField().getText();
            boolean register = login.getRegisterCheckbox().isSelected();

            if (user.isEmpty()) {
                login.getInformationLabel().setText("Digite um nome de usuário.");
                beep();
                return;
            }

            char[] pass1 = login.getPasswordField().getPassword();
            if (pass1.length == 0) {
                login.getInformationLabel().setText("Digite uma senha.");
                beep();
                clearArray.apply(pass1);
                return;
            }

            char[] pass2 = login.getPasswordCheckField().getPassword();
            if (register) {
                if (pass2.length == 0) {
                    login.getInformationLabel().setText("Digite sua senha novamente");
                    beep();
                    clearArray.apply(pass1);
                    return;
                }

                if (!Arrays.equals(pass1, pass2)) {
                    login.getInformationLabel().setText("As senhas não coincidem");
                    beep();
                    clearArray.apply(pass1);
                    clearArray.apply(pass2);
                    return;
                }
            }

            clearArray.apply(pass2);

            login.getStartSession().setEnabled(false);
            service.submit(() -> {
                try {
                    client.setWorking(true);

                    String protocol = register ? "register" : "login";

                    try {
                        client.getOutput().writeUTF(protocol);
                        client.getOutput().flush();
                    } catch (IOException ex) {
                        client.connect();
                        client.setWorking(true);
                        client.getOutput().writeUTF(protocol);
                    }

                    client.getOutput().writeUTF(user);
                    byte[] encrypted = encrypt.apply(pass1);
                    client.getOutput().writeShort(encrypted.length);
                    client.getOutput().write(encrypted);
                    clearArray.apply(pass1);
                    for (int i = 0; i < encrypted.length; i++) {
                        encrypted[i] = '\0';
                    }
                    client.getOutput().flush();

                    int response = client.getInput().readInt();

                    if (response != 0) {
                        if (register) {
                            login.getInformationLabel().setText("Usuário já registrado.");
                        } else {
                            login.getInformationLabel().setText("Usuário/Senha incorretos.");
                        }
                        beep();
                        login.getStartSession().setEnabled(true);
                        return;
                    }

                    sessionId = client.getInput().readUTF();

                    System.out.println("Logged in with session id " + sessionId);

                    login.getPasswordCheckField().setText("");
                    login.getPasswordField().setText("");
                    login.getUsernameField().setText("");

                    login.setVisible(false);
                    chat.setTitle("Chat do Cien - " + user);
                    chat.setVisible(true);

                    client.setWorking(false);
                } catch (IOException ex) {
                    client.close();
                    login.getInformationLabel().setText("Não foi possível conectar ao servidor");
                    beep();
                }
                login.getStartSession().setEnabled(true);
            });

        });
        login.setVisible(true);

    }

}
