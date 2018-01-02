package chat;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;

public class JMSChat extends Application {
    Connection connection;
    Session sessionSend;
    MessageProducer producer;
    private String codeUser;
    private String host;
    private int port;

    public static void main(String[] args) {
        Application.launch(JMSChat.class);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("JMS Client Gui");
        BorderPane root = new BorderPane();
        HBox hBox = new HBox();
        Label labelCode = new Label("Code:");
        labelCode.setPadding(new Insets(5));
        TextField textFieldCode = new TextField("C1");
        textFieldCode.setPromptText("Code");
        Label labelHost = new Label("Host:");
        labelHost.setPadding(new Insets(5));
        TextField textFieldHost = new TextField("localhost");
        textFieldHost.setPromptText("Host");
        Label labelPort = new Label("Port");
        labelHost.setPadding(new Insets(5));
        TextField textFieldPort = new TextField("61616");
        textFieldHost.setPromptText("Port");
        Button buttonConnect = new Button("Connect");
        hBox.getChildren().add(labelCode);
        hBox.getChildren().add(textFieldCode);
        hBox.getChildren().add(labelHost);
        hBox.getChildren().add(textFieldHost);
        hBox.getChildren().add(labelPort);
        hBox.getChildren().add(textFieldPort);
        hBox.getChildren().add(buttonConnect);
        hBox.setPadding(new Insets(10));
        hBox.setSpacing(10);
        hBox.setBackground(new Background(new BackgroundFill(Color.ORANGE, CornerRadii.EMPTY,
                Insets.EMPTY)));
        root.setTop(hBox);
        GridPane gridPane = new GridPane();
        gridPane.setPadding(new Insets(10));
        gridPane.setVgap(10);
        gridPane.setHgap(10);
        Label labelTo = new Label("To:");
        TextField textFieldTo = new TextField("C1");
        Label labelMessage = new Label("Message");
        Button buttonEnvoyer = new Button("Envoyer");
        TextArea textAreaMessage = new TextArea();
        textAreaMessage.setPrefWidth(300);
        textAreaMessage.setPrefColumnCount(3);
        Label labelImages = new Label("Image:");
        File f = new File("images");
        ObservableList<String> list = FXCollections.observableArrayList(f.list());
        ComboBox<String> comboBoxImages = new ComboBox<>(list);
        comboBoxImages.getSelectionModel().select(0);
        File fileImage = new
                File("images/" + comboBoxImages.getSelectionModel().getSelectedItem());
        Image image = new Image(fileImage.toURI().toString());
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(320);
        imageView.setFitHeight(240);
        Button buttonEnvoyerImage = new Button("Envoyer Image");
        gridPane.add(labelTo, 0, 0);
        gridPane.add(textFieldTo, 1, 0);
        gridPane.add(labelMessage, 0, 1);
        gridPane.add(textAreaMessage, 1, 1);
        gridPane.add(buttonEnvoyer, 2, 1);
        gridPane.add(labelImages, 0, 3);
        gridPane.add(comboBoxImages, 1, 3);
        gridPane.add(buttonEnvoyerImage, 2, 3);
        VBox vBox = new VBox();
        vBox.setPadding(new Insets(10));
        vBox.getChildren().add(gridPane);
        final ObservableList<String> observableList = FXCollections.observableArrayList();
        ListView<String> lisViewMessages = new ListView<>(observableList);
        HBox hBox2 = new HBox();
        hBox2.setPadding(new Insets(10));
        hBox2.setSpacing(10);
        hBox2.getChildren().add(lisViewMessages);
        hBox2.getChildren().add(imageView);
        vBox.getChildren().add(hBox2);
        root.setCenter(vBox);
        Scene scene = new Scene(root, 800, 400);
        primaryStage.setScene(scene);
        primaryStage.show();

        buttonConnect.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    codeUser = textFieldCode.getText();
                    host = textFieldHost.getText();
                    port = Integer.parseInt(textFieldPort.getText());
                    ConnectionFactory connectionFactory = new
                            ActiveMQConnectionFactory("tcp://" + host + ":" + port);
                    connection = connectionFactory.createConnection();
                    connection.start();
                    Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                    Destination destination = session.createQueue("DQueue3");
                    MessageConsumer consumer = session.createConsumer(destination, "code='" + codeUser + "'");
                    sessionSend = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                    producer = sessionSend.createProducer(destination);
                    producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
                    consumer.setMessageListener(new MessageListener() {
                        @Override
                        public void onMessage(Message message) {
                            try {
                                if (message instanceof TextMessage) {
                                    TextMessage textMessage = (TextMessage) message;
                                    String from = textMessage.getStringProperty("from");
                                    String mess = textMessage.getText();
                                    observableList.add("From:" + from + "===>" + mess);
                                } else if (message instanceof StreamMessage) {
                                    StreamMessage streamMessage = (StreamMessage) message;
                                    String nomPhoto = streamMessage.readString();
                                    int size = streamMessage.readInt();
                                    byte[] data = new byte[size];
                                    streamMessage.readBytes(data);
                                    Image image = new Image(new ByteArrayInputStream(data));
                                    imageView.setImage(image);
                                    observableList.add("Réception de la photo " + nomPhoto);
                                }
                            } catch (JMSException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
                hBox.setDisable(true);
            }
        });
        buttonEnvoyer.setOnAction(e -> {
            try {
                TextMessage message = sessionSend.createTextMessage();
                message.setText(textAreaMessage.getText());
                message.setStringProperty("code", textFieldTo.getText());
                message.setStringProperty("from", codeUser);
                producer.send(message);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        });
        comboBoxImages.getSelectionModel().selectedItemProperty()
                .addListener(new ChangeListener<String>() {
                    @Override
                    public void changed(ObservableValue<? extends String> observable, String oldValue, String
                            newValue) {
                        File f2 = new File("images/" + newValue);
                        Image image = new Image(f2.toURI().toString());
                        imageView.setImage(image);
                    }
                });
        buttonEnvoyerImage.setOnAction(e->{
            try {
                StreamMessage streamMessage=sessionSend.createStreamMessage();
                streamMessage.setStringProperty("code", textFieldTo.getText());
                streamMessage.setStringProperty("from",codeUser );
                String imageName=comboBoxImages.getSelectionModel().getSelectedItem();
                File f3=new File("images/"+imageName);
                FileInputStream fis=new FileInputStream(f3);
                byte[] data=new byte[(int)f3.length()];
                fis.read(data);
                streamMessage.writeString(imageName);
                streamMessage.writeInt(data.length);
                streamMessage.writeBytes(data);
                producer.send(streamMessage);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        });
    }
}
