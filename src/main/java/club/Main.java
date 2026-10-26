package club;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.*;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class Main {
    public static void main(String[] args) {
        // Configure l'apparence avant de créer les composants Swing.
        FlatLaf.setGlobalExtraDefaults(Map.of("@accentColor", "#4F46E5"));
        FlatLightLaf.setup();
        UIManager.put("Component.arc", 10);
        UIManager.put("Button.arc", 10);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));

        SwingUtilities.invokeLater(() -> {
            try {
                // Vérifie la connexion avant d'afficher l'interface.
                Db.get();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "Connexion Oracle impossible :\n" + e.getMessage()
                                + "\n\nVérifiez que le conteneur Docker est démarré.",
                        "Erreur de connexion", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
            build();
        });
    }

    private static void build() {
        // Construit la fenêtre principale et ses pages, affichées avec un CardLayout.
        JFrame f = new JFrame("Gestion de Club Sportif");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Image logoImage = loadLogo();
        if (logoImage != null) {
            f.setIconImage(logoImage);
            setTaskbarIcon(logoImage);
        }
        f.setMinimumSize(new Dimension(1000, 600));

        String[] names = {"Tableau de bord", "Membres", "Entraîneurs", "Séances", "Clubs", "Salles"};
        Page[] pages = {new DashboardPanel(), new MembresPanel(), new EntraineursPanel(),
                new SeancesPanel(), new ClubsPanel(), new SallesPanel()};

        CardLayout cards = new CardLayout();
        JPanel content = new JPanel(cards);
        content.setBackground(Util.BG);

        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBackground(new Color(0x1E1B4B));
        side.setBorder(new EmptyBorder(24, 16, 24, 16));
        side.setPreferredSize(new Dimension(240, 0));

        JPanel branding = new JPanel(new BorderLayout(10, 0));
        branding.setOpaque(false);
        branding.setAlignmentX(Component.LEFT_ALIGNMENT);
        branding.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        if (logoImage != null) {
            JLabel image = new JLabel(new ImageIcon(logoImage.getScaledInstance(44, 44, Image.SCALE_SMOOTH)));
            branding.add(image, BorderLayout.WEST);
        }
        JLabel logo = new JLabel("Club Sportif");
        logo.setForeground(Color.WHITE);
        logo.putClientProperty("FlatLaf.style", "font:bold +6");
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);
        branding.add(logo, BorderLayout.CENTER);
        JLabel sub = new JLabel("Oracle objet-relationnel");
        sub.setForeground(new Color(0xA5B4FC));
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(branding);
        side.add(sub);
        side.add(Box.createVerticalStrut(28));

        ButtonGroup group = new ButtonGroup();
        for (int i = 0; i < names.length; i++) {
            Page p = pages[i];
            String n = names[i];
            content.add(p, n);
            JToggleButton b = new JToggleButton(n);
            b.setHorizontalAlignment(SwingConstants.LEFT);
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.putClientProperty("FlatLaf.style",
                    "background:#1E1B4B; foreground:#C7D2FE; hoverBackground:#312E81;"
                    + "selectedBackground:#4F46E5; selectedForeground:#FFFFFF;"
                    + "arc:12; borderWidth:0; focusWidth:0; margin:8,14,8,14");
            b.addActionListener(e -> {
                // Recharge les données de la page choisie avant de l'afficher.
                p.refresh();
                cards.show(content, n);
            });
            group.add(b);
            side.add(b);
            side.add(Box.createVerticalStrut(4));
            if (i == 0) b.setSelected(true);
        }
        side.add(Box.createVerticalGlue());
        JLabel db = new JLabel(Db.USER + " @ FREEPDB1");
        db.setForeground(new Color(0x818CF8));
        db.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(db);

        f.setLayout(new BorderLayout());
        f.add(side, BorderLayout.WEST);
        f.add(content, BorderLayout.CENTER);
        pages[0].refresh();
        f.setExtendedState(JFrame.MAXIMIZED_BOTH);
        f.setVisible(true);
    }

    private static Image loadLogo() {
        // Charge le logo depuis les ressources incluses dans le JAR.
        java.net.URL logoUrl = Main.class.getResource("/club_sport.png");
        return logoUrl == null ? null : new ImageIcon(logoUrl).getImage();
    }

    private static void setTaskbarIcon(Image logoImage) {
        // Certaines plateformes distinguent l'icône de la fenêtre de celle du Dock.
        if (!Taskbar.isTaskbarSupported()) {
            return;
        }
        try {
            Taskbar.getTaskbar().setIconImage(logoImage);
        } catch (UnsupportedOperationException | SecurityException ignored) {
            // L'environnement de bureau peut ne pas permettre ce changement.
        }
    }
}
