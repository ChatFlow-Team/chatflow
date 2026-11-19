package com.chaflow.client.view;

import com.chaflow.client.SocketClient;
import com.chaflow.client.controller.AuthController;
import com.chaflow.client.controller.ChatController;
import com.chaflow.client.controller.MessengerController;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import javafx.application.Platform;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public final class MessengerView {
    // PostgreSQL bigint identifiers must survive the trip through JavaScript without rounding.
    private static final JsonMapper JSON = JsonMapper.builder()
            .enable(JsonWriteFeature.WRITE_NUMBERS_AS_STRINGS)
            .build();

    private final SocketClient.AuthResult account;
    private final AuthController auth;
    private final MessengerController messenger;
    private final ChatController chat;
    private final ExecutorService io;
    private final Runnable signedOut;
    private final LocalWebView page;

    private SocketClient.EventConnection events;
    private List<Map<String, Object>> conversations = List.of();
    private long selectedId = -1;
    private long selectionVersion;
    private long searchVersion;
    private boolean ready;
    private boolean closed;

    public MessengerView(SocketClient.AuthResult account, AuthController auth, MessengerController messenger,
                         ChatController chat, ExecutorService io, Runnable signedOut) {
        this.account = account;
        this.auth = auth;
        this.messenger = messenger;
        this.chat = chat;
        this.io = io;
        this.signedOut = signedOut;
        page = new LocalWebView("messenger.html", new Actions());
    }

    public WebView root() {
        return page.root();
    }

    private void initialize() {
        if (ready || closed) return;
        ready = true;
        page.call("showIdentity", Long.toString(account.userId()), account.displayName(), account.username());
        refresh();
        events = chat.listenEvents(account.token(), this::serverEvent,
                error -> onUi(() -> status("Connexion en temps réel interrompue.")));
    }

    private void serverEvent(Map<String, Object> event) {
        switch (text(event, "event")) {
            case "CONNECTED" -> onUi(this::refresh);
            case "MESSAGE_RECEIVED" -> {
                if (!(event.get("data") instanceof Map<?, ?> data)) return;
                long conversationId = number(data, "conversationId");
                onUi(() -> {
                    if (selectedId == conversationId) loadHistory();
                    else refreshConversations();
                });
            }
            default -> { }
        }
    }

    private void refresh() {
        refreshConversations();
        run(() -> {
            String friends = JSON.writeValueAsString(messenger.friends(account.token()));
            String requests = JSON.writeValueAsString(messenger.friendRequests(account.token()));
            onUi(() -> page.call("showContacts", friends, requests));
        });
    }

    private void refreshConversations() {
        run(() -> {
            List<Map<String, Object>> rows = messenger.conversations(account.token());
            String json = JSON.writeValueAsString(rows);
            onUi(() -> {
                conversations = rows;
                page.call("updateConversations", json);
                if (selectedId < 0 && !rows.isEmpty()) select(rows.getFirst());
            });
        });
    }

    private void select(Map<String, Object> conversation) {
        long id = number(conversation, "id");
        String name = text(conversation, "displayName");
        select(id, name, text(conversation, "username"), text(conversation, "status"));
    }

    private void select(long id, String name, String username, String presence) {
        selectedId = id;
        selectionVersion++;
        status("");
        page.call("selectConversation", Long.toString(id),
                name.isBlank() ? "Conversation n° " + id : name, username, presence);
        loadHistory();
    }

    private void loadHistory() {
        long id = selectedId;
        long version = selectionVersion;
        run(() -> {
            try {
                String payload = JSON.writeValueAsString(chat.messages(account.token(), id));
                onUi(() -> {
                    if (selectionVersion == version) page.call("showMessages", Long.toString(id), payload);
                    refreshConversations();
                });
            } catch (IOException error) {
                onUi(() -> {
                    if (selectionVersion == version) {
                        page.call("historyFailed", Long.toString(id));
                        status(errorMessage(error));
                    }
                });
            }
        });
    }

    public final class Actions {
        public void ready() {
            initialize();
        }

        public void select(String id) {
            conversations.stream()
                    .filter(row -> Long.toString(number(row, "id")).equals(id))
                    .findFirst().ifPresent(MessengerView.this::select);
        }

        public void search(String query, boolean keepStatus) {
            long version = ++searchVersion;
            if (query.length() < 2) {
                status("Saisissez au moins deux caractères.");
                return;
            }
            if (!keepStatus) status("");
            run(() -> {
                try {
                    String payload = JSON.writeValueAsString(messenger.search(account.token(), query));
                    onUi(() -> {
                        if (searchVersion == version) page.call("showSearch", payload);
                    });
                } catch (IOException error) {
                    onUi(() -> {
                        if (searchVersion == version) status(errorMessage(error));
                    });
                }
            });
        }

        public void cancelSearch() {
            searchVersion++;
            status("");
        }

        public void openDirect(String userId, String name, String username, String presence) {
            long version = ++selectionVersion;
            run(() -> {
                long id = chat.openDirect(account.token(), Long.parseLong(userId));
                onUi(() -> {
                    if (selectionVersion == version) {
                        searchVersion++;
                        page.call("clearSearch");
                        MessengerView.this.select(id, name, username, presence);
                    } else {
                        refreshConversations();
                    }
                });
            });
        }

        public void changeContact(String action, String personId) {
            run(() -> {
                long id = Long.parseLong(personId);
                String feedback = switch (action) {
                    case "add" -> text(messenger.friendRequest(account.token(), id), "message");
                    case "remove" -> {
                        messenger.removeFriend(account.token(), id);
                        yield "Contact retiré.";
                    }
                    case "accept", "decline" -> {
                        messenger.respond(account.token(), id, "accept".equals(action));
                        yield "Demande traitée.";
                    }
                    default -> throw new IllegalArgumentException("Action inconnue.");
                };
                onUi(() -> {
                    status(feedback);
                    page.call("refreshSearch");
                    refresh();
                });
            });
        }

        public void send(String body) {
            long id = selectedId;
            if (id < 0) return;
            run(() -> {
                try {
                    chat.sendMessage(account.token(), id, body);
                    onUi(() -> {
                        page.call("messageFinished", Long.toString(id), body, true);
                        status("");
                        if (selectedId == id) loadHistory();
                        else refreshConversations();
                    });
                } catch (IOException error) {
                    onUi(() -> {
                        page.call("messageFinished", Long.toString(id), body, false);
                        status(errorMessage(error));
                    });
                }
            });
        }

        public void upload(String filename, String contentType, String base64Data) {
            long id = selectedId;
            if (id < 0) { status("Sélectionnez une conversation."); return; }
            if (base64Data == null || base64Data.length() > 17_000_000) {
                status("Fichier trop volumineux (maximum 12 Mo)."); return;
            }
            status("Envoi du fichier…");
            run(() -> {
                try {
                    chat.uploadAttachment(account.token(), id, filename, contentType, base64Data);
                    onUi(() -> {
                        status("Fichier envoyé.");
                        if (selectedId == id) loadHistory(); else refreshConversations();
                    });
                } catch (IOException error) {
                    onUi(() -> status(errorMessage(error)));
                }
            });
        }

        public void previewAttachment(String blobId) {
            run(() -> {
                try {
                    Map<String,Object> result = chat.downloadAttachment(account.token(), Long.parseLong(blobId));
                    String mime = Objects.toString(result.get("contentType"), "application/octet-stream");
                    if (!mime.startsWith("image/")) return;
                    String data = Objects.toString(result.get("data"), "");
                    onUi(() -> page.call("showAttachmentPreview", blobId, mime, data));
                } catch (Exception error) {
                    onUi(() -> status(errorMessage(error)));
                }
            });
        }

        public void downloadAttachment(String blobId) {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Enregistrer la pièce jointe");
            java.io.File destination = chooser.showSaveDialog(page.root().getScene() == null
                    ? null : page.root().getScene().getWindow());
            if (destination == null) return;
            run(() -> {
                try {
                    Map<String,Object> result = chat.downloadAttachment(account.token(), Long.parseLong(blobId));
                    byte[] bytes = Base64.getDecoder().decode(Objects.toString(result.get("data"), ""));
                    Files.write(destination.toPath(), bytes);
                    onUi(() -> status("Fichier enregistré : " + destination.getName()));
                } catch (Exception error) {
                    onUi(() -> status(errorMessage(error)));
                }
            });
        }

        public void logout() {
            if (closed) return;
            Runnable closeEvents = detach();
            io.submit(() -> {
                closeEvents.run();
                auth.logout(account.token());
                Platform.runLater(signedOut);
            });
        }
    }

    /** Stops delivering UI updates and returns the socket cleanup for a background thread. */
    public Runnable detach() {
        closed = true;
        page.detach();
        SocketClient.EventConnection connection = events;
        events = null;
        return () -> {
            if (connection != null) connection.close();
        };
    }

    private void onUi(Runnable action) {
        Platform.runLater(() -> {
            if (!closed) action.run();
        });
    }

    private void status(String message) {
        page.call("showStatus", message);
    }

    private void run(Request request) {
        io.submit(() -> {
            try {
                request.execute();
            } catch (Exception error) {
                onUi(() -> status(errorMessage(error)));
            }
        });
    }

    private static String errorMessage(Exception error) {
        return Objects.requireNonNullElse(error.getMessage(), "La requête a échoué.");
    }

    private static String text(Map<String, Object> row, String key) {
        return Objects.toString(row.get(key), "");
    }

    private static long number(Map<?, ?> row, String key) {
        return row.get(key) instanceof Number value ? value.longValue() : -1;
    }

    @FunctionalInterface
    private interface Request {
        void execute() throws IOException;
    }
}