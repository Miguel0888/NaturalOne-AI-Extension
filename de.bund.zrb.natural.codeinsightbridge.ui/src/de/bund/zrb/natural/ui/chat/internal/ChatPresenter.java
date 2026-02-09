package de.bund.zrb.natural.ui.chat.internal;

/**
 * Handle UI events coming from the chat view.
 */
public interface ChatPresenter {

    void onViewVisible();

    void onSendUserMessage(String text);

    void onReplayLastMessage();

    void onClear();

    void onStop();

    void onChatModelSelected(String modelId);

    /**
     * Called when the UI switches to another chat session.
     * Presenter implementations can reset session-scoped internal state here.
     */
    void onSessionChanged(String sessionId);

    /**
     * Persist session-scoped presenter state (e.g. last user prompts used for resend).
     */
    Object snapshotSessionState();

    /**
     * Restore a session-scoped presenter state previously captured via {@link #snapshotSessionState()}.
     */
    void restoreSessionState(Object state);

    void onCopyCode(String code);

    void onInsertCode(String code);

    void onDiffCode(String code);

    void onApplyPatch(String code);

    void onNewFile(String code, String lang);

    void onRemoveMessage(String messageId);
}
