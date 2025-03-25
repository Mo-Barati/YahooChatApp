module com.chatapp.yahoochatapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires jbcrypt;
    requires java.desktop;
    requires javafx.web; // ✅ Already added for WebView
    requires javafx.graphics;


    opens com.chatapp.yahoochatapp to javafx.fxml;
    exports com.chatapp.yahoochatapp;
}