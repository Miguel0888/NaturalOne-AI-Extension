package de.bund.zrb.natural.ui.chat;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.prefs.BackingStoreException;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.themes.ITheme;
import org.eclipse.ui.themes.IThemeManager;

import de.bund.zrb.natural.ui.chat.internal.BundleResourceReader;
import de.bund.zrb.natural.ui.chat.internal.ChatPresenter;
import de.bund.zrb.natural.ui.chat.internal.ChatViewPort;
import de.bund.zrb.natural.ui.chat.internal.DummyChatPresenter;
import de.bund.zrb.natural.ui.chat.internal.EmbeddedFontCssBuilder;
import de.bund.zrb.natural.ui.chat.internal.FallbackUiResources;
import de.bund.zrb.natural.ui.chat.internal.MiniMarkdownParser;

/**
 * Provide a Copilot-like chat UI (click dummy) without any real backend.
 */
public final class ChatView extends ViewPart implements ChatViewPort {

    public static final String VIEW_ID = "de.bund.zrb.natural.codeinsightbridge.ui.chatView";

    private static final DateTimeFormatter HISTORY_TITLE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String PREF_THEME_MODE = "chat.ui.themeMode";
    private static final String PREF_CHAT_MODE = "chat.ui.mode";
    private static final String PREF_CHAT_PROVIDER_ID = "chat.ui.providerId";
    private static final String DEFAULT_CHAT_MODE = "Ask";
    private static final String DEFAULT_PROVIDER_ID = "custom";

    private enum ThemeMode {
        AUTO,
        LIGHT,
        DARK
    }


    private final BundleResourceReader resourceReader;
    private final EmbeddedFontCssBuilder fontCssBuilder;
    private final MiniMarkdownParser markdown;
    private final ChatPresenter presenter;

    private Browser browser;

    private Text inputArea;

    private Combo modeCombo;
    private Combo providerCombo;

    private Composite root;
    private Composite promptContainer;
    private Composite attachmentsBar;
    private ScrolledComposite attachmentsScroll;
    private Composite attachmentChips;
    private ToolItem settingsItem;
    private ToolItem historyToolItem;
    private ToolItem newChatToolItem;
    private ToolItem resendToolItem;
    private ToolItem toolsToolItem;
    private Menu toolsPopupMenu;
    private Menu settingsMenu;
    private Menu historyMenu;
    private Menu toolsMenu;

    private MenuItem themeAutoItem;
    private MenuItem themeLightItem;
    private MenuItem themeDarkItem;

    private ThemeMode themeMode;
    private final String preferencesNodeName;

    private boolean autoScrollEnabled;
    private int notificationIdCounter;

    private final Map<String, String> autocompleteModel;

    private final List<FileAttachment> attachments;
    private final List<ModelOption> providers;

    private final ChatHistory chatHistory;

    private boolean darkTheme;

    private Color uiBackground;
    private Color uiForeground;
    private Color uiBorder;
    private Color inputBackground;

    private IPropertyChangeListener themeListener;

    public ChatView() {
        this.resourceReader = new BundleResourceReader(ChatView.class);
        this.fontCssBuilder = new EmbeddedFontCssBuilder(resourceReader);
        this.markdown = new MiniMarkdownParser();
        this.presenter = new DummyChatPresenter(this, markdown);

        this.autoScrollEnabled = true;
        this.notificationIdCounter = 0;

        this.autocompleteModel = new LinkedHashMap<String, String>();
        this.autocompleteModel.put("help", "Show available dummy commands");
        this.autocompleteModel.put("new", "Start a new chat session");
        this.autocompleteModel.put("history", "Show chat history menu");

        this.attachments = new ArrayList<FileAttachment>();
        this.providers = createDefaultProviders();
        this.chatHistory = new ChatHistory();
        this.preferencesNodeName = FrameworkUtil.getBundle(ChatView.class).getSymbolicName();
        this.themeMode = ThemeMode.AUTO;
        this.darkTheme = true;
    }

    @Override
    public void createPartControl(Composite parent) {
        this.root = new Composite(parent, SWT.NONE);
        GridLayout rootLayout = new GridLayout(1, false);
        rootLayout.marginWidth = 0;
        rootLayout.marginHeight = 0;
        root.setLayout(rootLayout);

        this.themeMode = loadThemeModePreference();
        this.darkTheme = resolveDarkTheme();

        // Load persisted selections early so controls can restore their state.
        final String persistedMode = loadChatModePreference();
        final String persistedProviderId = loadChatProviderPreference();

        Composite header = createConversationHeader(root);
        header.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Composite browserContainer = new Composite(root, SWT.NONE);
        browserContainer.setLayout(new FillLayout());
        browserContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        browser = new Browser(browserContainer, SWT.NONE);
        initializeFunctions(browser);
        initializeChatView(browser);

        attachmentsBar = createAttachmentsBar(root);
        GridData attachmentsData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        attachmentsData.heightHint = 0;
        attachmentsBar.setLayoutData(attachmentsData);

        promptContainer = createPromptContainer(root, persistedMode, persistedProviderId);
        GridData promptData = new GridData(SWT.FILL, SWT.BOTTOM, true, false);
        promptData.heightHint = 160;
        promptContainer.setLayoutData(promptData);

        applyThemeFromMode();
        registerThemeListener();

        chatHistory.ensureActiveSessionExists();
        showWelcome();
    }

    @Override
    public void setFocus() {
        if (inputArea != null && !inputArea.isDisposed()) {
            inputArea.setFocus();
        }
        presenter.onViewVisible();
    }

    @Override
    public void dispose() {
        unregisterThemeListener();
        disposeMenus();
        disposeColors();
        super.dispose();
    }

    private void disposeMenus() {
        disposeMenu(settingsMenu);
        disposeMenu(historyMenu);
        disposeMenu(toolsMenu);
    }

    private void disposeMenu(Menu menu) {
        if (menu == null) {
            return;
        }
        if (!menu.isDisposed()) {
            menu.dispose();
        }
    }


    private Composite createConversationHeader(Composite parent) {
        Composite header = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 8;
        layout.marginHeight = 6;
        header.setLayout(layout);

        ToolBar toolbar = new ToolBar(header, SWT.FLAT | SWT.RIGHT);
        toolbar.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false));

        // Order like Copilot: Settings, History, New Chat, Resend, Tools
        settingsItem = new ToolItem(toolbar, SWT.PUSH);
        settingsItem.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ELCL_SYNCED));
        settingsItem.setToolTipText("Settings");

        historyToolItem = new ToolItem(toolbar, SWT.PUSH);
        historyToolItem.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER));
        historyToolItem.setToolTipText("History");

        newChatToolItem = new ToolItem(toolbar, SWT.PUSH);
        newChatToolItem.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_NEW_WIZARD));
        newChatToolItem.setToolTipText("New chat");

        resendToolItem = new ToolItem(toolbar, SWT.PUSH);
        resendToolItem.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_REDO));
        resendToolItem.setToolTipText("Resend");

        toolsToolItem = new ToolItem(toolbar, SWT.PUSH);
        toolsToolItem.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ELCL_SYNCED));
        toolsToolItem.setToolTipText("Tools");

        createMenus(toolbar);

        settingsItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                openSettingsMenu(toolbar);
            }
        });
        historyToolItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                openHistoryMenuForToolItem(toolbar, historyToolItem);
            }
        });
        newChatToolItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                startNewChat();
            }
        });
        resendToolItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                presenter.onReplayLastMessage();
            }
        });
        toolsToolItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                openToolsMenu(toolbar);
            }
        });

        return header;
    }

    private void createMenus(final ToolBar toolbar) {
        if (settingsMenu != null && !settingsMenu.isDisposed()) {
            settingsMenu.dispose();
        }
        if (historyMenu != null && !historyMenu.isDisposed()) {
            historyMenu.dispose();
        }
        if (toolsMenu != null && !toolsMenu.isDisposed()) {
            toolsMenu.dispose();
        }
        if (toolsPopupMenu != null && !toolsPopupMenu.isDisposed()) {
            toolsPopupMenu.dispose();
        }

        settingsMenu = new Menu(getSite().getShell(), SWT.POP_UP);

        // Tools are now their own top-level button, not inside Settings.
        toolsPopupMenu = new Menu(getSite().getShell(), SWT.POP_UP);
        toolsMenu = toolsPopupMenu;
        addToolMenuItem(toolsMenu, "Explain selection", true);
        addToolMenuItem(toolsMenu, "Generate tests", true);
        addToolMenuItem(toolsMenu, "Refactor", true);
        addToolMenuItem(toolsMenu, "Configure tools...", true);

        MenuItem appearanceCascade = new MenuItem(settingsMenu, SWT.CASCADE);
        appearanceCascade.setText("Appearance");
        Menu appearanceMenu = new Menu(settingsMenu);
        appearanceCascade.setMenu(appearanceMenu);

        themeAutoItem = new MenuItem(appearanceMenu, SWT.RADIO);
        themeAutoItem.setText("Auto (follow Eclipse)");
        themeAutoItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (themeAutoItem.getSelection()) {
                    setThemeMode(ThemeMode.AUTO);
                }
            }
        });

        themeLightItem = new MenuItem(appearanceMenu, SWT.RADIO);
        themeLightItem.setText("Light");
        themeLightItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (themeLightItem.getSelection()) {
                    setThemeMode(ThemeMode.LIGHT);
                }
            }
        });

        themeDarkItem = new MenuItem(appearanceMenu, SWT.RADIO);
        themeDarkItem.setText("Dark");
        themeDarkItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (themeDarkItem.getSelection()) {
                    setThemeMode(ThemeMode.DARK);
                }
            }
        });

        updateThemeMenuSelection();

        settingsMenu.addMenuListener(new MenuAdapter() {
            @Override
            public void menuShown(MenuEvent e) {
                updateThemeMenuSelection();
            }
        });

        historyMenu = new Menu(getSite().getShell(), SWT.POP_UP);
    }

    private void openSettingsMenu(ToolBar toolbar) {
        if (settingsMenu == null || settingsMenu.isDisposed() || settingsItem == null) {
            return;
        }
        Rectangle rect = settingsItem.getBounds();
        Point pt = toolbar.toDisplay(new Point(rect.x, rect.y + rect.height));
        settingsMenu.setLocation(pt);
        settingsMenu.setVisible(true);
    }

    private void openHistoryMenu() {
        if (historyMenu == null || historyMenu.isDisposed()) {
            return;
        }
        rebuildHistoryMenu();
        Point p = Display.getDefault().getCursorLocation();
        historyMenu.setLocation(p);
        historyMenu.setVisible(true);
    }

    private void openHistoryMenuForToolItem(ToolBar toolbar, ToolItem item) {
        if (historyMenu == null || historyMenu.isDisposed() || toolbar == null || item == null) {
            return;
        }
        rebuildHistoryMenu();
        Rectangle rect = item.getBounds();
        Point pt = toolbar.toDisplay(new Point(rect.x, rect.y + rect.height));
        historyMenu.setLocation(pt);
        historyMenu.setVisible(true);
    }

    private void openToolsMenu(ToolBar toolbar) {
        if (toolsPopupMenu == null || toolsPopupMenu.isDisposed() || toolsToolItem == null) {
            return;
        }
        Rectangle rect = toolsToolItem.getBounds();
        Point pt = toolbar.toDisplay(new Point(rect.x, rect.y + rect.height));
        toolsPopupMenu.setLocation(pt);
        toolsPopupMenu.setVisible(true);
    }

    private void updateThemeMenuSelection() {
        if (themeAutoItem == null || themeLightItem == null || themeDarkItem == null) {
            return;
        }
        ThemeMode mode = themeMode == null ? ThemeMode.AUTO : themeMode;
        themeAutoItem.setSelection(mode == ThemeMode.AUTO);
        themeLightItem.setSelection(mode == ThemeMode.LIGHT);
        themeDarkItem.setSelection(mode == ThemeMode.DARK);
    }

    private void addToolMenuItem(Menu menu, String label, boolean enabled) {
        MenuItem item = new MenuItem(menu, SWT.PUSH);
        item.setText(label);
        item.setEnabled(enabled);
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                showNotification("Tool is a dummy action in this plugin.", Duration.ofSeconds(2), NotificationType.INFO);
            }
        });
    }

    private Composite createAttachmentsBar(Composite parent) {
        Composite bar = new Composite(parent, SWT.NONE);
        this.attachmentsBar = bar;
        GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 8;
        layout.marginHeight = 6;
        layout.horizontalSpacing = 8;
        bar.setLayout(layout);

        ToolBar left = new ToolBar(bar, SWT.FLAT);
        left.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

        ToolItem attach = new ToolItem(left, SWT.PUSH);
        attach.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FILE));
        attach.setToolTipText("Attach files");
        attach.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                openFileDialogAndAttach();
            }
        });

        attachmentsScroll = new ScrolledComposite(bar, SWT.H_SCROLL | SWT.V_SCROLL);
        attachmentsScroll.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        attachmentsScroll.setExpandHorizontal(true);
        attachmentsScroll.setExpandVertical(true);

        attachmentChips = new Composite(attachmentsScroll, SWT.NONE);
        RowLayout row = new RowLayout(SWT.HORIZONTAL);
        row.spacing = 8;
        row.marginTop = 2;
        row.marginBottom = 2;
        row.marginLeft = 2;
        row.marginRight = 2;
        row.wrap = true;
        attachmentChips.setLayout(row);

        attachmentsScroll.setContent(attachmentChips);
        attachmentsScroll.setMinSize(attachmentChips.computeSize(SWT.DEFAULT, SWT.DEFAULT));

        renderAttachments();

        return bar;
    }

    private Composite createPromptContainer(Composite parent, String persistedMode, String persistedProviderId) {
        Composite prompt = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 8;
        layout.marginHeight = 8;
        layout.verticalSpacing = 6;
        prompt.setLayout(layout);

        inputArea = createUserInput(prompt);
        GridData inputData = new GridData(SWT.FILL, SWT.FILL, true, true);
        inputData.heightHint = 110;
        inputArea.setLayoutData(inputData);

        Composite bottomBar = new Composite(prompt, SWT.NONE);
        GridLayout barLayout = new GridLayout(3, false);
        barLayout.marginWidth = 0;
        barLayout.marginHeight = 0;
        barLayout.horizontalSpacing = 8;
        bottomBar.setLayout(barLayout);
        bottomBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Composite left = new Composite(bottomBar, SWT.NONE);
        GridLayout leftLayout = new GridLayout(2, false);
        leftLayout.marginWidth = 0;
        leftLayout.marginHeight = 0;
        leftLayout.horizontalSpacing = 8;
        left.setLayout(leftLayout);
        left.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

        modeCombo = new Combo(left, SWT.DROP_DOWN | SWT.READ_ONLY);
        modeCombo.setItems(new String[]{"Ask", "Edit", "Agent", "Plan"});
        restoreModeSelection(modeCombo, persistedMode);
        modeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                saveChatModePreference(modeCombo.getText());
            }
        });

        providerCombo = new Combo(left, SWT.DROP_DOWN | SWT.READ_ONLY);
        providerCombo.setItems(toProviderLabels(providers));
        int providerIndex = restoreProviderSelection(providerCombo, persistedProviderId);
        if (providerIndex >= 0 && providerIndex < providers.size()) {
            presenter.onChatModelSelected(providers.get(providerIndex).id);
        }
        providerCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int idx = providerCombo.getSelectionIndex();
                if (idx >= 0 && idx < providers.size()) {
                    saveChatProviderPreference(providers.get(idx).id);
                    presenter.onChatModelSelected(providers.get(idx).id);
                }
            }
        });

        Label spacer = new Label(bottomBar, SWT.NONE);
        spacer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        ToolBar actions = new ToolBar(bottomBar, SWT.FLAT | SWT.RIGHT);
        actions.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));

        ToolItem stop = new ToolItem(actions, SWT.PUSH);
        stop.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ELCL_STOP));
        stop.setToolTipText("Cancel");
        stop.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                presenter.onStop();
            }
        });

        ToolItem send = new ToolItem(actions, SWT.PUSH);
        send.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_FORWARD));
        send.setToolTipText("Send (Enter)");
        send.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                sendIfNotEmpty();
            }
        });

        setupAutocomplete(inputArea);

        return prompt;
    }

    private Text createUserInput(Composite parent) {
        Text t = new Text(parent, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        t.setMessage("Type a message... (Enter to send, Shift+Enter for a new line)");
        t.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.keyCode == SWT.CR && (e.stateMask & SWT.SHIFT) == 0) {
                    e.doit = false;
                    sendIfNotEmpty();
                }
            }
        });
        return t;
    }

    private void setupAutocomplete(Text text) {
        IContentProposalProvider provider = new IContentProposalProvider() {
            @Override
            public IContentProposal[] getProposals(String contents, int position) {
                if (contents == null) {
                    return new IContentProposal[0];
                }
                int slash = contents.lastIndexOf('/');
                if (slash < 0) {
                    return new IContentProposal[0];
                }
                String prefix = contents.substring(slash + 1).trim();
                List<IContentProposal> proposals = new ArrayList<IContentProposal>();
                for (final Map.Entry<String, String> entry : autocompleteModel.entrySet()) {
                    if (prefix.isEmpty() || entry.getKey().startsWith(prefix)) {
                        proposals.add(new IContentProposal() {
                            @Override
                            public String getContent() {
                                return "/" + entry.getKey() + " ";
                            }

                            @Override
                            public int getCursorPosition() {
                                return getContent().length();
                            }

                            @Override
                            public String getDescription() {
                                return entry.getValue();
                            }

                            @Override
                            public String getLabel() {
                                return "/" + entry.getKey();
                            }
                        });
                    }
                }
                return proposals.toArray(new IContentProposal[0]);
            }
        };

        ContentProposalAdapter adapter = new ContentProposalAdapter(text, new TextContentAdapter(), provider, null, null);
        adapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
    }

    private void sendIfNotEmpty() {
        if (inputArea == null || inputArea.isDisposed()) {
            return;
        }
        String text = inputArea.getText();
        if (text == null) {
            return;
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        if (trimmed.startsWith("/")) {
            handleSlashCommand(trimmed);
            return;
        }

        // Include some UI state in the dummy message to keep the demo interesting.
        String decorated = decorateWithContext(trimmed);

        presenter.onSendUserMessage(decorated);
    }

    private String decorateWithContext(String trimmed) {
        String mode = modeCombo == null ? "Ask" : modeCombo.getText();
        String provider = providerCombo == null ? "Custom" : providerCombo.getText();
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(mode).append(" | ").append(provider).append("] ");
        sb.append(trimmed);

        if (!attachments.isEmpty()) {
            sb.append("\n\nAttached files:");
            for (FileAttachment a : attachments) {
                sb.append("\n- ").append(a.displayName);
            }
        }
        return sb.toString();
    }

    private void handleSlashCommand(String trimmed) {
        String cmd = trimmed.substring(1).trim();

        if ("help".equalsIgnoreCase(cmd)) {
            String msg = "Available dummy commands:\n\n- /help\n- /new\n- /history";
            String id = UUID.randomUUID().toString();
            appendMessage(id, "assistant");
            setMessageHtml(id, msg);
            clearUserInput();
            return;
        }

        if ("new".equalsIgnoreCase(cmd)) {
            startNewChat();
            clearUserInput();
            return;
        }

        if ("history".equalsIgnoreCase(cmd)) {
            openHistoryMenu();
            clearUserInput();
            return;
        }

        showNotification("Unknown command: " + trimmed, Duration.ofSeconds(4), NotificationType.WARNING);
        clearUserInput();
    }

    private void startNewChat() {
        chatHistory.archiveActiveSession();
        chatHistory.ensureActiveSessionExists();

        clearAttachments();
        presenter.onClear();
    }

    private void rebuildHistoryMenu() {
        for (MenuItem item : historyMenu.getItems()) {
            item.dispose();
        }

        List<ChatSession> sessions = new ArrayList<ChatSession>(chatHistory.getArchivedSessions());
        Collections.reverse(sessions);

        if (sessions.isEmpty()) {
            MenuItem empty = new MenuItem(historyMenu, SWT.PUSH);
            empty.setText("(No previous chats)");
            empty.setEnabled(false);
            return;
        }

        for (final ChatSession session : sessions) {
            MenuItem it = new MenuItem(historyMenu, SWT.PUSH);
            it.setText(session.title);
            it.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    loadSession(session);
                }
            });
        }
    }

    private void loadSession(ChatSession session) {
        if (session == null) {
            return;
        }

        List<RenderedMessage> snapshot = new ArrayList<RenderedMessage>(session.messages);
        chatHistory.activate(session);

        resetBrowserPresentationOnly();
        clearAttachments();
        renderSnapshot(snapshot);

        showNotification("Loaded chat: " + session.title, Duration.ofSeconds(2), NotificationType.INFO);
    }

    private void resetBrowserPresentationOnly() {
        if (browser == null || browser.isDisposed()) {
            return;
        }
        initializeChatView(browser);
        applyThemeToBrowser();
    }

    private void renderSnapshot(List<RenderedMessage> snapshot) {
        if (browser == null || browser.isDisposed()) {
            return;
        }
        if (snapshot == null) {
            return;
        }

        for (RenderedMessage m : snapshot) {
            appendMessageInBrowserOnly(m.messageId, m.role);
            setMessageHtmlInBrowserOnly(m.messageId, m.htmlOrMarkdown);
        }

        if (autoScrollEnabled) {
            browser.execute("window.scrollTo(0, document.body.scrollHeight);");
        }
    }

    private void appendMessageInBrowserOnly(String messageId, String role) {
        final String cssClass = "user".equals(role) ? "chat-bubble me" : "chat-bubble you";
        String id = escapeForJs(messageId);
        String js = "var node = document.createElement('div');"
                + "node.setAttribute('id', 'message-" + id + "');"
                + "node.setAttribute('class', '" + cssClass + "');"
                + "var toolbar = document.createElement('div');"
                + "toolbar.setAttribute('class', 'message-toolbar');"
                + "var trash = document.createElement('i');"
                + "trash.setAttribute('class', 'fa-solid fa-trash');"
                + "trash.onclick = function() { window.eclipseRemoveMessage('" + id + "'); };"
                + "toolbar.appendChild(trash);"
                + "var content = document.createElement('div');"
                + "content.setAttribute('id', 'message-content-" + id + "');"
                + "node.appendChild(toolbar);"
                + "node.appendChild(content);"
                + "document.getElementById('content').appendChild(node);";
        browser.execute(js);
    }

    private void setMessageHtmlInBrowserOnly(String messageId, String htmlOrMarkdown) {
        String body = markdown.toHtml(htmlOrMarkdown);
        String fixed = escapeForJsStringLiteral(body);
        String id = escapeForJs(messageId);

        String js = "var target = document.getElementById('message-content-" + id + "') || document.getElementById('message-" + id + "');"
                + "if (target) { target.innerHTML = '" + fixed + "'; } renderCode();";
        browser.execute(js);
    }

    private void clearAttachments() {
        attachments.clear();
        renderAttachments();
    }

    private void openFileDialogAndAttach() {
        FileDialog dialog = new FileDialog(getSite().getShell(), SWT.MULTI);
        dialog.setText("Select files");
        dialog.setFilterExtensions(new String[]{"*.*"});
        String first = dialog.open();
        if (first == null) {
            return;
        }
        String dir = dialog.getFilterPath();
        String[] names = dialog.getFileNames();
        for (String name : names) {
            String path = dir + System.getProperty("file.separator") + name;
            attachments.add(new FileAttachment(path, name));
        }
        renderAttachments();
    }

    private void renderAttachments() {
        if (attachments == null) {
            return;
        }
        if (attachmentsBar == null || attachmentsBar.isDisposed()) {
            return;
        }
        if (attachmentChips == null || attachmentChips.isDisposed()) {
            return;
        }

        for (org.eclipse.swt.widgets.Control child : attachmentChips.getChildren()) {
            child.dispose();
        }

        for (int i = 0; i < attachments.size(); i++) {
            FileAttachment a = attachments.get(i);
            createAttachmentChip(attachmentChips, a, i);
        }

        attachmentChips.layout(true, true);
        if (attachmentsScroll != null && !attachmentsScroll.isDisposed()) {
            attachmentsScroll.setMinSize(attachmentChips.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        }

        Object layoutData = attachmentsBar.getLayoutData();
        if (layoutData instanceof GridData) {
            ((GridData) layoutData).heightHint = attachments.isEmpty() ? 0 : 42;
        }

        org.eclipse.swt.widgets.Composite parent = attachmentsBar.getParent();
        if (parent != null && !parent.isDisposed()) {
            parent.layout(true, true);
        }

    }

    private void createAttachmentChip(Composite parent, final FileAttachment a, final int index) {
        Composite chip = new Composite(parent, SWT.BORDER);
        GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 6;
        layout.marginHeight = 2;
        layout.horizontalSpacing = 6;
        chip.setLayout(layout);

        Label name = new Label(chip, SWT.NONE);
        name.setText(a.displayName);

        Label remove = new Label(chip, SWT.NONE);
        remove.setText("✕");
        remove.setToolTipText("Remove");
        remove.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseUp(MouseEvent e) {
                if (index >= 0 && index < attachments.size()) {
                    attachments.remove(index);
                    renderAttachments();
                }
            }
        });

        applyThemeToChip(chip, name, remove);
    }

    private void showWelcome() {
        String welcomeId = "welcome-message";
        appendMessage(welcomeId, "assistant");
        setMessageHtml(welcomeId,
                "Welcome! This is a Copilot-like UI dummy.\n\n"
                        + "Enter sends, Shift+Enter adds a line break.\n"
                        + "Use the mode/provider dropdowns and attach files to see how the UI behaves.");
    }

    private void restoreModeSelection(Combo combo, String persistedMode) {
        if (combo == null || combo.isDisposed()) {
            return;
        }
        String desired = persistedMode == null ? "" : persistedMode.trim();
        if (desired.isEmpty()) {
            desired = DEFAULT_CHAT_MODE;
        }

        String[] items = combo.getItems();
        for (int i = 0; i < items.length; i++) {
            if (desired.equalsIgnoreCase(items[i])) {
                combo.select(i);
                return;
            }
        }
        combo.select(0);
    }

    private int restoreProviderSelection(Combo combo, String persistedProviderId) {
        if (combo == null || combo.isDisposed()) {
            return 0;
        }
        String desired = persistedProviderId == null ? "" : persistedProviderId.trim();
        if (desired.isEmpty()) {
            desired = DEFAULT_PROVIDER_ID;
        }

        for (int i = 0; i < providers.size(); i++) {
            if (desired.equalsIgnoreCase(providers.get(i).id)) {
                combo.select(i);
                return i;
            }
        }
        combo.select(0);
        return 0;
    }

    private String loadChatModePreference() {
        try {
            IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(preferencesNodeName);
            return prefs.get(PREF_CHAT_MODE, DEFAULT_CHAT_MODE);
        } catch (Exception ex) {
            return DEFAULT_CHAT_MODE;
        }
    }

    private void saveChatModePreference(String mode) {
        String value = (mode == null || mode.trim().isEmpty()) ? DEFAULT_CHAT_MODE : mode.trim();
        try {
            IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(preferencesNodeName);
            prefs.put(PREF_CHAT_MODE, value);
            prefs.flush();
        } catch (BackingStoreException ex) {
            // best-effort
        }
    }

    private String loadChatProviderPreference() {
        try {
            IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(preferencesNodeName);
            return prefs.get(PREF_CHAT_PROVIDER_ID, DEFAULT_PROVIDER_ID);
        } catch (Exception ex) {
            return DEFAULT_PROVIDER_ID;
        }
    }

    private void saveChatProviderPreference(String providerId) {
        String value = (providerId == null || providerId.trim().isEmpty()) ? DEFAULT_PROVIDER_ID : providerId.trim();
        try {
            IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(preferencesNodeName);
            prefs.put(PREF_CHAT_PROVIDER_ID, value);
            prefs.flush();
        } catch (BackingStoreException ex) {
            // best-effort
        }
    }


    private ThemeMode loadThemeModePreference() {
        try {
            IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(preferencesNodeName);
            String value = prefs.get(PREF_THEME_MODE, ThemeMode.AUTO.name());
            return ThemeMode.valueOf(value);
        } catch (Exception ex) {
            return ThemeMode.AUTO;
        }
    }

    private void saveThemeModePreference(ThemeMode mode) {
        try {
            IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(preferencesNodeName);
            prefs.put(PREF_THEME_MODE, mode.name());
            prefs.flush();
        } catch (BackingStoreException ex) {
            // Ignore - preferences are best-effort.
        }
    }

    private void setThemeMode(ThemeMode mode) {
        if (mode == null) {
            mode = ThemeMode.AUTO;
        }
        this.themeMode = mode;
        saveThemeModePreference(mode);
        applyThemeFromMode();
    }

    private boolean resolveDarkTheme() {
        if (themeMode == ThemeMode.DARK) {
            return true;
        }
        if (themeMode == ThemeMode.LIGHT) {
            return false;
        }
        return detectDarkTheme();
    }

    private void applyThemeFromMode() {
        this.darkTheme = resolveDarkTheme();
        createOrUpdateColors(darkTheme);
        applyThemeToSwt();
        applyThemeToBrowser();
    }

    private void applyAutoTheme() {
        if (themeMode == ThemeMode.AUTO) {
            applyThemeFromMode();
        }
    }

    private boolean detectDarkTheme() {
        String themeId = "";
        try {
            IThemeManager tm = PlatformUI.getWorkbench().getThemeManager();
            ITheme current = tm.getCurrentTheme();
            if (current != null && current.getId() != null) {
                themeId = current.getId();
            }
        } catch (Exception ex) {
            // Ignore and fall back to luminance detection.
        }

        if (themeId.toLowerCase().contains("dark")) {
            return true;
        }

        RGB bg = Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND).getRGB();
        int luminance = (int) (0.2126 * bg.red + 0.7152 * bg.green + 0.0722 * bg.blue);
        return luminance < 128;
    }

    private void registerThemeListener() {
        if (themeListener != null) {
            return;
        }
        themeListener = new IPropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                if (themeMode == ThemeMode.AUTO) {
                    applyThemeFromMode();
                }
            }
        };
        try {
            PlatformUI.getWorkbench().getThemeManager().addPropertyChangeListener(themeListener);
        } catch (Exception ex) {
            // Ignore when running without workbench.
        }
    }

    private void unregisterThemeListener() {
        if (themeListener == null) {
            return;
        }
        try {
            PlatformUI.getWorkbench().getThemeManager().removePropertyChangeListener(themeListener);
        } catch (Exception ex) {
            // Ignore
        }
        themeListener = null;
    }

    private void createOrUpdateColors(boolean dark) {
        disposeColors();

        Display d = Display.getDefault();
        if (dark) {
            uiBackground = new Color(d, 30, 30, 30);
            uiForeground = new Color(d, 230, 230, 230);
            uiBorder = new Color(d, 70, 70, 70);
            inputBackground = new Color(d, 35, 35, 35);
        } else {
            uiBackground = new Color(d, 250, 250, 250);
            uiForeground = new Color(d, 25, 25, 25);
            uiBorder = new Color(d, 200, 200, 200);
            inputBackground = new Color(d, 255, 255, 255);
        }
    }

    private void disposeColors() {
        disposeColor(uiBackground);
        disposeColor(uiForeground);
        disposeColor(uiBorder);
        disposeColor(inputBackground);
        uiBackground = null;
        uiForeground = null;
        uiBorder = null;
        inputBackground = null;
    }

    private void disposeColor(Color c) {
        if (c != null && !c.isDisposed()) {
            c.dispose();
        }
    }

    private void applyThemeToSwt() {
        if (root == null || root.isDisposed()) {
            return;
        }

        applyRecursive(root);

        if (inputArea != null && !inputArea.isDisposed()) {
            inputArea.setBackground(inputBackground);
            inputArea.setForeground(uiForeground);
        }

        if (modeCombo != null && !modeCombo.isDisposed()) {
            modeCombo.setBackground(inputBackground);
            modeCombo.setForeground(uiForeground);
        }
        if (providerCombo != null && !providerCombo.isDisposed()) {
            providerCombo.setBackground(inputBackground);
            providerCombo.setForeground(uiForeground);
        }

        renderAttachments();
    }

    private void applyRecursive(Composite c) {
        c.setBackground(uiBackground);
        for (org.eclipse.swt.widgets.Control child : c.getChildren()) {
            child.setBackground(uiBackground);
            child.setForeground(uiForeground);
            if (child instanceof Composite) {
                applyRecursive((Composite) child);
            }
        }
    }

    private void applyThemeToChip(Composite chip, Label name, Label remove) {
        if (chip == null || chip.isDisposed()) {
            return;
        }
        chip.setBackground(inputBackground);
        name.setBackground(inputBackground);
        name.setForeground(uiForeground);
        remove.setBackground(inputBackground);
        remove.setForeground(uiForeground);
    }

    private void applyThemeToBrowser() {
        if (browser == null || browser.isDisposed()) {
            return;
        }

        String css = buildBrowserThemeCss(darkTheme);
        String escaped = escapeForJsStringLiteral(css);
        String js = "var style = document.getElementById('theme-css');"
                + "if(style){ style.innerHTML = '" + escaped + "'; }";

        browser.execute(js);
    }

    private String buildBrowserThemeCss(boolean dark) {
        if (dark) {
            return "html,body,#content{background-color:#1e1e1e !important;color:#f0f0f0 !important;}"
                    + ".chat-bubble{border-bottom:1px solid rgba(255,255,255,0.10) !important;}"
                    + ".chat-bubble.me{background-color:#2d333b !important;}"
                    + ".chat-bubble.you{background-color:#1e1e1e !important;}"
                    + "pre,code{background-color:rgba(255,255,255,0.08) !important;}";
        }
        return "html,body,#content{background-color:#ffffff !important;color:#111111 !important;}"
                + ".chat-bubble{border-bottom:1px solid rgba(0,0,0,0.08) !important;}"
                + ".chat-bubble.me{background-color:#f3f3f3 !important;}"
                + ".chat-bubble.you{background-color:#ffffff !important;}"
                + "pre,code{background-color:rgba(0,0,0,0.08) !important;}";
    }

    private static String[] toProviderLabels(List<ModelOption> providers) {
        String[] arr = new String[providers.size()];
        for (int i = 0; i < providers.size(); i++) {
            arr[i] = providers.get(i).displayName;
        }
        return arr;
    }

    private static List<ModelOption> createDefaultProviders() {
        List<ModelOption> list = new ArrayList<ModelOption>();
        list.add(new ModelOption("custom", "Custom", null));
        list.add(new ModelOption("openai", "ChatGPT", null));
        list.add(new ModelOption("anthropic", "Claude", null));
        list.add(new ModelOption("google", "Gemini", null));
        list.add(new ModelOption("grok", "Grok", null));
        list.add(new ModelOption("deepseek", "DeepSeek", null));
        return list;
    }

    private void initializeFunctions(Browser b) {
        new CopyCodeFunction(b, "eclipseFunc");
        new CopyCodeFunction(b, "eclipseCopyCode");
        new ApplyPatchFunction(b, "eclipseApplyPatch");
        new DiffCodeFunction(b, "eclipseDiffCode");
        new InsertCodeFunction(b, "eclipseInsertCode");
        new NewFileFunction(b, "eclipseNewFile");
        new ScrollInteractionFunction(b, "eclipseScrollInteraction");
        new RemoveMessageFunction(b, "eclipseRemoveMessage");
    }

    private void initializeChatView(Browser b) {
        String css = loadCss();
        String js = loadJavaScripts();
        String fonts = fontCssBuilder.buildCss();
        String themeCss = buildBrowserThemeCss(darkTheme);

        String banner = "";
        boolean missingCoreAssets = css.indexOf("/*MISSING:textview.css*/") >= 0 || js.indexOf("/*MISSING:textview.js*/") >= 0;
        if (missingCoreAssets) {
            banner = FallbackUiResources.missingAssetsBannerHtml(
                    "Mindestens css/textview.css oder js/textview.js wurde im installierten Bundle nicht gefunden. "
                            + "Prüfe build.properties (bin.includes) und ob die Ressourcen im Bundle-JAR wirklich enthalten sind. "
                            + "Details stehen im Error Log."
            );
        }

        String html = "<!DOCTYPE html>"
                + "<html>"
                + "<head>"
                + "<meta charset=\"utf-8\"/>"
                + "<style>" + css + "</style>"
                + "<style>" + fonts + "</style>"
                + "<style id=\"theme-css\">" + themeCss + "</style>"
                + "<script>" + js + "</script>"
                + "</head>"
                + "<body>"
                + "<div id=\"notification-container\"></div>"
                + banner
                + "<div id=\"content\"></div>"
                + "</body>"
                + "</html>";

        b.setText(html);
    }

    private String loadCss() {
        List<String> cssFiles = new ArrayList<String>();
        cssFiles.add("textview.css");
        if (darkTheme) {
            cssFiles.add("dark.min.css");
        }
        cssFiles.add("fa6.all.min.css");
        cssFiles.add("katex.min.css");
        StringBuilder sb = new StringBuilder();

        for (String f : cssFiles) {
            String path = "css/" + f;
            String content = resourceReader.readUtf8OrEmpty(path);
            if (content.isEmpty() && "textview.css".equals(f)) {
                sb.append("/*MISSING:textview.css*/\n");
                sb.append(FallbackUiResources.minimalCss()).append("\n");
            } else {
                sb.append(content).append("\n");
            }
        }
        return sb.toString();
    }

    private String loadJavaScripts() {
        String[] jsFiles = new String[]{"highlight.min.js", "textview.js", "katex.min.js"};
        StringBuilder sb = new StringBuilder();

        for (String f : jsFiles) {
            String path = "js/" + f;
            String content = resourceReader.readUtf8OrEmpty(path);
            if (content.isEmpty() && "textview.js".equals(f)) {
                sb.append("/*MISSING:textview.js*/\n");
                sb.append(FallbackUiResources.minimalJs()).append("\n\n");
            } else {
                sb.append(content).append("\n\n");
            }
        }

        sb.append("\n\n").append(FallbackUiResources.minimalJs());
        return sb.toString();
    }

    // --- ChatViewPort

    @Override
    public void clearChatView() {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (browser == null || browser.isDisposed()) {
                    return;
                }
                chatHistory.clearActiveSessionMessages();
                initializeChatView(browser);
                applyThemeToBrowser();
            }
        });
    }

    @Override
    public void clearUserInput() {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (inputArea == null || inputArea.isDisposed()) {
                    return;
                }
                inputArea.setText("");
            }
        });
    }

    @Override
    public void appendMessage(final String messageId, final String role) {
        final String cssClass = "user".equals(role) ? "chat-bubble me" : "chat-bubble you";
        chatHistory.appendMessageToActiveSession(messageId, role);

        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (browser == null || browser.isDisposed()) {
                    return;
                }
                String id = escapeForJs(messageId);
                String js = "var node = document.createElement('div');"
                        + "node.setAttribute('id', 'message-" + id + "');"
                        + "node.setAttribute('class', '" + cssClass + "');"
                        + "var toolbar = document.createElement('div');"
                        + "toolbar.setAttribute('class', 'message-toolbar');"
                        + "var trash = document.createElement('i');"
                        + "trash.setAttribute('class', 'fa-solid fa-trash');"
                        + "trash.onclick = function() { window.eclipseRemoveMessage('" + id + "'); };"
                        + "toolbar.appendChild(trash);"
                        + "var content = document.createElement('div');"
                        + "content.setAttribute('id', 'message-content-" + id + "');"
                        + "node.appendChild(toolbar);"
                        + "node.appendChild(content);"
                        + "document.getElementById('content').appendChild(node);";
                browser.execute(js);
                if (autoScrollEnabled) {
                    browser.execute("window.scrollTo(0, document.body.scrollHeight);");
                }
            }
        });
    }

    @Override
    public void setMessageHtml(final String messageId, final String htmlOrMarkdown) {
        chatHistory.setMessageHtmlInActiveSession(messageId, htmlOrMarkdown);

        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (browser == null || browser.isDisposed()) {
                    return;
                }

                String body = markdown.toHtml(htmlOrMarkdown);
                String fixed = escapeForJsStringLiteral(body);
                String id = escapeForJs(messageId);

                String js = "var target = document.getElementById('message-content-" + id + "') || document.getElementById('message-" + id + "');"
                        + "if (target) { target.innerHTML = '" + fixed + "'; } renderCode();";

                browser.execute(js);
                if (autoScrollEnabled) {
                    browser.execute("window.scrollTo(0, document.body.scrollHeight);");
                }
            }
        });
    }

    @Override
    public void removeMessage(final String messageId) {
        chatHistory.removeMessageFromActiveSession(messageId);

        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (browser == null || browser.isDisposed()) {
                    return;
                }
                String id = escapeForJs(messageId);
                browser.execute("var node = document.getElementById('message-" + id + "'); if(node) { node.remove(); }");
            }
        });
    }

    @Override
    public void showNotification(final String message, final Duration duration, final NotificationType type) {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (browser == null || browser.isDisposed()) {
                    return;
                }
                String notificationId = "notification-" + (notificationIdCounter++);
                String icon;
                String bg;
                String fg;

                if (type == NotificationType.ERROR) {
                    icon = "fa-solid fa-circle-xmark";
                    bg = darkTheme ? "#522" : "#ffdddd";
                    fg = darkTheme ? "#fff" : "#7a0000";
                } else if (type == NotificationType.WARNING) {
                    icon = "fa-solid fa-triangle-exclamation";
                    bg = darkTheme ? "#544400" : "#fff4cc";
                    fg = darkTheme ? "#fff" : "#6b4e00";
                } else {
                    icon = "fa-solid fa-circle-info";
                    bg = darkTheme ? "#0b3a56" : "#ddf1ff";
                    fg = darkTheme ? "#fff" : "#004b7a";
                }

                String safeMsg = escapeForJs(message);
                String js = "showNotification('" + notificationId + "', '" + icon + "', '" + bg + "', '" + fg + "', '" + safeMsg + "');";
                browser.execute(js);

                long ms = duration == null ? 0L : duration.toMillis();
                if (ms > 0L) {
                    browser.execute("setTimeout(function(){ removeNotification('" + notificationId + "'); }, " + ms + ");");
                }
            }
        });
    }

    // --- Utilities

    private static String escapeForJs(String text) {
        if (text == null) {
            return "";
        }
        String s = text;
        s = s.replace("\\", "\\\\");
        s = s.replace("\"", "\\\"");
        s = s.replace("'", "\\'");
        s = s.replace("\r", "");
        return s;
    }

    private static String escapeForJsStringLiteral(String text) {
        if (text == null) {
            return "";
        }
        String s = text;
        s = s.replace("\\", "\\\\");
        s = s.replace("\r", "");
        s = s.replace("\n", "\\n");
        s = s.replace("\t", "\\t");
        s = s.replace("'", "\\'");
        return s;
    }

    private static final class FileAttachment {
        private final String path;
        private final String displayName;

        private FileAttachment(String path, String displayName) {
            this.path = path;
            this.displayName = displayName;
        }
    }

    private static final class ModelOption {
        private final String id;
        private final String displayName;
        @SuppressWarnings("unused")
        private final String iconFile;

        private ModelOption(String id, String displayName, String iconFile) {
            this.id = id;
            this.displayName = displayName;
            this.iconFile = iconFile;
        }
    }

    private static final class RenderedMessage {
        private final String messageId;
        private final String role;
        private String htmlOrMarkdown;

        private RenderedMessage(String messageId, String role) {
            this.messageId = messageId;
            this.role = role;
        }
    }

    private static final class ChatSession {
        private final String id;
        private final String title;
        private final List<RenderedMessage> messages;

        private ChatSession(String id, String title) {
            this.id = id;
            this.title = title;
            this.messages = new ArrayList<RenderedMessage>();
        }
    }

    private static final class ChatHistory {
        private ChatSession active;
        private final List<ChatSession> archived;

        private ChatHistory() {
            this.archived = new ArrayList<ChatSession>();
        }

        private void ensureActiveSessionExists() {
            if (active != null) {
                return;
            }
            active = newSession();
        }

        private void archiveActiveSession() {
            if (active == null) {
                active = newSession();
                return;
            }
            if (!active.messages.isEmpty()) {
                if (!archived.contains(active)) {
                    archived.add(active);
                }
            }
            active = newSession();
        }

        private void activate(ChatSession session) {
            if (session == null) {
                return;
            }
            this.active = session;
        }

        private ChatSession newSession() {
            String id = UUID.randomUUID().toString();
            String title = "Chat " + HISTORY_TITLE_FORMAT.format(LocalDateTime.now());
            return new ChatSession(id, title);
        }

        private List<ChatSession> getArchivedSessions() {
            return archived;
        }

        private void clearActiveSessionMessages() {
            if (active != null) {
                active.messages.clear();
            }
        }

        private void appendMessageToActiveSession(String messageId, String role) {
            ensureActiveSessionExists();
            active.messages.add(new RenderedMessage(messageId, role));
        }

        private void setMessageHtmlInActiveSession(String messageId, String htmlOrMarkdown) {
            ensureActiveSessionExists();
            RenderedMessage m = find(active.messages, messageId);
            if (m != null) {
                m.htmlOrMarkdown = htmlOrMarkdown;
            }
        }

        private void removeMessageFromActiveSession(String messageId) {
            ensureActiveSessionExists();
            RenderedMessage m = find(active.messages, messageId);
            if (m != null) {
                active.messages.remove(m);
            }
        }

        private RenderedMessage find(List<RenderedMessage> messages, String messageId) {
            for (RenderedMessage m : messages) {
                if (m.messageId.equals(messageId)) {
                    return m;
                }
            }
            return null;
        }
    }

// --- Browser Functions



    private final class CopyCodeFunction extends BrowserFunction {
        private CopyCodeFunction(Browser browser, String name) {
            super(browser, name);
        }

        @Override
        public Object function(Object[] arguments) {
            if (arguments != null && arguments.length > 0 && arguments[0] instanceof String) {
                String code = (String) arguments[0];
                copyToClipboard(code);
                presenter.onCopyCode(code);
            }
            return null;
        }
    }

    private final class ApplyPatchFunction extends BrowserFunction {
        private ApplyPatchFunction(Browser browser, String name) {
            super(browser, name);
        }

        @Override
        public Object function(Object[] arguments) {
            if (arguments != null && arguments.length > 0 && arguments[0] instanceof String) {
                presenter.onApplyPatch((String) arguments[0]);
            }
            return null;
        }
    }

    private final class InsertCodeFunction extends BrowserFunction {
        private InsertCodeFunction(Browser browser, String name) {
            super(browser, name);
        }

        @Override
        public Object function(Object[] arguments) {
            if (arguments != null && arguments.length > 0 && arguments[0] instanceof String) {
                presenter.onInsertCode((String) arguments[0]);
            }
            return null;
        }
    }

    private final class DiffCodeFunction extends BrowserFunction {
        private DiffCodeFunction(Browser browser, String name) {
            super(browser, name);
        }

        @Override
        public Object function(Object[] arguments) {
            if (arguments != null && arguments.length > 0 && arguments[0] instanceof String) {
                presenter.onDiffCode((String) arguments[0]);
            }
            return null;
        }
    }

    private final class NewFileFunction extends BrowserFunction {
        private NewFileFunction(Browser browser, String name) {
            super(browser, name);
        }

        @Override
        public Object function(Object[] arguments) {
            if (arguments != null && arguments.length >= 2 && arguments[0] instanceof String && arguments[1] instanceof String) {
                presenter.onNewFile((String) arguments[0], (String) arguments[1]);
            }
            return null;
        }
    }

    private final class RemoveMessageFunction extends BrowserFunction {
        private RemoveMessageFunction(Browser browser, String name) {
            super(browser, name);
        }

        @Override
        public Object function(Object[] arguments) {
            if (arguments != null && arguments.length > 0 && arguments[0] instanceof String) {
                presenter.onRemoveMessage((String) arguments[0]);
            }
            return null;
        }
    }

    private final class ScrollInteractionFunction extends BrowserFunction {
        private ScrollInteractionFunction(Browser browser, String name) {
            super(browser, name);
        }

        @Override
        public Object function(Object[] arguments) {
            if (arguments != null && arguments.length > 0 && arguments[0] instanceof Boolean) {
                autoScrollEnabled = ((Boolean) arguments[0]).booleanValue();
            }
            return null;
        }
    }

    private void copyToClipboard(String text) {
        Clipboard clipboard = new Clipboard(getSite().getShell().getDisplay());
        try {
            clipboard.setContents(new Object[]{text}, new org.eclipse.swt.dnd.Transfer[]{TextTransfer.getInstance()});
        } finally {
            clipboard.dispose();
        }
    }

    // --- Nested Types

    private static final class SimpleCommandProposal implements IContentProposal {
        private final String command;
        private final String description;

        private SimpleCommandProposal(String command, String description) {
            this.command = command;
            this.description = description;
        }

        @Override
        public String getContent() {
            return "/" + command;
        }

        @Override
        public int getCursorPosition() {
            return getContent().length();
        }

        @Override
        public String getLabel() {
            return "/" + command;
        }

        @Override
        public String getDescription() {
            return description;
        }
    }


}
