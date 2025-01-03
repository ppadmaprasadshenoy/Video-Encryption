package in.pp.EncryptionSystem;

import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

// Custom filter for video files
class VideoFileFilter extends FileFilter {
    public boolean accept(File file) {
        String[] supportedFormats = { ".mp4", ".avi", ".mkv", ".mov", ".webm", ".hevc" };
        for (String format : supportedFormats) {
            if (file.getName().endsWith(format)) {
                return true;
            }
        }
        return file.isDirectory();
    }

    public String getDescription() {
        return "*.mp4, *.avi, *.mkv, *.mov, *.webm, *.hevc";
    }
}

public class Cryptography extends JFrame implements ActionListener {
    private JButton browse, enc, denc, cancel;
    private JTextField filename;
    private JFileChooser jfc;
    private File file;

    public Cryptography() {
        super("Video Encryption & Decryption");
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(d.width / 2, d.height / 2);
        setLocation(d.width / 4, d.height / 4);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        // Main panel with background color
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(45, 52, 54));

        // Title label
        JLabel title = new JLabel("Video Encryption & Decryption", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 24));
        title.setForeground(Color.WHITE);

        // Center panel for inputs and buttons
        JPanel centerPanel = new JPanel(new GridLayout(5, 1, 10, 10));
        centerPanel.setBackground(new Color(45, 52, 54));

        // File input field
        filename = new JTextField(30);
        filename.setEditable(false);
        filename.setFont(new Font("Arial", Font.PLAIN, 16));
        filename.setForeground(Color.BLACK);

        // Buttons
        browse = createStyledButton("Browse", new Color(0, 184, 148));
        enc = createStyledButton("Encrypt", new Color(9, 132, 227));
        denc = createStyledButton("Decrypt", new Color(232, 67, 147));
        cancel = createStyledButton("Cancel", new Color(214, 48, 49));

        enc.setEnabled(false);
        denc.setEnabled(false);

        // File chooser
        jfc = new JFileChooser();
        jfc.setFileFilter(new VideoFileFilter());
        jfc.setCurrentDirectory(new File("C:\\Users\\ASUS\\Desktop\\RMTC Project\\Video-Encryption\\videos")); // default

        // Add components to center panel
        centerPanel.add(createStyledLabel("Select a video file for Encryption/Decryption:"));
        centerPanel.add(filename);
        centerPanel.add(browse);
        centerPanel.add(enc);
        centerPanel.add(denc);
        centerPanel.add(cancel);

        // Add components to main panel
        mainPanel.add(title, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        add(mainPanel);
        setVisible(true);

        // Add action listeners
        browse.addActionListener(this);
        enc.addActionListener(this);
        denc.addActionListener(this);
        cancel.addActionListener(this);
    }

    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 16));
        button.setForeground(Color.WHITE);
        button.setBackground(color);
        button.setFocusPainted(false);
        button.setBorderPainted(false);

        // Adding hover effect
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(button.getBackground().darker());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(color);
            }
        });

        return button;
    }

    private JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.LEFT);
        label.setFont(new Font("Arial", Font.PLAIN, 16));
        label.setForeground(Color.WHITE);
        return label;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == browse) {
            int result = jfc.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                file = jfc.getSelectedFile();
                filename.setText(file.getAbsolutePath());
                enc.setEnabled(true);
                denc.setEnabled(true);
            }
        } else if (e.getSource() == enc || e.getSource() == denc) {
            try {
                String password = JOptionPane.showInputDialog(this, "Enter a password for AES key derivation:");
                if (password == null || password.isEmpty()) {
                    throw new IllegalArgumentException("Password cannot be null or empty.");
                }

                int validLength = password.length() <= 16 ? 16
                        : password.length() <= 24 ? 24 : password.length() <= 32 ? 32 : -1;
                if (validLength == -1) {
                    throw new IllegalArgumentException(
                            "Password length exceeds the maximum supported length of 32 characters.");
                }

                password = String.format("%-" + validLength + "s", password).substring(0, validLength);

                boolean isEncryption = (e.getSource() == enc);
                processFile(password, isEncryption);

                JOptionPane.showMessageDialog(this,
                        (isEncryption ? "Encryption" : "Decryption") + " completed successfully!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else if (e.getSource() == cancel) {
            System.exit(0);
        }
    }

    private static SecretKeySpec deriveKey(String password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 10000, 256);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] key = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(key, "AES");
    }

    private void processFile(String password, boolean isEncryption) throws Exception {
        // Prepare output directories
        String outputDir = isEncryption ? "encrypted" : "decrypted";
        Files.createDirectories(Paths.get(outputDir));

        // Output file
        String outputFileName = outputDir + File.separator + (isEncryption ? "encrypted_" : "decrypted_")
                + file.getName();

        byte[] salt = new byte[16];
        if (isEncryption) {
            // Generate a new random salt
            SecureRandom random = new SecureRandom();
            random.nextBytes(salt);
        } else {
            // Read the salt from the encrypted file
            try (FileChannel inputChannel = new FileInputStream(file).getChannel()) {
                ByteBuffer saltBuffer = ByteBuffer.allocate(salt.length);
                inputChannel.read(saltBuffer);
                saltBuffer.flip();
                salt = saltBuffer.array();
            }
        }

        SecretKeySpec secretKey = deriveKey(password, salt); // Derive key using the salt

        // Initialize the Cipher
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(isEncryption ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, secretKey);

        try (FileChannel inputChannel = new FileInputStream(file).getChannel();
                FileChannel outputChannel = FileChannel.open(Paths.get(outputFileName), StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE)) {

            if (isEncryption) {
                // Write the salt at the beginning of the file
                outputChannel.write(ByteBuffer.wrap(salt));
            } else {
                // Skip the salt in the encrypted file
                inputChannel.position(salt.length);
            }

            long fileSize = file.length() - (isEncryption ? 0 : salt.length);

            // Get available memory and adjust buffer size dynamically
            long availableMemory = Runtime.getRuntime().freeMemory();
            int maxBufferSize = 10 * 1024 * 1024; // Maximum buffer size (10 MB)
            int bufferSize = (int) Math.min(Math.min(fileSize, availableMemory / 10), maxBufferSize);

            ByteBuffer buffer = ByteBuffer.allocate(bufferSize);

            while (inputChannel.read(buffer) > 0) {
                buffer.flip();
                byte[] outputBytes = cipher.update(buffer.array(), 0, buffer.limit());
                outputChannel.write(ByteBuffer.wrap(outputBytes));
                buffer.clear();
            }

            byte[] finalBytes = cipher.doFinal();
            outputChannel.write(ByteBuffer.wrap(finalBytes));
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Cryptography::new);
    }
}
