package de.bund.zrb.natural.ui.chat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;

/**
 * Provide a minimal chat UI that can run without any external systems.
 */
public final class ChatView extends ViewPart {

    public static final String ID = "de.bund.zrb.natural.codeinsightbridge.ui.chatView";

    private StyledText conversation;
    private Text input;

    @Override
    public void createPartControl(Composite parent) {
        Composite root = new Composite(parent, SWT.NONE);
        root.setLayout(new GridLayout(1, false));

        createConversationArea(root);
        createInputArea(root);

        appendSystemMessage("Ready. Type a message and press Enter.");
    }

    private void createConversationArea(Composite parent) {
        ScrolledComposite scrolled = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
        scrolled.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        scrolled.setExpandHorizontal(true);
        scrolled.setExpandVertical(true);

        conversation = new StyledText(scrolled, SWT.READ_ONLY | SWT.WRAP | SWT.MULTI);
        conversation.setAlwaysShowScrollBars(false);

        scrolled.setContent(conversation);
        scrolled.setMinSize(conversation.computeSize(SWT.DEFAULT, SWT.DEFAULT));

        conversation.addModifyListener(e -> scrolled.setMinSize(conversation.computeSize(SWT.DEFAULT, SWT.DEFAULT)));
    }

    private void createInputArea(Composite parent) {
        Composite inputBar = new Composite(parent, SWT.NONE);
        inputBar.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false));
        inputBar.setLayout(new GridLayout(2, false));

        input = new Text(inputBar, SWT.BORDER);
        input.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Button send = new Button(inputBar, SWT.PUSH);
        send.setText("Send");
        send.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

        send.addListener(SWT.Selection, e -> sendMessage());
        input.addListener(SWT.DefaultSelection, e -> sendMessage());
    }

    private void sendMessage() {
        String message = input.getText();
        input.setText("");

        if (message == null || message.trim().isEmpty()) {
            return;
        }

        appendUserMessage(message.trim());
        appendAssistantMessage(createPlaceholderAnswer(message.trim()));
    }

    private String createPlaceholderAnswer(String userMessage) {
        return "(dummy) I received: " + userMessage;
    }

    private void appendSystemMessage(String message) {
        appendLine("[system] " + message);
    }

    private void appendUserMessage(String message) {
        appendLine("[you] " + message);
    }

    private void appendAssistantMessage(String message) {
        appendLine("[assistant] " + message);
    }

    private void appendLine(String line) {
        String current = conversation.getText();
        if (current == null || current.isEmpty()) {
            conversation.setText(line);
        } else {
            conversation.setText(current + "\n" + line);
        }
        conversation.setSelection(conversation.getCharCount());
        conversation.showSelection();
    }

    @Override
    public void setFocus() {
        if (input != null && !input.isDisposed()) {
            input.setFocus();
        }
    }
}
