/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package faescapeplan;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import org.apache.commons.io.FileUtils;
import org.jsoup.Connection;
import org.jsoup.Connection.Response;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author Owner
 */
public class FAEscapePlanUI extends javax.swing.JFrame {
    
    UserData userData = new UserData();  
    
    public static final String VERSION = "0.4";
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:44.0) Gecko/20100101 Firefox/44.0";
    
    public static String tempPath;
    
    public FAEscapePlanUI() {
        initComponents();
        this.loginUser.requestFocusInWindow();
        this.saveLocText.setText(System.getProperty("user.home"));
        this.galleryAction.setSelectedIndex(1);
        this.scrapsAction.setSelectedIndex(1);
        this.favsAction.setSelectedIndex(1);
        this.journalsAction.setSelectedIndex(1);
        this.notesAction.setSelectedIndex(1);
        this.setPaths();
    }
    
    private String getOS() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return "windows";
        } else if (os.contains("mac")) {
            return "mac";
        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            return "unix";
        } else {
            throw new RuntimeException("Unsupported operating system");
        }
    }
    
    private void setPaths() {
        if (getOS().equals("windows")) {
            String winTempPath = System.getProperty("user.home") + "\\AppData\\Local\\Temp\\FAEscapePlan\\";
            createDirectory(winTempPath);
            tempPath = winTempPath;
        } else if (getOS().equals("mac") || getOS().equals("unix")) {
            String unixTempPath = "/tmp/FAEscapePlan/";
            createDirectory(unixTempPath);
            tempPath = unixTempPath;
        }
    }
    
    private void loginConnect() {
        //Transfer login procedure here
    }
    
    private void loginDisconnect() {
        userData.clearData();
        userData.setLoginState(false);
    }
    
    private void getSessionCookies() {
        // REFACTOR
    }
    
    private void unlockForm() {
        this.loginButton.setText("Log Out");
        this.userTitle.setText(userData.getName());
        this.galleryAction.setEnabled(true);
        this.scrapsAction.setEnabled(true);
        //this.favsAction.setEnabled(true);
        this.journalsAction.setEnabled(true);
        //this.notesAction.setEnabled(true);
        this.backupButton.setEnabled(true);
        this.statusText.setText("Logged in as " + userData.getName());
    }
    
    private void resetForm() {
        this.loginUser.setText("");
        this.loginUser.setEditable(true);
        this.loginPass.setText("");
        this.loginPass.setEditable(true);
        this.loginButton.setText("Log In");
        this.userTitle.setText("Username");
        this.refreshButton.setEnabled(false);
        this.galleryAction.setEnabled(false);
        this.scrapsAction.setEnabled(false);
        this.favsAction.setEnabled(false);
        this.journalsAction.setEnabled(false);
        this.notesAction.setEnabled(false);
        this.backupButton.setEnabled(false);
        this.statusText.setText("Not logged in");
        this.iconDisplay.setIcon(new ImageIcon(getClass().getResource("/resources/FAtesticon.png")));
    }
    
    private void getProfileImg() {
        try {
            Document doc = Jsoup.connect("http://www.furaffinity.net/user/" + userData.getName())
                    .cookies(userData.getCookies())
                    .userAgent(USER_AGENT)
                    .get();
            String iconLink = "http:" + doc.getElementsByClass("avatar").get(0).attr("src");
            Response iconResponse = Jsoup.connect(iconLink)
                    .cookies(userData.getCookies())
                    .userAgent(USER_AGENT)
                    .maxBodySize(0)
                    .ignoreContentType(true)
                    .execute();
            String iconPath = tempPath + userData.getName() + ".gif";
            try (FileOutputStream userIcon = new FileOutputStream(new File(iconPath))) {
                userIcon.write(iconResponse.bodyAsBytes());
            }       
            ImageIcon icon = new ImageIcon(iconPath);
            this.iconDisplay.setIcon(icon);
        } catch (IOException ex) {
            Logger.getLogger(FAEscapePlanUI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void updateTextLog(String message) {
        this.logTextBox.append(message + "\n");
        this.logTextBox.update(this.logTextBox.getGraphics());
    }
    /*
    private void loadSettings() {
        try (FileReader file = new FileReader("C:\\")) {
            file.read();
        } catch (IOException ex) {
            Logger.getLogger(FAEscapePlanUI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    */
    private void createDirectory(String path) {
        if (!Files.isDirectory(Paths.get(path))) {
            boolean folderCreated = new File(path).mkdir();
            
            if (folderCreated) {
                System.out.println("Folder created successfully");
            } else {
                System.out.println("Folder could not be created");
            }
        }

    }
    
    private ArrayList<String> indexSection(String section) {
        ArrayList<String> idList = new ArrayList<>();
        boolean itemsRemain = true;
        int pageCount = 1;
        updateTextLog("Indexing " + section + "...");
        
        while (itemsRemain) {
            try {
                Document currentPage = Jsoup.connect("http://www.furaffinity.net/" + section + "/" + userData.getName() + "/" + pageCount + "/") // TEST
                        .timeout(10000)
                        .userAgent(USER_AGENT)
                        .cookies(userData.getCookies())
                        .get();

                if (currentPage.getElementById("no-images") == null) {
                    updateTextLog("Indexing page " + pageCount);
                    Elements elementList = currentPage.getElementsByAttributeValueMatching("id", "sid_\\d+");

                    for (Element item : elementList) {
                        String cleanId = item.attr("id").replace("sid_", "");
                        idList.add(cleanId);
                    }
                    
                    pageCount++;
                } else {
                    itemsRemain = false;
                    updateTextLog("Finished indexing " + section);
                }

            } catch (HttpStatusException ex) {
                Logger.getLogger(FAEscapePlanUI.class.getName()).log(Level.SEVERE, null, ex);
                System.out.println("Could not connect to FA"); // DEBUG
                break;
            } catch (SocketTimeoutException ex) {
                Logger.getLogger(FAEscapePlanUI.class.getName()).log(Level.SEVERE, null, ex);
                System.out.println("Connection timed out"); // DEBUG
                break;
            } catch (IOException ex) {
                Logger.getLogger(FAEscapePlanUI.class.getName()).log(Level.SEVERE, null, ex);
                System.out.println("An IO Exception occurred"); // DEBUG
                break;
            }
        }
        return idList;
    }
    
    private ArrayList<String> indexJournals() {
        ArrayList<String> journalList = new ArrayList<>();
        updateTextLog("Indexing journal entries...");
        
        try {
            Document currentPage = Jsoup.connect("http://www.furaffinity.net/journals/" + userData.getName() + "/")
                    .cookies(userData.getCookies())
                    .userAgent(USER_AGENT)
                    .get();
            Elements elementList = currentPage.getElementsByAttributeValueMatching("id", "jid:\\d+");
            
            for (Element item : elementList) {
                String cleanJid = item.attr("id").replace("jid:", "");
                journalList.add(cleanJid);
            }
            
        } catch (IOException ex) {
            Logger.getLogger(FAEscapePlanUI.class.getName()).log(Level.SEVERE, null, ex);
        }
        return journalList;
    }
    
    private void downloadImageList(ArrayList<String> input, String downloadLoc) {        
        for (String item : input) {
            try {
                updateTextLog("Getting item");
                Document doc = Jsoup.connect("http://www.furaffinity.net/view/" + item + "/")
                        .cookies(userData.getCookies())
                        .userAgent(USER_AGENT)
                        .get();
                String downloadLink = "http:" + doc.getElementById("submissionImg").attr("src");
                String downloadTitle = doc.getElementById("submissionImg").attr("alt");
                String fileType = downloadLink.substring(downloadLink.length() - 4);
                Response response = Jsoup.connect(downloadLink)
                        .cookies(userData.getCookies())
                        .userAgent(USER_AGENT)
                        .maxBodySize(0)
                        .ignoreContentType(true)
                        .execute();
                updateTextLog("Saving item");
                try (FileOutputStream out = new FileOutputStream(new File(downloadLoc + "\\" + downloadTitle + fileType))) {
                    out.write(response.bodyAsBytes());
                }
            } catch (SocketTimeoutException ex) {
                Logger.getLogger(FAEscapePlanUI.class.getName()).log(Level.SEVERE, null, ex);
                updateTextLog("Connection timed out");
            } catch (IOException ex) {
                Logger.getLogger(FAEscapePlanUI.class.getName()).log(Level.SEVERE, null, ex);
                updateTextLog("An IO Exception occurred");
            }
            
        }
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        saveLocChooser = new javax.swing.JFileChooser();
        iconPanel = new javax.swing.JPanel();
        iconDisplay = new javax.swing.JLabel();
        loginButton = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JSeparator();
        statusBar = new javax.swing.JPanel();
        statusText = new javax.swing.JLabel();
        loginUser = new javax.swing.JTextField();
        loginPass = new javax.swing.JPasswordField();
        loginUserTitle = new javax.swing.JLabel();
        loginPassTitle = new javax.swing.JLabel();
        settingsPanel = new javax.swing.JPanel();
        userTitle = new javax.swing.JLabel();
        galleryTitle = new javax.swing.JLabel();
        scrapsTitle = new javax.swing.JLabel();
        favsTitle = new javax.swing.JLabel();
        galleryCount = new javax.swing.JLabel();
        scrapsCount = new javax.swing.JLabel();
        favsCount = new javax.swing.JLabel();
        galleryAction = new javax.swing.JComboBox<>();
        scrapsAction = new javax.swing.JComboBox<>();
        favsAction = new javax.swing.JComboBox<>();
        jSeparator2 = new javax.swing.JSeparator();
        journalsTitle = new javax.swing.JLabel();
        notesTitle = new javax.swing.JLabel();
        journalsCount = new javax.swing.JLabel();
        notesCount = new javax.swing.JLabel();
        journalsAction = new javax.swing.JComboBox<>();
        notesAction = new javax.swing.JComboBox<>();
        jSeparator3 = new javax.swing.JSeparator();
        saveLocTitle = new javax.swing.JLabel();
        saveLocText = new javax.swing.JTextField();
        saveLocButton = new javax.swing.JButton();
        backupProgress = new javax.swing.JProgressBar();
        backupButton = new javax.swing.JButton();
        refreshButton = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        logTextBox = new javax.swing.JTextArea();
        menuBar = new javax.swing.JMenuBar();
        menuFile = new javax.swing.JMenu();
        menuExit = new javax.swing.JMenuItem();
        menuHelp = new javax.swing.JMenu();
        menuAbout = new javax.swing.JMenuItem();

        saveLocChooser.setAcceptAllFileFilterUsed(false);
        saveLocChooser.setDialogType(javax.swing.JFileChooser.CUSTOM_DIALOG);
        saveLocChooser.setDialogTitle("Select backup folder location");
        saveLocChooser.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("FAEscapePlan");
        setBackground(new java.awt.Color(255, 255, 255));
        setResizable(false);
        setSize(new java.awt.Dimension(751, 418));

        iconPanel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        iconDisplay.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/FAtesticon.png"))); // NOI18N

        javax.swing.GroupLayout iconPanelLayout = new javax.swing.GroupLayout(iconPanel);
        iconPanel.setLayout(iconPanelLayout);
        iconPanelLayout.setHorizontalGroup(
            iconPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(iconDisplay)
        );
        iconPanelLayout.setVerticalGroup(
            iconPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(iconDisplay)
        );

        loginButton.setText("Log In");
        loginButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                loginButtonMouseClicked(evt);
            }
        });

        jSeparator1.setOrientation(javax.swing.SwingConstants.VERTICAL);
        jSeparator1.setPreferredSize(new java.awt.Dimension(10, 50));

        statusBar.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        statusText.setText("Not logged in");

        javax.swing.GroupLayout statusBarLayout = new javax.swing.GroupLayout(statusBar);
        statusBar.setLayout(statusBarLayout);
        statusBarLayout.setHorizontalGroup(
            statusBarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(statusText, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        statusBarLayout.setVerticalGroup(
            statusBarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, statusBarLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(statusText))
        );

        loginUserTitle.setText("Username");

        loginPassTitle.setText("Password");

        userTitle.setFont(new java.awt.Font("Arial", 1, 18)); // NOI18N
        userTitle.setText("Username");

        galleryTitle.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        galleryTitle.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        galleryTitle.setText("Gallery:");
        galleryTitle.setMaximumSize(new java.awt.Dimension(60, 22));
        galleryTitle.setMinimumSize(new java.awt.Dimension(60, 22));
        galleryTitle.setPreferredSize(new java.awt.Dimension(60, 22));

        scrapsTitle.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        scrapsTitle.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        scrapsTitle.setText("Scraps:");

        favsTitle.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        favsTitle.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        favsTitle.setText("Favs:");

        galleryCount.setFont(new java.awt.Font("Arial", 0, 18)); // NOI18N
        galleryCount.setText("0");

        scrapsCount.setFont(new java.awt.Font("Arial", 0, 18)); // NOI18N
        scrapsCount.setText("0");

        favsCount.setFont(new java.awt.Font("Arial", 0, 18)); // NOI18N
        favsCount.setText("0");

        galleryAction.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        galleryAction.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "New Backup", "Update backup", "No action" }));
        galleryAction.setEnabled(false);

        scrapsAction.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        scrapsAction.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "New Backup", "Update backup", "No action" }));
        scrapsAction.setEnabled(false);

        favsAction.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        favsAction.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "New Backup", "Update backup", "No action" }));
        favsAction.setEnabled(false);

        jSeparator2.setOrientation(javax.swing.SwingConstants.VERTICAL);

        journalsTitle.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        journalsTitle.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        journalsTitle.setText("Journals:");

        notesTitle.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        notesTitle.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        notesTitle.setText("Notes:");

        journalsCount.setFont(new java.awt.Font("Arial", 0, 18)); // NOI18N
        journalsCount.setText("0");

        notesCount.setFont(new java.awt.Font("Arial", 0, 18)); // NOI18N
        notesCount.setText("0");

        journalsAction.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        journalsAction.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "New Backup", "Update backup", "No action" }));
        journalsAction.setEnabled(false);

        notesAction.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        notesAction.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "New Backup", "Update backup", "No action" }));
        notesAction.setEnabled(false);

        saveLocTitle.setText("Backup folder:");

        saveLocText.setText("C:\\");

            saveLocButton.setText("...");
            saveLocButton.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent evt) {
                    saveLocButtonMouseClicked(evt);
                }
            });

            backupButton.setFont(new java.awt.Font("Arial", 0, 18)); // NOI18N
            backupButton.setText("START");
            backupButton.setEnabled(false);
            backupButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    backupButtonActionPerformed(evt);
                }
            });

            refreshButton.setText("Refresh");
            refreshButton.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent evt) {
                    refreshButtonMouseClicked(evt);
                }
            });

            javax.swing.GroupLayout settingsPanelLayout = new javax.swing.GroupLayout(settingsPanel);
            settingsPanel.setLayout(settingsPanelLayout);
            settingsPanelLayout.setHorizontalGroup(
                settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, settingsPanelLayout.createSequentialGroup()
                    .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addGroup(settingsPanelLayout.createSequentialGroup()
                            .addComponent(saveLocTitle)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                            .addComponent(saveLocText)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(saveLocButton, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addComponent(jSeparator3, javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(settingsPanelLayout.createSequentialGroup()
                            .addComponent(userTitle, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(refreshButton))
                        .addGroup(settingsPanelLayout.createSequentialGroup()
                            .addGap(0, 0, Short.MAX_VALUE)
                            .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addGroup(settingsPanelLayout.createSequentialGroup()
                                    .addComponent(backupProgress, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(backupButton, javax.swing.GroupLayout.PREFERRED_SIZE, 104, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(settingsPanelLayout.createSequentialGroup()
                                    .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                        .addComponent(favsTitle, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(galleryTitle, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(scrapsTitle, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                    .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                        .addComponent(scrapsCount, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 60, Short.MAX_VALUE)
                                        .addComponent(galleryCount, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(favsCount, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                    .addGap(18, 18, 18)
                                    .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(galleryAction, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(scrapsAction, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(favsAction, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(journalsTitle, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(notesTitle, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                    .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(settingsPanelLayout.createSequentialGroup()
                                            .addComponent(notesCount, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addGap(18, 18, 18)
                                            .addComponent(notesAction, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(settingsPanelLayout.createSequentialGroup()
                                            .addComponent(journalsCount, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addGap(18, 18, 18)
                                            .addComponent(journalsAction, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))))
                    .addGap(40, 40, 40))
            );
            settingsPanelLayout.setVerticalGroup(
                settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(settingsPanelLayout.createSequentialGroup()
                    .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(userTitle)
                        .addComponent(refreshButton))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                    .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addGroup(settingsPanelLayout.createSequentialGroup()
                            .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(galleryAction, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(galleryTitle, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(galleryCount)
                                    .addComponent(journalsTitle)
                                    .addComponent(journalsCount)
                                    .addComponent(journalsAction, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(scrapsTitle)
                                .addComponent(scrapsCount)
                                .addComponent(scrapsAction, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(notesTitle)
                                .addComponent(notesCount)
                                .addComponent(notesAction, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(favsTitle)
                                .addComponent(favsCount)
                                .addComponent(favsAction, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addComponent(jSeparator2))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                    .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(saveLocTitle)
                        .addComponent(saveLocText, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(saveLocButton))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 11, Short.MAX_VALUE)
                    .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(backupButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, settingsPanelLayout.createSequentialGroup()
                            .addComponent(backupProgress, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGap(8, 8, 8)))
                    .addContainerGap())
            );

            logTextBox.setEditable(false);
            logTextBox.setColumns(20);
            logTextBox.setRows(5);
            jScrollPane1.setViewportView(logTextBox);

            menuFile.setText("File");

            menuExit.setText("Exit");
            menuExit.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    menuExitActionPerformed(evt);
                }
            });
            menuFile.add(menuExit);

            menuBar.add(menuFile);

            menuHelp.setText("Help");

            menuAbout.setText("About...");
            menuHelp.add(menuAbout);

            menuBar.add(menuHelp);

            setJMenuBar(menuBar);

            javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
            getContentPane().setLayout(layout);
            layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(jScrollPane1)
                            .addContainerGap())
                        .addGroup(layout.createSequentialGroup()
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(loginUserTitle)
                                .addComponent(iconPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(loginUser)
                                .addComponent(loginButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(loginPassTitle)
                                .addComponent(loginPass))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                            .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(settingsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 570, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGap(0, 0, Short.MAX_VALUE))))
                .addComponent(statusBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            );
            layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGap(6, 6, 6)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(settingsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(iconPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                            .addComponent(loginUserTitle)
                            .addGap(0, 0, 0)
                            .addComponent(loginUser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(loginPassTitle)
                            .addGap(0, 0, 0)
                            .addComponent(loginPass, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(loginButton))
                        .addComponent(jSeparator1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 126, Short.MAX_VALUE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(statusBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            );

            pack();
        }// </editor-fold>//GEN-END:initComponents

    private void loginButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_loginButtonMouseClicked
        if (!userData.getLoginState()) {
            if (this.loginUser.getText().trim().isEmpty() || String.valueOf(this.loginPass.getPassword()).trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Fields cannot be empty.");
            } else {
                try {
                    updateTextLog("Logging in...");
                    this.loginButton.setEnabled(false);
                    this.loginUser.setEditable(false);
                    this.loginPass.setEditable(false);

                    Map<String, String> sessionCookies = Jsoup.connect("http://www.furaffinity.net")
                            .userAgent(USER_AGENT)
                            .execute()
                            .cookies();

                    Response loginResponse = Jsoup.connect("https://www.furaffinity.net/login/")
                            .userAgent(USER_AGENT)
                            .data("action", "login")
                            .data("name", this.loginUser.getText())
                            .data("pass", String.valueOf(this.loginPass.getPassword()))
                            .data("login", "Login+to FurAffinity")
                            .cookies(sessionCookies)
                            .referrer("http://www.furaffinity.net/login/")
                            .timeout(10000)
                            .method(Connection.Method.POST)
                            .execute();
                    Document doc = loginResponse.parse();

                    if (!doc.getElementById("my-username").text().equalsIgnoreCase("~" + this.loginUser.getText())) {
                        JOptionPane.showMessageDialog(this, "Login failed, check your username and password");
                    } else {
                        userData.setName(this.loginUser.getText());
                        sessionCookies.putAll(loginResponse.cookies());
                        userData.setCookies(sessionCookies);                
                        this.loginPass.setText("000000000000");
                        userData.setLoginState(true);
                        getProfileImg();
                        unlockForm();
                        updateTextLog("Successfully logged in");
                    }
                } catch (IOException ex) {
                    Logger.getLogger(FAEscapePlanUI.class.getName()).log(Level.SEVERE, null, ex);
                    System.out.println("Could not connect to FA");
                    this.loginUser.setEditable(true);
                    this.loginPass.setEditable(true);
                } finally {
                    this.loginButton.setEnabled(true);
                }
            }
        } else {
            loginDisconnect();
            resetForm();
        }
    }//GEN-LAST:event_loginButtonMouseClicked

    private void menuExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuExitActionPerformed
        System.exit(0);
    }//GEN-LAST:event_menuExitActionPerformed

    private void saveLocButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_saveLocButtonMouseClicked
        int returnVal = saveLocChooser.showDialog(this, "Select");
        
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File folder = saveLocChooser.getSelectedFile();
            this.saveLocText.setText(folder.getAbsolutePath());
        }
    }//GEN-LAST:event_saveLocButtonMouseClicked

    private void backupButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backupButtonActionPerformed
        if (!Files.isDirectory(Paths.get(this.saveLocText.getText()))) {
            JOptionPane.showMessageDialog(this, "Save location doesn't exist.");
        } else {
            try {
                String homePath = this.saveLocText.getText() + "\\" + userData.getName();
                createDirectory(homePath);
                
                switch (this.galleryAction.getSelectedIndex()) {
                    case 0:
                        FileUtils.deleteDirectory(new File(homePath + "\\gallery"));
                    case 1:
                        String galleryPath = homePath + "\\gallery";
                        createDirectory(galleryPath);
                        ArrayList<String> galleryIndex = indexSection("gallery");
                        downloadImageList(galleryIndex, galleryPath);
                        break;
                    default:
                        break;
                }
                switch (this.scrapsAction.getSelectedIndex()) {
                    case 0:
                        FileUtils.deleteDirectory(new File(homePath + "\\scraps"));
                    case 1:
                        String scrapsPath = homePath + "\\scraps";
                        createDirectory(scrapsPath);
                        ArrayList<String> scrapsIndex = indexSection("scraps");
                        downloadImageList(scrapsIndex, scrapsPath);
                        break;
                    default:
                        break;
                }
                
                //index favs
                switch (this.journalsAction.getSelectedIndex()) {
                    case 0:
                        FileUtils.deleteDirectory(new File(homePath + "\\journals"));
                    case 1:
                        String journalsPath = homePath + "\\journals";
                        createDirectory(journalsPath);
                        ArrayList<String> journalsIndex = indexJournals();
                        //download journals
                        break;
                    default:
                        break;
                }
                //index notes
                
            } catch (IOException ex) {
                Logger.getLogger(FAEscapePlanUI.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_backupButtonActionPerformed

    private void refreshButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_refreshButtonMouseClicked
        ArrayList<String> galleryIndex = indexSection("gallery");
        this.galleryCount.setText(String.valueOf(galleryIndex.size()));
        ArrayList<String> scrapsIndex = indexSection("scraps");
        this.scrapsCount.setText(String.valueOf(scrapsIndex.size()));
        //favs
        ArrayList<String> journalsIndex = indexJournals();
        this.journalsCount.setText(String.valueOf(journalsIndex.size()));
    }//GEN-LAST:event_refreshButtonMouseClicked

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Windows".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(FAEscapePlanUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(FAEscapePlanUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(FAEscapePlanUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(FAEscapePlanUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new FAEscapePlanUI().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton backupButton;
    private javax.swing.JProgressBar backupProgress;
    private javax.swing.JComboBox<String> favsAction;
    private javax.swing.JLabel favsCount;
    private javax.swing.JLabel favsTitle;
    private javax.swing.JComboBox<String> galleryAction;
    private javax.swing.JLabel galleryCount;
    private javax.swing.JLabel galleryTitle;
    private javax.swing.JLabel iconDisplay;
    private javax.swing.JPanel iconPanel;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JComboBox<String> journalsAction;
    private javax.swing.JLabel journalsCount;
    private javax.swing.JLabel journalsTitle;
    private javax.swing.JTextArea logTextBox;
    private javax.swing.JButton loginButton;
    private javax.swing.JPasswordField loginPass;
    private javax.swing.JLabel loginPassTitle;
    private javax.swing.JTextField loginUser;
    private javax.swing.JLabel loginUserTitle;
    private javax.swing.JMenuItem menuAbout;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenuItem menuExit;
    private javax.swing.JMenu menuFile;
    private javax.swing.JMenu menuHelp;
    private javax.swing.JComboBox<String> notesAction;
    private javax.swing.JLabel notesCount;
    private javax.swing.JLabel notesTitle;
    private javax.swing.JButton refreshButton;
    private javax.swing.JButton saveLocButton;
    private javax.swing.JFileChooser saveLocChooser;
    private javax.swing.JTextField saveLocText;
    private javax.swing.JLabel saveLocTitle;
    private javax.swing.JComboBox<String> scrapsAction;
    private javax.swing.JLabel scrapsCount;
    private javax.swing.JLabel scrapsTitle;
    private javax.swing.JPanel settingsPanel;
    private javax.swing.JPanel statusBar;
    private javax.swing.JLabel statusText;
    private javax.swing.JLabel userTitle;
    // End of variables declaration//GEN-END:variables
}
