package com.example.collabboard.util;

import com.example.collabboard.model.SessionData;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class SessionDataCellFactory extends ListCell<SessionData> {
    
    @Override
    protected void updateItem(SessionData session, boolean empty) {
        super.updateItem(session, empty);
        
        if (empty || session == null) {
            setText(null);
            setGraphic(null);
        } else {
            setText(null);
            setGraphic(createSessionCell(session));
        }
    }
    
    private HBox createSessionCell(SessionData session) {
        HBox cell = new HBox(15);
        cell.setAlignment(Pos.CENTER_LEFT);
        cell.setPadding(new Insets(10, 15, 10, 15));
        cell.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-radius: 8;");
        
        Circle colorIndicator = new Circle(8);
        try {
            colorIndicator.setFill(Color.web(session.getColorCode()));
        } catch (Exception e) {
            colorIndicator.setFill(Color.LIGHTGRAY);
        }
        
        VBox sessionInfo = new VBox(5);
        sessionInfo.setAlignment(Pos.CENTER_LEFT);
        
        Label titleLabel = new Label(session.getTitle());
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2C3E50;");
        
        Label detailsLabel = new Label(session.getLastActive() + " • " + session.getParticipantCount() + " participants");
        detailsLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7F8C8D;");
        
        sessionInfo.getChildren().addAll(titleLabel, detailsLabel);
        
        cell.getChildren().addAll(colorIndicator, sessionInfo);
        
        return cell;
    }
}
