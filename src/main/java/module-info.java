module com.chatapp.yahoochatapp {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.chatapp.yahoochatapp to javafx.fxml;
    exports com.chatapp.yahoochatapp;
}