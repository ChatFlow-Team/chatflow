package com.chaflow.client.view;

import java.net.URI;
import java.util.Objects;
import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

/** Loads a bundled page and exposes its actions only in that page's window. */
public final class LocalWebView {
    private final WebView view = new WebView();
    private final WebEngine engine = view.getEngine();
    private final URI location;
    // WebKit holds Java bridge objects weakly.
    private final Object actions;
    private JSObject window;

    public LocalWebView(String resource, Object actions) {
        this.actions = actions;
        location = URI.create(Objects.requireNonNull(getClass().getResource("/web/" + resource)).toExternalForm());
        view.setContextMenuEnabled(false);
        engine.setCreatePopupHandler(features -> null);
        engine.locationProperty().addListener((property, oldLocation, newLocation) -> {
            if (!isLocalPage(newLocation)) {
                detach();
                engine.getLoadWorker().cancel();
            }
        });
        engine.getLoadWorker().stateProperty().addListener((property, oldState, state) -> {
            if (state == Worker.State.SCHEDULED) {
                detach();
            } else if (state == Worker.State.SUCCEEDED && isLocalPage(engine.getLocation())) {
                window = (JSObject) engine.executeScript("window");
                window.setMember("chatflow", this.actions);
                window.call("chatflowReady");
            }
        });
        engine.load(location.toString());
    }

    public WebView root() {
        return view;
    }

    public void call(String function, Object... arguments) {
        if (window != null && isLocalPage(engine.getLocation())) {
            window.call(function, arguments);
        }
    }

    private boolean isLocalPage(String url) {
        try {
            return url != null && location.equals(URI.create(url));
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    public void detach() {
        if (window != null) {
            window.removeMember("chatflow");
            window = null;
        }
    }
}
