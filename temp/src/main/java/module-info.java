module com.chaflow.client {
    requires javafx.controls;
    requires com.fasterxml.jackson.databind;
    exports com.chaflow.client;
    exports com.chaflow.client.controller;
    exports com.chaflow.client.service;
    exports com.chaflow.client.model;
    exports com.chaflow.client.view;
}
