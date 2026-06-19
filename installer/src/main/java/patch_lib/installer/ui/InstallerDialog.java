package patch_lib.installer.ui;

import patch_lib.installer.InstallArgs;
import patch_lib.installer.Reason;
import patch_lib.installer.core.ApplyResult;
import patch_lib.installer.core.WritePermissionProbe;
import patch_lib.installer.elevation.Elevation;
import patch_lib.installer.platform.Platform;
import patch_lib.installer.platform.PlatformFactory;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;
import java.net.URI;

/** The installer window. Simple, no banner, in the style of the NeoForge installer. */
public final class InstallerDialog extends JFrame {

    //Placeholder until the real forum thread exists.
    private static final String FORUM_URL = "https://fractalsoftworks.com/forum/index.php?topic=PLACEHOLDER";

    //Width the text wraps at. Kept fairly narrow so short messages, like the success one, do not look empty.
    private static final int TEXT_WIDTH = 380;

    private static final Color MUTED = new Color(0x77, 0x77, 0x77);

    private final InstallArgs args;
    private final Platform platform;
    private final File launcherFile;
    private final boolean needsAdmin;

    private final JLabel titleLabel = new JLabel();
    private final JTextArea bodyArea = new JTextArea();
    private final JLabel versionsLabel = new JLabel();
    private final JButton installButton = new JButton("Install");
    private final JButton forumButton = new JButton("Open forum thread");
    private final JButton closeButton = new JButton("Close");

    public InstallerDialog(InstallArgs args) {
        super("PatchLib Installer");
        this.args = args;
        this.platform = PlatformFactory.current();
        this.launcherFile = platform.launcherFile(args.workingDir);
        //Silent probe, runs once on open. It never triggers UAC, it only decides whether the
        //install needs to elevate when the button is pressed. Only Windows can elevate; on Mac and
        //Linux the files are user owned, so the install is attempted directly and any failure is shown.
        this.needsAdmin = platform.supportsElevation()
            && WritePermissionProbe.needsElevation(args.workingDir, launcherFile);

        buildUi();
    }

    private void buildUi() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        Font base = labelFont();
        titleLabel.setFont(base.deriveFont(Font.BOLD, base.getSize2D() + 5f));
        titleLabel.setText(title());

        bodyArea.setEditable(false);
        bodyArea.setFocusable(false);
        bodyArea.setOpaque(false);
        bodyArea.setLineWrap(true);
        bodyArea.setWrapStyleWord(true);
        bodyArea.setBorder(null);
        bodyArea.setFont(base.deriveFont(base.getSize2D() + 2f));

        versionsLabel.setFont(base);
        versionsLabel.setForeground(MUTED);
        if (args.reason == Reason.VERSION_MISMATCH) {
            versionsLabel.setText("Mod version: " + safe(args.modVersion)
                + "      Agent version: " + safe(args.agentVersion));
        } else {
            versionsLabel.setVisible(false);
        }

        setBody(introText());

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setBorder(BorderFactory.createEmptyBorder(16, 18, 8, 18));
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);
        bodyArea.setAlignmentX(LEFT_ALIGNMENT);
        versionsLabel.setAlignmentX(LEFT_ALIGNMENT);
        text.add(titleLabel);
        text.add(Box.createVerticalStrut(8));
        text.add(bodyArea);
        text.add(Box.createVerticalStrut(10));
        text.add(versionsLabel);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 8));
        buttons.setBorder(BorderFactory.createEmptyBorder(0, 14, 6, 12));
        buttons.add(installButton);
        buttons.add(forumButton);
        buttons.add(closeButton);

        installButton.addActionListener(e -> runInstall());
        forumButton.addActionListener(e -> openForum());
        closeButton.addActionListener(e -> dispose());

        setLayout(new BorderLayout());
        add(text, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        configurePrimary();
        pack();
        setLocationRelativeTo(null);
    }

    //Highlights the main action subtly by making it the default button (Enter triggers it).
    private void configurePrimary() {
        if (platform.supportsAutoInstall()) {
            getRootPane().setDefaultButton(installButton);
        } else {
            installButton.setEnabled(false);
            getRootPane().setDefaultButton(forumButton);
        }
    }

    private String title() {
        return args.reason == Reason.VERSION_MISMATCH
            ? "PatchLib version mismatch"
            : "PatchLib is not fully installed";
    }

    //One text shown on every platform. It points anyone who would rather not use the button at the
    //manual steps on the forum, without tying the button to any one platform.
    private String introText() {
        return situationText()
            + " Press the Install button below to set it up. Depending on where Starsector is "
            + "installed, this might ask for admin permissions."
            + "\n\nIf you would rather set it up by hand, or you would rather not grant admin "
            + "permissions, PatchLib's forum page has step by step instructions for installing it "
            + "manually.";
    }

    //Describes why the installer opened, without mentioning the button. The button advice is added after.
    private String situationText() {
        if (args.reason == Reason.VERSION_MISMATCH) {
            return "PatchLib's installed agent version does not match the mod. This usually happens right "
                + "after you update the mod.";
        }
        return "PatchLib needs to set a flag in Starsector's launch config. This happens the first time "
            + "you install PatchLib, and again after a Starsector update.";
    }

    private void runInstall() {
        setButtonsEnabled(false);
        versionsLabel.setVisible(false);
        titleLabel.setText("Installing");
        setBody("Working on it...");
        pack();
        File installerJar = SelfLocator.installerJar();

        new SwingWorker<ApplyResult, Void>() {
            @Override
            protected ApplyResult doInBackground() {
                if (needsAdmin) {
                    if (installerJar == null) {
                        return ApplyResult.failure("Could not locate the installer jar to run with admin rights.");
                    }
                    return Elevation.runElevated(args, installerJar);
                }
                return platform.apply(args);
            }

            @Override
            protected void done() {
                ApplyResult result;
                try {
                    result = get();
                } catch (Exception e) {
                    result = ApplyResult.failure("Install failed: " + e.getMessage());
                }
                showResult(result);
            }
        }.execute();
    }

    private void showResult(ApplyResult result) {
        versionsLabel.setVisible(false);
        if (result.success) {
            titleLabel.setText("Installation successful");
            setBody("You can close this window now, then launch Starsector to start playing.");
            installButton.setVisible(false);
        } else if (result.cancelled) {
            titleLabel.setText("Install cancelled");
            setBody("Nothing was changed. Press Install to try again, or follow the manual steps on "
                + "PatchLib's forum page.");
            installButton.setEnabled(true);
        } else {
            titleLabel.setText("Installation failed");
            setBody("PatchLib could not finish the install.\n\n" + result.message
                + "\n\nYou can try again, or install it manually from PatchLib's forum page.");
            installButton.setEnabled(true);
        }
        forumButton.setEnabled(true);
        closeButton.setEnabled(true);
        getRootPane().setDefaultButton(result.success ? closeButton : installButton);
        closeButton.requestFocusInWindow();
        pack();
    }

    private void setButtonsEnabled(boolean enabled) {
        installButton.setEnabled(enabled);
        forumButton.setEnabled(enabled);
        closeButton.setEnabled(enabled);
    }

    //Sets the body text and locks the area to the wrap width so the window stays narrow.
    private void setBody(String content) {
        //Clear the previous hints first, otherwise getPreferredSize returns the old locked height
        //instead of measuring the new text.
        bodyArea.setPreferredSize(null);
        bodyArea.setMaximumSize(null);
        bodyArea.setMinimumSize(null);
        bodyArea.setText(content);
        bodyArea.setSize(new Dimension(TEXT_WIDTH, Short.MAX_VALUE));
        Dimension fixed = new Dimension(TEXT_WIDTH, bodyArea.getPreferredSize().height);
        bodyArea.setPreferredSize(fixed);
        bodyArea.setMaximumSize(fixed);
        bodyArea.setMinimumSize(fixed);
    }

    private void openForum() {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(FORUM_URL));
                return;
            }
        } catch (Exception ignored) {
            //Fall through to showing the link.
        }
        JOptionPane.showMessageDialog(this,
            "Open this link in your browser:\n" + FORUM_URL,
            "Forum thread", JOptionPane.INFORMATION_MESSAGE);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private Font labelFont() {
        Font f = UIManager.getFont("Label.font");
        return f != null ? f : installButton.getFont();
    }
}
