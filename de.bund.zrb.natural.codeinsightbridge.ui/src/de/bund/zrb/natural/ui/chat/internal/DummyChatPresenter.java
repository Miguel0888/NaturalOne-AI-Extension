package de.bund.zrb.natural.ui.chat.internal;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.swt.widgets.Display;

/**
 * Provide a small demo conversation without any external integration.
 */
public final class DummyChatPresenter implements ChatPresenter {

    private final ChatViewPort view;
    private final MiniMarkdownParser markdown;

    private final List<String> userMessages;
    private final AtomicLong generationId;

    private String selectedModelId;

    public DummyChatPresenter(ChatViewPort view, MiniMarkdownParser markdown) {
        this.view = view;
        this.markdown = markdown;
        this.userMessages = new ArrayList<String>();
        this.generationId = new AtomicLong(0L);
        this.selectedModelId = "assistai";
    }

    @Override
    public void onViewVisible() {
        // Do nothing
    }

    @Override
    public void onSendUserMessage(String text) {
        if (text == null) {
            return;
        }
        String normalized = text.trim();
        if (normalized.isEmpty()) {
            return;
        }

        userMessages.add(normalized);

        String userId = newId();
        view.appendMessage(userId, "user");
        view.setMessageHtml(userId, normalized);
        view.clearUserInput();

        String assistantId = newId();
        view.appendMessage(assistantId, "assistant");
        streamDummyAnswer(assistantId, normalized);
    }

    @Override
    public void onReplayLastMessage() {
        if (userMessages.isEmpty()) {
            view.showNotification("Nothing to regenerate yet.", Duration.ofSeconds(3), ChatViewPort.NotificationType.INFO);
            return;
        }
        String last = userMessages.get(userMessages.size() - 1);

        String assistantId = newId();
        view.appendMessage(assistantId, "assistant");
        streamDummyAnswer(assistantId, last);
    }

    @Override
    public void onClear() {
        generationId.incrementAndGet();
        userMessages.clear();
        view.clearChatView();
        view.showNotification("Conversation cleared.", Duration.ofSeconds(2), ChatViewPort.NotificationType.INFO);
    }

    @Override
    public void onStop() {
        generationId.incrementAndGet();
        view.showNotification("Stopped.", Duration.ofSeconds(2), ChatViewPort.NotificationType.WARNING);
    }

    @Override
    public void onChatModelSelected(String modelId) {
        if (modelId == null || modelId.trim().isEmpty()) {
            return;
        }
        this.selectedModelId = modelId.trim();
        view.showNotification("Model selected: " + this.selectedModelId, Duration.ofSeconds(2), ChatViewPort.NotificationType.INFO);
    }

    @Override
    public void onCopyCode(String code) {
        view.showNotification("Code copied to clipboard.", Duration.ofSeconds(2), ChatViewPort.NotificationType.INFO);
    }

    @Override
    public void onInsertCode(String code) {
        view.showNotification("Insert is a dummy action in this plugin.", Duration.ofSeconds(3), ChatViewPort.NotificationType.INFO);
    }

    @Override
    public void onDiffCode(String code) {
        view.showNotification("Diff is a dummy action in this plugin.", Duration.ofSeconds(3), ChatViewPort.NotificationType.INFO);
    }

    @Override
    public void onApplyPatch(String code) {
        view.showNotification("Apply Patch is a dummy action in this plugin.", Duration.ofSeconds(3), ChatViewPort.NotificationType.INFO);
    }

    @Override
    public void onNewFile(String code, String lang) {
        view.showNotification("New File is a dummy action in this plugin.", Duration.ofSeconds(3), ChatViewPort.NotificationType.INFO);
    }

    @Override
    public void onRemoveMessage(String messageId) {
        if (messageId == null || messageId.trim().isEmpty()) {
            return;
        }
        view.removeMessage(messageId);
    }

    private void streamDummyAnswer(final String assistantId, final String userText) {
        final long myGenerationId = generationId.incrementAndGet();

        final String full = buildDummyResponse(userText);
        final int[] index = new int[] { 0 };

        Runnable tick = new Runnable() {
            @Override
            public void run() {
                if (generationId.get() != myGenerationId) {
                    return;
                }
                int next = Math.min(full.length(), index[0] + 20);
                String partial = full.substring(0, next);
                index[0] = next;
                view.setMessageHtml(assistantId, partial);

                if (index[0] < full.length()) {
                    Display.getDefault().timerExec(40, this);
                }
            }
        };

        Display.getDefault().timerExec(40, tick);
    }

    private String buildDummyResponse(String userText) {
        StringBuilder sb = new StringBuilder();
        sb.append("**Dummy-Assistant** (Model: ").append(selectedModelId).append(")\n\n");
        sb.append("You wrote:\n\n");
        sb.append("`" + userText + "`\n\n");
        sb.append("Here is a small sample code block (UI demo):\n\n");
        sb.append("```java\n");
        sb.append("// Print a short demo message\n");
        sb.append("System.out.println(\"Hello from the AssistAI-like UI dummy!\");\n");
        sb.append("```\n\n");
        sb.append("Try the toolbar buttons, the model dropdown and the code-block buttons.\n");
        return sb.toString();
    }

    private static String newId() {
        return UUID.randomUUID().toString();
    }
}
