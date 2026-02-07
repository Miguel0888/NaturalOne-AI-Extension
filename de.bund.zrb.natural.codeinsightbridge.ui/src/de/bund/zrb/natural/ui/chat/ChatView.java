package de.bund.zrb.natural.ui.chat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;

/**
 * Provide a minimal chat-like view with transcript + input field.
 */
public final class ChatView extends ViewPart {

    public static final String VIEW_ID = "de.bund.zrb.natural.codeinsightbridge.ui.views.chat";

    private StyledText transcript;
    private Text input;

    @Override
    public void createPartControl(Composite parent) {
        Composite root = new Composite(parent, SWT.NONE);
        root.setLayout(new GridLayout(1, false));

        transcript = new StyledText(root, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY);
        transcript.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Composite inputRow = new Composite(root, SWT.NONE);
        inputRow.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        inputRow.setLayout(new GridLayout(2, false));

        input = new Text(inputRow, SWT.BORDER);
        input.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        input.addListener(SWT.DefaultSelection, event -> send());

        Button sendButton = new Button(inputRow, SWT.PUSH);
        sendButton.setText("Send");
        sendButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        sendButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                send();
            }
        });

        appendLine("System", "Chat view ready.");
    }

    private void send() {
        String text = input.getText();
        if (text == null) {
            return;
        }
        text = text.trim();
        if (text.isEmpty()) {
            return;
        }

        input.setText("");
        appendLine("You", text);

        // Simulate a response. Replace with real integration later.
        final String response = "Echo: " + text;
        Display.getDefault().asyncExec(() -> appendLine("Assistant", response));
    }

    private void appendLine(String speaker, String message) {
        String line = speaker + ": " + message + System.lineSeparator();
        transcript.append(line);
        transcript.setSelection(transcript.getCharCount());
        transcript.showSelection();
    }

    @Override
    public void setFocus() {
        if (input != null && !input.isDisposed()) {
            input.setFocus();
        }
    }
}
