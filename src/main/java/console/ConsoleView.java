/**
 * Copyright (C) 2015 uphy.jp
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package console;

import com.google.gson.JsonObject;
import database.Database;
import database.Excel;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.util.Callback;
import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;


/**
 * @author Yuhi Ishikura
 */
public class ConsoleView extends BorderPane {

  private final PrintStream out;
  private final TextArea textArea;
  private final InputStream in;

  public static PrintStream standardOut;
  public static PrintStream consoleOut;

  public ConsoleView() {
    this(Charset.defaultCharset());
  }

  public ConsoleView(Charset charset) {
    getStyleClass().add("console");
    this.textArea = new TextArea();
    this.textArea.setWrapText(true);
    KeyBindingUtils.installEmacsKeyBinding(this.textArea);
    setCenter(this.textArea);

    final TextInputControlStream stream = new TextInputControlStream(this.textArea, Charset.defaultCharset());
    try {
      this.out = new PrintStream(stream.getOut(), true, charset.name());
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
    this.in = stream.getIn();

    final ContextMenu menu = new ContextMenu();
    menu.getItems().add(createItem("???????????????? ??????????????", e -> {
      try {
        stream.clear();
        this.textArea.clear();
      } catch (IOException e1) {
        throw new RuntimeException(e1);
      }
    }));
    menu.getItems().add(createItem("?????????? ??????????????", e -> {
      System.setOut(consoleOut);
    }));
    menu.getItems().add(createItem("?????????? ??????????????", e -> {
      System.setOut(standardOut);
    }));
    menu.getItems().add(createItem("?????????????????????? ??????????", e -> {
      JsonObject json = new JsonObject();
      json.addProperty("cmd","scan_nodes");
      EventBus.getDefault().post(json.toString());
    }));
    menu.getItems().add(createItem("???????????????????? ????????????????????????", e -> {
      JsonObject json = new JsonObject();
      json.addProperty("cmd","scan_stop");
      EventBus.getDefault().post(json.toString());
    }));
    menu.getItems().add(createItem("?????????????? ?? Excel", e -> {
      System.out.println("?????????????? ?? Excel");
      System.out.println(showDialogXls());
    }));
    menu.getItems().add(createItem("??????????", e -> {
      System.out.println("Exit");
      Platform.exit();
    }));
    this.textArea.setContextMenu(menu);

    setPrefWidth(600);
    setPrefHeight(400);
  }

  private MenuItem createItem(String name, EventHandler<ActionEvent> a) {
    final MenuItem menuItem = new MenuItem(name);
    menuItem.setOnAction(a);
    return menuItem;
  }

  public PrintStream getOut() {
    return out;
  }

  public InputStream getIn() {
    return in;
  }

  String showDialogXls(){
    Dialog<String> dialog = new Dialog<>();
    dialog.setTitle("?????????????? ????????????");
    dialog.setHeaderText("?????? ???????? ?????? ???????????? ?????????????? ???????????? ?????? ???????????????? ?? Excel.\n" +
            "?????????? ???????????? ?????????????? OK (?????? ?????????????? ?????????????? 'x').");
    dialog.setResizable(true);

    Label label1 = new Label("???????? ??: ");
    Label label2 = new Label("???????? ????: ");
    DatePicker from = new DatePicker();
    DatePicker to = new DatePicker();
    //TextField text1 = new TextField();
    //TextField text2 = new TextField();

    GridPane grid = new GridPane();
    grid.add(label1, 1, 1);
    grid.add(from, 2, 1);
    grid.add(label2, 1, 2);
    grid.add(to, 2, 2);
    dialog.getDialogPane().setContent(grid);

    ButtonType buttonTypeOk = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
    dialog.getDialogPane().getButtonTypes().add(buttonTypeOk);

    dialog.setResultConverter(new Callback<ButtonType, String>() {
      @Override
      public String call(ButtonType b) {

        if (b == buttonTypeOk) {
          DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
          LocalDate date = from.getValue();
          try {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(Database.DATE_FORMAT.parse(to.getValue().format(formatter) + " 00:00:00"));
            calendar.add(Calendar.DATE, 1);
            Excel.export(Database.DATE_FORMAT.parse(from.getValue().format(formatter) + " 00:00:00"), calendar.getTime());
          } catch (ParseException exception) {
            System.err.println("???????????? ???????????????? ????????: " + exception);
          }
          return "";//new PhoneBook(text1.getText(), text2.getText());
        }

        return null;
      }
    });

    Optional<String> result = dialog.showAndWait();
    return result.get();
  }



}
