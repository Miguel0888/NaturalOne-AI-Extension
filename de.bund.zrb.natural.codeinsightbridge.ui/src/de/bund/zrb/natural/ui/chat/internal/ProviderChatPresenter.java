package de.bund.zrb.natural.ui.chat.internal;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;

import de.bund.zrb.natural.ui.chat.internal.ollama.OllamaChatClient;

/**
 * Presenter that can route requests to multiple providers.
 *
 * v1: "custom" (dummy) and "ollama" (local HTTP).
 */
public final class ProviderChatPresenter implements ChatPresenter {

    private final ChatViewPort view;
    @SuppressWarnings("unused")
    private final MiniMarkdownParser markdown;
    private final OllamaChatClient ollama;

    private final List<String> userMessages;
    private final AtomicLong generationId;

    private String selectedProviderId;

    public ProviderChatPresenter(ChatViewPort view, MiniMarkdownParser markdown, OllamaChatClient ollama) {
        this.view = view;
        this.markdown = markdown;
        this.ollama = ollama;
        this.userMessages = new ArrayList<String>();
        this.generationId = new AtomicLong(0L);
        this.selectedProviderId = "custom";
    }

    @Override
    public void onViewVisible() {
        // no-op
    }

    @Override
    public void onSendUserMessage(String text) {
        if (text == null) {
            return;
        }
        final String normalized = text.trim();
        if (normalized.isEmpty()) {
            return;
        }

        userMessages.add(normalized);

        String userId = newId();
        view.appendMessage(userId, "user");
        view.setMessageHtml(userId, normalized);
        view.clearUserInput();

        final String assistantId = newId();
        view.appendMessage(assistantId, "assistant");
        view.setMessageHtml(assistantId, "_Thinking..._");

        final long myGen = generationId.incrementAndGet();

        Job job = new Job("AI Provider Request") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    String answer = generateAnswer(normalized);
                    if (generationId.get() != myGen) {
                        return Status.CANCEL_STATUS;
                    }
                    updateAssistant(assistantId, answer);
                } catch (final Exception ex) {
                    if (generationId.get() != myGen) {
                        return Status.CANCEL_STATUS;
                    }
                    updateAssistant(assistantId, "**Error:** " + safeMessage(ex));
                }
                return Status.OK_STATUS;
            }
        };
        job.setSystem(true);
        job.schedule();
    }

    @Override
    public void onReplayLastMessage() {
        if (userMessages.isEmpty()) {
            view.showNotification("Nothing to regenerate yet.", Duration.ofSeconds(3), ChatViewPort.NotificationType.INFO);
            return;
        }
        onSendUserMessage(userMessages.get(userMessages.size() - 1));
    }

    @Override
    public void onClear() {
        generationId.incrementAndGet();
        userMessages.clear();
        view.clearChatView();
        view.showNotification("New chat started.", Duration.ofSeconds(2), ChatViewPort.NotificationType.INFO);
    }

    @Override
    public void onStop() {
        generationId.incrementAndGet();
        try {
            ollama.cancelActiveRequest();
        } catch (Exception ignore) {
            // ignore
        }
        view.showNotification("Stopped.", Duration.ofSeconds(2), ChatViewPort.NotificationType.WARNING);
    }

    @Override
    public void onChatModelSelected(String modelId) {
        if (modelId == null || modelId.trim().isEmpty()) {
            return;
        }
        this.selectedProviderId = modelId.trim();
        view.showNotification("Provider selected: " + this.selectedProviderId, Duration.ofSeconds(2), ChatViewPort.NotificationType.INFO);
    }

    @Override
    public void onSessionChanged(String sessionId) {
        generationId.incrementAndGet();
        userMessages.clear();
    }

    @Override
    public Object snapshotSessionState() {
        return new ArrayList<String>(userMessages);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void restoreSessionState(Object state) {
        generationId.incrementAndGet();
        userMessages.clear();
        if (state instanceof List) {
            for (Object o : ((List<?>) state)) {
                if (o instanceof String) {
                    userMessages.add((String) o);
                }
            }
        }
    }

    @Override
    public void onCopyCode(String code) {
        view.showNotification("Code copied to clipboard.", Duration.ofSeconds(2), ChatViewPort.NotificationType.INFO);
    }

    @Override
    public void onInsertCode(String code) {
        view.showNotification("Insert is not implemented yet.", Duration.ofSeconds(3), ChatViewPort.NotificationType.INFO);
    }

    @Override
    public void onDiffCode(String code) {
        view.showNotification("Diff is not implemented yet.", Duration.ofSeconds(3), ChatViewPort.NotificationType.INFO);
    }

    @Override
    public void onApplyPatch(String code) {
        view.showNotification("Apply Patch is not implemented yet.", Duration.ofSeconds(3), ChatViewPort.NotificationType.INFO);
    }

    @Override
    public void onNewFile(String code, String lang) {
        view.showNotification("New File is not implemented yet.", Duration.ofSeconds(3), ChatViewPort.NotificationType.INFO);
    }

    @Override
    public void onRemoveMessage(String messageId) {
        if (messageId == null || messageId.trim().isEmpty()) {
            return;
        }
        view.removeMessage(messageId);
    }

    private String generateAnswer(String userText) throws Exception {
        if ("ollama".equalsIgnoreCase(selectedProviderId)) {
            String system = "You are a helpful coding assistant. Respond in Markdown.";
            return ollama.chat(system, userText);
        }
        return buildDummyResponse(userText);
    }

    private void updateAssistant(final String assistantId, final String markdownText) {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                view.setMessageHtml(assistantId, markdownText);
            }
        });
    }

    private String buildDummyResponse(String userText) {
        StringBuilder sb = new StringBuilder();
        sb.append("**Dummy-Assistant** (Provider: ").append(selectedProviderId).append(")\n\n");
        sb.append("You wrote:\n\n");
        sb.append("`" + userText + "`\n\n");
        sb.append("Select **Ollama** in the provider dropdown to use the local model.\n");
        return sb.toString();
    }

    private static String safeMessage(Throwable t) {
        if (t == null) {
            return "";
        }
        String m = t.getMessage();
        if (m != null && !m.trim().isEmpty()) {
            return m;
        }
        return t.getClass().getSimpleName();
    }

    private static String newId() {
        return UUID.randomUUID().toString();
    }
}

