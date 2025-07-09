import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;

public class Uebersetzer extends JFrame {

    private JTextField txtText = new JTextField("text", 20);
    private JComboBox<String> cbSprachen = new JComboBox<>(
            new String[]{"en", "fr", "es", "it", "pt", "ru", "ja", "zh", "pl"}
    );
    private JLabel lblUbersetzt = new JLabel("Ubersetzung ...");
    private JButton btnUbersetze = new JButton("Ubersetze");
    private ExecutorService executor = Executors.newSingleThreadExecutor();


    public Uebersetzer(){
        super("Ubersetzer");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));
        add(new Label("Eure Eingabe"));
        add(txtText);
        add(new Label("Zielsprache"));
        add(cbSprachen);
        add(btnUbersetze);
        add(lblUbersetzt);


        btnUbersetze.addActionListener((e) -> translateAsync());
        setSize(600,150);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void translateAsync(){
        String quelltext = txtText.getText().trim();
        String zielsprache = (String) cbSprachen.getSelectedItem();
        if (quelltext.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Etwas eingeben !!!");
            return;
        }
        fetchTranslation(quelltext, zielsprache)
                .thenAccept(translation -> SwingUtilities.invokeLater(() ->
                        lblUbersetzt.setText(translation)))
                .exceptionally(ex -> {
                            SwingUtilities.invokeLater(() ->
                                    lblUbersetzt.setText(ex.getMessage()));
                            return null;
                        }
                );
    }

    private CompletableFuture<String> fetchTranslation(String quelltext, String zielsprache) {
        return CompletableFuture.supplyAsync(() -> {
                    try {
                        String url = "https://api.mymemory.translated.net/get" + "?q=" + URLEncoder.encode(quelltext, StandardCharsets.UTF_8)
                                + "&langpair=de|" + zielsprache;

                        String json = fetchAPI(url);
                        JSONObject root = new JSONObject();
                        JSONObject responseData = root.getJSONObject("responsData");
                        return responseData.getString("translatedText");

                    } catch (Exception ex){
                        return "Ubersetzung schiefgelaufen";
                    }
                }, executor)
                .orTimeout(10, TimeUnit.SECONDS);
    }


    private String fetchAPI(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), "UTF-8")
        )) {
            StringBuilder sb = new StringBuilder();
            String ziele;
            while ((ziele=reader.readLine()) != null) {
                sb.append(ziele);
            }
            return sb.toString();
        }
    }

    @Override
    public void dispose() {
        executor.shutdownNow();
        super.dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Uebersetzer::new);
    }
}
