import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class FileDownloaderApp {

    private JFrame frame;
    private JTextField urlField;
    private JTextField savePathField;
    private JProgressBar progressBar;
    private JButton startButton;
    private JButton stopButton;

    private ExecutorService executor;
    private AtomicBoolean isDownloading;

    public FileDownloaderApp() {
        initializeUI();
    }

    private void initializeUI() {
        frame = new JFrame("File Downloader");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 200);
        frame.setLayout(new GridLayout(5, 1));

        urlField = new JTextField();
        savePathField = new JTextField();
        progressBar = new JProgressBar(0, 100);
        startButton = new JButton("Старт");
        stopButton = new JButton("Стоп");
        stopButton.setEnabled(false);

        frame.add(new JLabel("URL файлу:"));
        frame.add(urlField);
        frame.add(new JLabel("Назва збереження:"));
        frame.add(savePathField);
        frame.add(progressBar);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);
        frame.add(buttonPanel);

        executor = Executors.newSingleThreadExecutor();
        isDownloading = new AtomicBoolean(false);

        startButton.addActionListener(e -> startDownload());
        stopButton.addActionListener(e -> stopDownload());

        frame.setVisible(true);
    }

    private void startDownload() {
        String fileUrl = urlField.getText();
        String savePath = savePathField.getText();

        if (fileUrl.isEmpty() || savePath.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Будь ласка, вкажіть URL-адресу та шлях до збереження.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        File file = new File(savePath);
        if (file.exists()) {
            JOptionPane.showMessageDialog(frame, "Файл уже створено.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        isDownloading.set(true);
        startButton.setEnabled(false);
        stopButton.setEnabled(true);

        executor.submit(() -> {
            try {
                downloadFile(fileUrl, savePath);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } finally {
                isDownloading.set(false);
                SwingUtilities.invokeLater(() -> {
                    startButton.setEnabled(true);
                    stopButton.setEnabled(false);
                });
            }
        });
    }

    private void stopDownload() {
        isDownloading.set(false);
    }

    private void downloadFile(String fileUrl, String savePath) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(fileUrl).openConnection();
        int contentLength = connection.getContentLength();
        progressBar.setMaximum(contentLength);

        try (InputStream inputStream = connection.getInputStream();
             FileOutputStream outputStream = new FileOutputStream(savePath)) {

            byte[] buffer = new byte[1024];
            int bytesRead;
            int totalRead = 0;

            while (isDownloading.get() && (bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalRead += bytesRead;

                final int progress = totalRead;
                SwingUtilities.invokeLater(() -> progressBar.setValue(progress));
            }

            if (!isDownloading.get()) {
                new File(savePath).delete();
                throw new IOException("Завантаження зупинено користувачем");
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(FileDownloaderApp::new);
    }
}
