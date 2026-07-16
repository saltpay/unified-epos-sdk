package com.teya.epos.samples.poslink;

import com.teya.sdkutilities.Logger;
import com.teya.unifiedepossdk.PaymentStateSubscription;
import com.teya.unifiedepossdk.TeyaPosLinkSDK;
import com.teya.unifiedepossdk.poslink.PosLinkSDK;
import com.teya.unifiedepossdk.poslink.TeyaPosLinkPaymentInProgressUi;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import kotlin.Unit;

/**
 * Minimal plain Java + Swing host for the Teya PosLink SDK that takes a single card payment,
 * calling the SDK directly in-process (no JSON-RPC bridge).
 *
 * The SDK renders its own login / device-linking / payment windows (Compose), so this app never
 * touches Compose itself — it only calls init() / setup() / makePayment() and reacts to callbacks.
 * The physical Teya terminal is where the card is tapped.
 */
public final class PosLinkJvmSampleApp {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final String CURRENCY_CODE = "GBP";

    private final JTextField clientIdField = new JTextField("", 28);
    private final JPasswordField clientSecretField = new JPasswordField("", 28);
    private final JTextField eposInstanceIdField = new JTextField("java-poslink-sample", 28);
    private final JTextField amountField = new JTextField("1000", 28);
    private final JTextArea logArea = new JTextArea(16, 60);
    private final JButton initButton = new JButton("1. Initialise SDK");
    private final JButton setupButton = new JButton("2. Setup (opens Teya UI)");
    private final JButton payButton = new JButton("3. Make payment");

    private PosLinkSDK sdk;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PosLinkJvmSampleApp().show());
    }

    private void show() {
        JFrame frame = new JFrame("Teya PosLink SDK — JVM payment sample");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(8, 8));

        frame.add(buildForm(), BorderLayout.NORTH);
        frame.add(buildLog(), BorderLayout.CENTER);

        setupButton.setEnabled(false);
        payButton.setEnabled(false);

        initButton.addActionListener(e -> initialiseSdk());
        setupButton.addActionListener(e -> runSetup());
        payButton.addActionListener(e -> makePayment());

        frame.pack();
        frame.setMinimumSize(new Dimension(720, 560));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        log("Enter your Client ID and secret, then press \"1. Initialise SDK\".");
    }

    private JPanel buildForm() {
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12));

        form.add(labelled("Client ID", clientIdField));
        form.add(Box.createVerticalStrut(6));
        form.add(labelled("Client secret", clientSecretField));
        form.add(Box.createVerticalStrut(6));
        form.add(labelled("ePOS instance ID", eposInstanceIdField));
        form.add(Box.createVerticalStrut(6));
        form.add(labelled("Amount (minor units)", amountField));
        form.add(Box.createVerticalStrut(10));

        JPanel buttons = new JPanel();
        buttons.add(initButton);
        buttons.add(setupButton);
        buttons.add(payButton);
        form.add(buttons);

        return form;
    }

    private static JPanel labelled(String text, JTextField field) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        JLabel label = new JLabel(text);
        label.setPreferredSize(new Dimension(150, 24));
        row.add(label, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        return row;
    }

    private JScrollPane buildLog() {
        logArea.setEditable(false);
        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(BorderFactory.createTitledBorder("SDK log"));
        return scroll;
    }

    private void initialiseSdk() {
        String clientId = clientIdField.getText().trim();
        String clientSecret = new String(clientSecretField.getPassword()).trim();
        String eposInstanceId = eposInstanceIdField.getText().trim();

        if (clientId.isEmpty() || clientSecret.isEmpty()) {
            log("Client ID and client secret are both required.");
            return;
        }

        try {
            PosLinkSDK.AuthConfig authConfig = new PosLinkSDK.AuthConfig.Managed(clientId, clientSecret);

            sdk = TeyaPosLinkSDK.init(
                    authConfig,
                    /* isProductionEnv = */ false,
                    eposInstanceId.isEmpty() ? null : eposInstanceId,
                    new SwingLogger()
            );
        } catch (Throwable t) {
            log("Initialisation failed: " + t.getClass().getSimpleName() + ": " + t.getMessage());
            return;
        }

        log("SDK initialised: " + sdk);
        initButton.setEnabled(false);
        setupButton.setEnabled(true);
    }

    private void runSetup() {
        log("Calling setup() — the SDK opens its own window for login and device linking.");
        setupButton.setEnabled(false);

        sdk.setup(
                failure -> {
                    onEdt(() -> {
                        log("Setup failed: " + failure.getReason() + " — " + failure.getDebugErrorMessage());
                        setupButton.setEnabled(true);
                    });
                    return Unit.INSTANCE;
                },
                () -> {
                    onEdt(() -> {
                        log("Setup succeeded. Linked device: " + sdk.getLinkedDeviceDetails());
                        payButton.setEnabled(true);
                    });
                    return Unit.INSTANCE;
                }
        );
    }

    private void makePayment() {
        int amount;
        try {
            amount = Integer.parseInt(amountField.getText().trim());
        } catch (NumberFormatException e) {
            log("Amount must be a whole number of minor units (e.g. 1000 = £10.00).");
            return;
        }
        if (amount <= 0) {
            log("Amount must be greater than zero.");
            return;
        }

        String transactionId = UUID.randomUUID().toString();
        log("Starting payment " + transactionId + " for " + amount + " " + CURRENCY_CODE
                + " minor units. Follow the prompts on the terminal.");
        payButton.setEnabled(false);

        PaymentStateSubscription subscription = sdk.getTransactionsApi().makePayment(
                transactionId,
                amount,
                CURRENCY_CODE,
                /* tip = */ null,
                /* purchaseData = */ null,
                /* tabContext = */ null
        );

        subscription.subscribe(state -> onEdt(() -> {
            log("Payment state = " + state.getState() + (state.isFinal() ? " (final)" : ""));
            if (state.isFinal()) {
                if (state.getReason() != null) {
                    log("Reason: " + state.getReason());
                }
                log("Payment finished. You can start another payment.");
                payButton.setEnabled(true);
            }
        }));

        subscription.subscribe(TeyaPosLinkPaymentInProgressUi.create(
                /* autoDismissOnFinalStateAfterMs = */ 2000L,
                state -> {
                    log("Payment UI dismissed: " + state);
                    return Unit.INSTANCE;
                }));
    }

    private void log(String message) {
        onEdt(() -> {
            logArea.append("[" + LocalTime.now().format(TIME) + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private static void onEdt(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
        } else {
            SwingUtilities.invokeLater(action);
        }
    }

    private final class SwingLogger implements Logger {
        @Override
        public void d(String message) {
            log("D  " + message);
        }

        @Override
        public void i(String message) {
            log("I  " + message);
        }

        @Override
        public void w(String message) {
            log("W  " + message);
        }

        @Override
        public void e(String message) {
            log("E  " + message);
        }
    }
}
