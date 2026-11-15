package com.chaflow.client;

import com.chaflow.client.controller.AuthController;
import com.chaflow.client.controller.MessengerController;
import com.chaflow.client.controller.ChatController;
import com.chaflow.client.service.SocketService;
import com.chaflow.client.service.AuthService;
import com.chaflow.client.service.ChatService;
import com.chaflow.client.view.AuthView;
import com.chaflow.client.view.LoginView;
import com.chaflow.client.view.RegisterView;
import com.chaflow.client.view.ChatView;
import com.chaflow.client.view.MessengerView;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChaFlowApp extends Application {
    private final SocketService socket=new SocketService();
    private final AuthController authController=new AuthController(new AuthService(socket));
    private final MessengerController messengerController=new MessengerController(new ChatService(socket));
    private final ChatController chatController=new ChatController(new ChatService(socket));
    private ChatView chatView;
    private final ExecutorService io=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"chaflow-network");t.setDaemon(true);return t;});
    private Stage stage; private SocketClient.AuthResult account; private long selectedConversation=-1;
    private SocketClient.EventConnection eventConnection;
    private VBox messageList; private TextField messageInput; private Label chatTitle; private VBox conversationItems; private VBox searchResults; private VBox requestItems; private VBox friendItems; private Label status;

    @Override public void start(Stage stage){this.stage=stage;showAuth();stage.show();}
    private void showAuth(){
        LoginView login=new LoginView((button,credentials)->authenticate(button,()->authController.login(credentials.phone(),credentials.password())));
        RegisterView register=new RegisterView((button,data)->authenticate(button,()->authController.register(data.phone(),data.username(),data.displayName(),data.password())),message->status.setText(message));
        AuthView view=new AuthView(login,register);
        status=view.status();
        stage.setTitle("ChaFlow — Sign in");stage.setScene(new Scene(view.root(),520,570));
    }
    private void authenticate(Button button,java.util.function.Supplier<SocketClient.AuthResult> call){button.setDisable(true);status.setText("Connecting…");io.submit(()->{SocketClient.AuthResult r=call.get();Platform.runLater(()->{button.setDisable(false);if(r.successful()){account=r;showMessenger();}else status.setText(r.message());});});}

    private void showMessenger(){
        BorderPane root=new BorderPane();root.setStyle("-fx-background-color:#f4f7f9;");
        VBox sidebar=new VBox(12);sidebar.setPadding(new Insets(18));sidebar.setPrefWidth(330);sidebar.setStyle("-fx-background-color:white;-fx-border-color:#e5e7eb;-fx-border-width:0 1 0 0;");
        HBox top=new HBox(8);Label brand=new Label("ChaFlow");brand.setFont(Font.font(23));brand.setStyle("-fx-font-weight:bold;-fx-text-fill:#16a085;");Region spacer=new Region();HBox.setHgrow(spacer,Priority.ALWAYS);Button logout=new Button("Sign out");logout.setOnAction(e->{SocketClient.AuthResult previous=account;selectedConversation=-1;closeEventConnection();io.submit(()->{authController.logout(previous.token());Platform.runLater(()->{account=null;showAuth();});});});top.getChildren().addAll(brand,spacer,logout);
        Label who=new Label(account.displayName()+"  ·  @"+account.username());who.setStyle("-fx-text-fill:#64748b;");
        TextField search=field("Find by username or phone number");Button find=primary("Search accounts");find.setOnAction(e->search(search.getText()));search.setOnAction(e->search(search.getText()));
        Label searchHeading=section("People");searchResults=new VBox(6);ScrollPane peopleScroll=new ScrollPane(searchResults);peopleScroll.setFitToWidth(true);peopleScroll.setPrefViewportHeight(145);peopleScroll.setStyle("-fx-background:white;");
        Label convHeading=section("Conversations");conversationItems=new VBox(5);ScrollPane convScroll=new ScrollPane(conversationItems);convScroll.setFitToWidth(true);VBox.setVgrow(convScroll,Priority.ALWAYS);
        Label friendHeading=section("Friends");friendItems=new VBox(5);ScrollPane friendScroll=new ScrollPane(friendItems);friendScroll.setFitToWidth(true);friendScroll.setPrefViewportHeight(110);
        Label reqHeading=section("Friend requests");requestItems=new VBox(5);ScrollPane reqScroll=new ScrollPane(requestItems);reqScroll.setFitToWidth(true);reqScroll.setPrefViewportHeight(105);
        sidebar.getChildren().addAll(top,who,new Separator(),search,find,searchHeading,peopleScroll,friendHeading,friendScroll,convHeading,convScroll,reqHeading,reqScroll);root.setLeft(sidebar);
        chatView=new ChatView(this::sendMessage);chatTitle=chatView.title();messageList=chatView.messages();messageInput=chatView.input();
        status=new Label("Ready");
        MessengerView messengerView=new MessengerView(sidebar,chatView,status);
        stage.setTitle("ChaFlow — Messages");stage.setScene(new Scene(messengerView.root(),1120,760));refreshAll();startEventConnection();
    }
    private void startEventConnection(){
        closeEventConnection();
        if(account==null)return;
        String token=account.token();
        eventConnection=chatController.listenEvents(token,
                this::handleServerEvent,
                error->Platform.runLater(()->{
                    if(account!=null && account.token().equals(token))
                        status.setText("Live connection lost: "+(error.getMessage()==null?"network error":error.getMessage()));
                }));
    }

    private void handleServerEvent(Map<String,Object> event){
        if("CONNECTED".equals(text(event,"event"))){
            Platform.runLater(()->{if(account!=null){status.setText("Live connection established.");refreshAll();}});
            return;
        }
        if(!"MESSAGE_RECEIVED".equals(text(event,"event")))return;
        Object rawData=event.get("data");
        if(!(rawData instanceof Map<?,?> data))return;
        Object rawConversation=data.get("conversationId");
        long conversationId=rawConversation instanceof Number n?n.longValue():-1;
        Platform.runLater(()->{
            if(account==null)return;
            status.setText("New message received.");
            refreshAll();
            if(selectedConversation==conversationId)loadMessages();
        });
    }

    private void closeEventConnection(){
        SocketClient.EventConnection current=eventConnection;
        eventConnection=null;
        if(current!=null)current.close();
    }

    private void search(String q){if(q==null||q.trim().length()<2){status.setText("Enter at least 2 characters.");return;}run(()->{List<Map<String,Object>> found=messengerController.search(account.token(),q);Platform.runLater(()->{searchResults.getChildren().clear();if(found.isEmpty())searchResults.getChildren().add(new Label("No ChatFlow account found."));for(Map<String,Object> u:found)searchResults.getChildren().add(personRow(u));status.setText(found.size()+" account(s) found.");});});}
    private HBox personRow(Map<String,Object> u){Label name=new Label(text(u,"displayName")+"  @"+text(u,"username"));name.setStyle("-fx-font-weight:bold;");Label phone=new Label(text(u,"phone"));phone.setStyle("-fx-text-fill:#64748b;-fx-font-size:11px;");VBox info=new VBox(2,name,phone);HBox.setHgrow(info,Priority.ALWAYS);Button chat=new Button("Message");chat.setOnAction(e->run(()->{long id=chatController.openDirect(account.token(),number(u,"id"));Platform.runLater(()->openConversation(id,text(u,"displayName")));}));Button add=new Button(Boolean.TRUE.equals(u.get("isFriend"))?"Friends":"+ Friend");add.setDisable(Boolean.TRUE.equals(u.get("isFriend"))||u.get("pendingStatus")!=null);add.setOnAction(e->run(()->{Map<String,Object> r=messengerController.friendRequest(account.token(),number(u,"id"));Platform.runLater(()->status.setText(text(r,"message")));refreshAll();}));HBox row=new HBox(7,info,chat,add);row.setAlignment(Pos.CENTER_LEFT);row.setPadding(new Insets(7));row.setStyle("-fx-background-color:#f8fafc;-fx-background-radius:8;");return row;}
    private void refreshAll(){if(account==null)return;run(()->{List<Map<String,Object>> conv=messengerController.conversations(account.token());List<Map<String,Object>> req=messengerController.friendRequests(account.token());List<Map<String,Object>> friends=messengerController.friends(account.token());Platform.runLater(()->{friendItems.getChildren().clear();if(friends.isEmpty())friendItems.getChildren().add(new Label("No friends yet."));for(Map<String,Object> f:friends){Label n=new Label(text(f,"displayName")+"  @"+text(f,"username"));Button msg=new Button("Message"),remove=new Button("Remove");msg.setOnAction(e->run(()->{long cid=chatController.openDirect(account.token(),number(f,"id"));Platform.runLater(()->openConversation(cid,text(f,"displayName")));}));remove.setOnAction(e->run(()->{messengerController.removeFriend(account.token(),number(f,"id"));refreshAll();}));friendItems.getChildren().add(new HBox(5,n,msg,remove));}conversationItems.getChildren().clear();for(Map<String,Object> c:conv){String unread=number(c,"unreadCount")>0?"  ·  "+number(c,"unreadCount")+" unread":"";Button b=new Button(text(c,"displayName").isBlank()?"Conversation #"+c.get("id")+unread:text(c,"displayName")+unread+"\n"+Objects.toString(c.get("lastMessage"),"Start a conversation"));b.setMaxWidth(Double.MAX_VALUE);b.setAlignment(Pos.CENTER_LEFT);b.setStyle("-fx-background-color:#f8fafc;-fx-padding:10;-fx-background-radius:8;");long id=number(c,"id");b.setOnAction(e->openConversation(id,text(c,"displayName")));conversationItems.getChildren().add(b);}requestItems.getChildren().clear();if(req.isEmpty())requestItems.getChildren().add(new Label("No pending requests."));for(Map<String,Object> r:req){Label l=new Label(text(r,"displayName")+" @"+text(r,"username"));Button yes=new Button("Accept"),no=new Button("Decline");yes.setOnAction(e->respond(r,true));no.setOnAction(e->respond(r,false));HBox row=new HBox(5,l,yes,no);row.setAlignment(Pos.CENTER_LEFT);requestItems.getChildren().add(row);}});});}
    private void respond(Map<String,Object> r,boolean accept){run(()->{messengerController.respond(account.token(),number(r,"requestId"),accept);refreshAll();});}
    private void openConversation(long id,String name){selectedConversation=id;chatTitle.setText(name==null||name.isBlank()?"Conversation #"+id:name);loadMessages();}
    private void loadMessages(){if(selectedConversation<0)return;long id=selectedConversation;run(()->{List<Map<String,Object>> rows=chatController.messages(account.token(),id);Platform.runLater(()->{if(selectedConversation!=id)return;messageList.getChildren().clear();for(Map<String,Object> m:rows){boolean mine=number(m,"senderId")==account.userId();Label body=new Label(text(m,"content"));body.setWrapText(true);body.setMaxWidth(480);body.setPadding(new Insets(9,12,9,12));body.setStyle(mine?"-fx-background-color:#d1fae5;-fx-background-radius:12;":"-fx-background-color:white;-fx-background-radius:12;");Label by=new Label(mine?"You":text(m,"displayName")+" · @"+text(m,"username"));by.setStyle("-fx-font-size:10px;-fx-text-fill:#64748b;");VBox bubble=new VBox(3,by,body);HBox line=new HBox(bubble);line.setAlignment(mine?Pos.CENTER_RIGHT:Pos.CENTER_LEFT);messageList.getChildren().add(line);}});});}
    private void sendMessage(){String body=messageInput.getText().trim();if(selectedConversation<0||body.isEmpty())return;long id=selectedConversation;run(()->{chatController.sendMessage(account.token(),id,body);Platform.runLater(()->{if(messageInput!=null&&messageInput.getText().trim().equals(body))messageInput.clear();});loadMessages();refreshAll();});}
    private void run(Throwing action){io.submit(()->{try{action.run();}catch(Exception e){Platform.runLater(()->status.setText(e.getMessage()==null?"Request failed.":e.getMessage()));}});}
    @FunctionalInterface private interface Throwing{void run()throws Exception;}
    private static String text(Map<String,Object> m,String k){return Objects.toString(m.get(k),"");}private static long number(Map<String,Object>m,String k){Object o=m.get(k);return o instanceof Number n?n.longValue():-1;}
    private static TextField field(String prompt){TextField f=new TextField();f.setPromptText(prompt);return f;}private static Label section(String s){Label l=new Label(s);l.setStyle("-fx-font-weight:bold;-fx-text-fill:#334155;");return l;}
    private static VBox labelled(String name,Control c){Label l=new Label(name);l.setStyle("-fx-font-size:12px;-fx-font-weight:bold;");return new VBox(5,l,c);}private static Button primary(String s){Button b=new Button(s);b.setStyle("-fx-background-color:#16a085;-fx-text-fill:white;-fx-font-weight:bold;-fx-padding:9 15 9 15;");return b;}
    @Override public void stop(){
        closeEventConnection();
        SocketClient.AuthResult previous=account;
        if(previous!=null){
            Thread logoutThread=new Thread(()->authController.logout(previous.token()),"chaflow-shutdown-logout");
            logoutThread.setDaemon(true);
            logoutThread.start();
        }
        io.shutdownNow();
    }
    public static void main(String[] args){launch(args);}
}
