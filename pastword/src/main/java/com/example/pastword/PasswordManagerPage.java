package com.example.pastword;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PasswordManagerPage extends StackPane {

    private static final int PIN_LENGTH = 5;
    private static final double OVERLAY_LABEL_WIDTH = 132;
    private static final double OVERLAY_INPUT_WIDTH = 340;

    private final PastwordApp app;
    private final UserAccount account;
    private final VBox folderListBox = new VBox(10);
    private final Label viewTitle = new Label();
    private final TextField folderSearchField = new TextField();
    private final TextField searchField = new TextField();
    private final StackPane overlayLayer = new StackPane();
    private final Label toastLabel = new Label();
    private final PauseTransition toastTimer = new PauseTransition(Duration.seconds(3));
    private final ObservableList<EntryRow> tableItems = FXCollections.observableArrayList();
    private final TableView<EntryRow> passwordTable = new TableView<>(tableItems);
    private final TableColumn<EntryRow, Boolean> selectColumn = new TableColumn<>("Select");
    private final Button multiSelectButton = new Button("Multi-select");
    private final Button selectAllButton = new Button("Select all");
    private final Button deleteSelectedButton = new Button("Delete Selected");
    private final boolean requirePinOnOpen;

    private String selectedFolder;
    private SearchOptions searchOptions = SearchOptions.defaults();
    private boolean multiSelectMode;

    public PasswordManagerPage(PastwordApp app, UserAccount account, boolean requirePinOnOpen) {
        this.app = app;
        this.account = account;
        this.requirePinOnOpen = requirePinOnOpen;

        getStyleClass().add("manager-root");

        BorderPane shell = new BorderPane();
        shell.getStyleClass().add("manager-shell");
        shell.setLeft(createSidebar());
        shell.setCenter(createContent());

        overlayLayer.getStyleClass().add("overlay-layer");
        overlayLayer.setVisible(false);
        overlayLayer.setManaged(false);

        toastLabel.getStyleClass().add("manager-toast");
        toastLabel.setVisible(false);
        toastLabel.setManaged(false);
        StackPane.setAlignment(toastLabel, Pos.TOP_CENTER);
        StackPane.setMargin(toastLabel, new Insets(18, 0, 0, 0));

        toastTimer.setOnFinished(event -> {
            toastLabel.setVisible(false);
            toastLabel.setManaged(false);
        });

        getChildren().addAll(createDecorationLayer(), shell, overlayLayer, toastLabel);

        configureTable();
        refreshFolderList();
        refreshPasswordTable();

        if (requirePinOnOpen) {
            Platform.runLater(this::showPinOverlay);
        }
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox(16);
        sidebar.getStyleClass().add("sidebar-panel");
        sidebar.setPadding(new Insets(18, 16, 18, 16));
        sidebar.prefWidthProperty().bind(widthProperty().multiply(0.2));
        sidebar.setMinWidth(170);
        sidebar.setMaxWidth(250);

        Label logo = new Label("PASTWORD");
        logo.getStyleClass().add("sidebar-logo");

        Button createFolderButton = createSidebarButton("+ Create Folder");
        createFolderButton.setOnAction(event -> showFolderEditor(null));

        Button homeButton = createSidebarButton("Home");
        homeButton.setOnAction(event -> {
            selectedFolder = null;
            refreshFolderList();
            refreshPasswordTable();
        });

        folderSearchField.getStyleClass().add("sidebar-search");
        folderSearchField.setPromptText("Search folders");
        folderSearchField.textProperty().addListener((observable, oldValue, newValue) -> refreshFolderList());

        ScrollPane folderScroll = new ScrollPane(folderListBox);
        folderScroll.setFitToWidth(true);
        folderScroll.getStyleClass().add("transparent-scroll");
        VBox.setVgrow(folderScroll, Priority.ALWAYS);

        VBox accountBox = new VBox(4);
        accountBox.getStyleClass().add("account-box");
        accountBox.setSpacing(10);

        StackPane avatar = AvatarViewFactory.createAvatar(account, 42, "sidebar-avatar");

        Label accountName = new Label(account.getUsername());
        accountName.getStyleClass().add("account-name");
        Label accountEmail = new Label(account.getEmail());
        accountEmail.getStyleClass().add("account-email");

        VBox accountDetails = new VBox(2, accountName, accountEmail);
        accountDetails.setAlignment(Pos.CENTER_LEFT);

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        Button settingsButton = new Button("\u2699");
        settingsButton.getStyleClass().addAll("mini-action", "settings-icon-button");
        settingsButton.setOnAction(event -> app.showSettingsPage());

        HBox accountHeader = new HBox(10, avatar, accountDetails, footerSpacer, settingsButton);
        accountHeader.setAlignment(Pos.CENTER_LEFT);

        Button logoutButton = new Button("Logout");
        logoutButton.getStyleClass().add("mini-action");
        logoutButton.setOnAction(event -> app.logout());

        accountBox.getChildren().addAll(accountHeader, logoutButton);

        sidebar.getChildren().addAll(logo, createFolderButton, homeButton, folderSearchField, folderScroll, accountBox);
        return sidebar;
    }

    private VBox createContent() {
        VBox content = new VBox(16);
        content.getStyleClass().add("content-panel");
        content.setPadding(new Insets(18, 22, 18, 22));

        Label headerLogo = new Label("PASTWORD");
        headerLogo.getStyleClass().add("content-logo");
        headerLogo.setMaxWidth(Double.MAX_VALUE);
        headerLogo.setAlignment(Pos.CENTER);

        HBox titleRow = new HBox(12);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        viewTitle.getStyleClass().add("view-title");

        Region titleSpacer = new Region();
        HBox.setHgrow(titleSpacer, Priority.ALWAYS);

        searchField.getStyleClass().add("toolbar-search");
        searchField.setPromptText("Search passwords");
        searchField.setPrefWidth(270);
        searchField.textProperty().addListener((observable, oldValue, newValue) ->
                updateSearchOptions(newValue, searchOptions.getFolderFilter(), searchOptions.getSortBy(), searchOptions.isAscending()));

        multiSelectButton.getStyleClass().addAll("icon-button", "toolbar-button");
        multiSelectButton.setOnAction(event -> toggleMultiSelectMode());

        selectAllButton.getStyleClass().addAll("icon-button", "toolbar-button");
        selectAllButton.setVisible(false);
        selectAllButton.setManaged(false);
        selectAllButton.setDisable(true);
        selectAllButton.setOnAction(event -> toggleSelectAllRows());

        deleteSelectedButton.getStyleClass().addAll("icon-button", "toolbar-button", "toolbar-danger");
        deleteSelectedButton.setVisible(false);
        deleteSelectedButton.setManaged(false);
        deleteSelectedButton.setDisable(true);
        deleteSelectedButton.setOnAction(event -> showDeleteSelectedPanel());

        Button filterButton = new Button("Filter");
        filterButton.getStyleClass().addAll("icon-button", "toolbar-button");
        filterButton.setOnAction(event -> showFilterPanel());

        titleRow.getChildren().addAll(viewTitle, searchField, titleSpacer, multiSelectButton, selectAllButton, deleteSelectedButton, filterButton);

        VBox.setVgrow(passwordTable, Priority.ALWAYS);

        Button addButton = new Button("+ Add");
        addButton.getStyleClass().add("add-shortcut");
        addButton.setOnAction(event -> showEntryEditor(null));

        HBox addRow = new HBox(addButton);
        addRow.setAlignment(Pos.CENTER);

        content.getChildren().addAll(headerLogo, titleRow, passwordTable, addRow);
        return content;
    }

    private void configureTable() {
        passwordTable.getStyleClass().add("password-table");
        passwordTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        passwordTable.setPlaceholder(createTablePlaceholder("No passwords saved for this view yet."));
        passwordTable.setFocusTraversable(false);
        passwordTable.setEditable(true);

        selectColumn.setCellValueFactory(cellData -> cellData.getValue().selectedProperty());
        selectColumn.setCellFactory(CheckBoxTableCell.forTableColumn(selectColumn));
        selectColumn.setEditable(true);
        selectColumn.setSortable(false);
        selectColumn.setReorderable(false);
        selectColumn.setResizable(false);
        selectColumn.setVisible(false);
        selectColumn.prefWidthProperty().bind(passwordTable.widthProperty().multiply(0.09));
        selectColumn.setStyle("-fx-alignment: CENTER;");

        TableColumn<EntryRow, String> nameColumn = createTextColumn("Name", row -> row.getEntry().getTitle(), 0.29);
        TableColumn<EntryRow, String> usernameColumn = createTextColumn("Username", row -> row.getEntry().getUsername(), 0.24);
        TableColumn<EntryRow, String> passwordColumn = createTextColumn("Password", row -> mask(row.getEntry().getPassword()), 0.18);

        TableColumn<EntryRow, EntryRow> actionsColumn = new TableColumn<>("Controls");
        actionsColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));
        actionsColumn.setCellFactory(column -> new TableCell<>() {
            private final Button viewButton = createTableActionButton("View");
            private final Button editButton = createTableActionButton("Edit");
            private final Button deleteButton = createTableActionButton("Delete");
            private final HBox controls = new HBox(8, viewButton, editButton, deleteButton);

            {
                controls.setAlignment(Pos.CENTER);
                viewButton.setOnAction(event -> {
                    EntryRow row = getItem();
                    if (row != null) {
                        showDetailsPanel(new EntryContext(row.getFolderName(), row.getEntry()));
                    }
                });
                editButton.setOnAction(event -> {
                    EntryRow row = getItem();
                    if (row != null) {
                        showEntryEditor(new EntryContext(row.getFolderName(), row.getEntry()));
                    }
                });
                deleteButton.setOnAction(event -> {
                    EntryRow row = getItem();
                    if (row != null) {
                        showDeleteEntryPanel(new EntryContext(row.getFolderName(), row.getEntry()));
                    }
                });
            }

            @Override
            protected void updateItem(EntryRow item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER);
                setGraphic(empty || item == null ? null : controls);
            }
        });
        actionsColumn.setSortable(false);
        actionsColumn.setReorderable(false);
        actionsColumn.setStyle("-fx-alignment: CENTER;");
        actionsColumn.prefWidthProperty().bind(passwordTable.widthProperty().multiply(0.20));

        passwordTable.getColumns().setAll(selectColumn, nameColumn, usernameColumn, passwordColumn, actionsColumn);
    }

    private TableColumn<EntryRow, String> createTextColumn(String title, java.util.function.Function<EntryRow, String> mapper, double widthFactor) {
        TableColumn<EntryRow, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(mapper.apply(cellData.getValue())));
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER);
                setText(empty || item == null ? null : item);
            }
        });
        column.setReorderable(false);
        column.setStyle("-fx-alignment: CENTER;");
        column.prefWidthProperty().bind(passwordTable.widthProperty().multiply(widthFactor));
        return column;
    }

    private Button createTableActionButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().addAll("icon-button", "table-action-button");
        return button;
    }

    private Label createTablePlaceholder(String text) {
        Label placeholder = new Label(text);
        placeholder.getStyleClass().add("empty-state");
        return placeholder;
    }

    private Button createSidebarButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("sidebar-button");
        button.setMaxWidth(Double.MAX_VALUE);
        return button;
    }

    private void refreshFolderList() {
        folderListBox.getChildren().clear();

        if (account.getFolders().isEmpty()) {
            folderListBox.getChildren().add(createTablePlaceholder("No folders yet.\nCreate one to organize passwords."));
            return;
        }

        String query = folderSearchField.getText().trim().toLowerCase();
        List<String> visibleFolders = account.getFolders().stream()
                .filter(folderName -> query.isBlank() || folderName.toLowerCase().contains(query))
                .toList();

        if (visibleFolders.isEmpty()) {
            folderListBox.getChildren().add(createTablePlaceholder("No folders match your search."));
            return;
        }

        for (String folderName : visibleFolders) {
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);

            Button folderButton = new Button(folderName);
            folderButton.getStyleClass().add("folder-button");
            if (folderName.equals(selectedFolder)) {
                folderButton.getStyleClass().add("folder-button-active");
            }
            folderButton.setMaxWidth(Double.MAX_VALUE);
            folderButton.setOnAction(event -> {
                selectedFolder = folderName;
                refreshFolderList();
                refreshPasswordTable();
            });

            Button menuButton = new Button("...");
            menuButton.getStyleClass().add("mini-action");
            menuButton.setOnAction(event -> showFolderEditor(folderName));

            HBox.setHgrow(folderButton, Priority.ALWAYS);
            row.getChildren().addAll(folderButton, menuButton);
            folderListBox.getChildren().add(row);
        }
    }

    private void refreshPasswordTable() {
        viewTitle.setText(selectedFolder == null ? "Home" : selectedFolder);
        List<EntryRow> rows = new ArrayList<>();
        for (EntryContext context : collectVisibleEntries()) {
            rows.add(new EntryRow(context.folderName, context.entry));
        }

        tableItems.setAll(rows);
        updateDeleteSelectedState();

        String placeholderText = selectedFolder == null
                ? "No passwords saved yet. Use + Add to create one."
                : "This folder is empty. Use + Add to populate it.";
        passwordTable.setPlaceholder(createTablePlaceholder(rows.isEmpty() ? placeholderText : "No passwords saved for this view yet."));
    }

    private void updateSearchOptions(String query, String folderFilter, SearchOptions.SortBy sortBy, boolean ascending) {
        searchOptions = new SearchOptions(query, folderFilter, sortBy, ascending);
        refreshPasswordTable();
    }

    private List<EntryContext> collectVisibleEntries() {
        List<EntryContext> entries = new ArrayList<>();
        List<String> foldersToRead = new ArrayList<>();

        if (selectedFolder == null) {
            foldersToRead.addAll(account.getFolders());
        } else {
            foldersToRead.add(selectedFolder);
        }

        for (String folderName : foldersToRead) {
            for (PasswordEntry entry : account.getEntriesForFolder(folderName)) {
                entries.add(new EntryContext(folderName, entry));
            }
        }

        String searchFolder = searchOptions.getFolderFilter();
        String query = searchOptions.getQuery().trim().toLowerCase();
        entries.removeIf(context -> !"All folders".equals(searchFolder) && !context.folderName.equals(searchFolder));
        entries.removeIf(context -> !query.isBlank() && !(context.entry.getTitle().toLowerCase().contains(query)
                || context.entry.getUsername().toLowerCase().contains(query)
                || context.entry.getNotes().toLowerCase().contains(query)
                || context.folderName.toLowerCase().contains(query)));

        Comparator<EntryContext> comparator;
        if (searchOptions.getSortBy() == SearchOptions.SortBy.USERNAME) {
            comparator = Comparator.comparing(context -> context.entry.getUsername().toLowerCase());
        } else if (searchOptions.getSortBy() == SearchOptions.SortBy.FOLDER) {
            comparator = Comparator.comparing(context -> context.folderName.toLowerCase());
        } else {
            comparator = Comparator.comparing(context -> context.entry.getTitle().toLowerCase());
        }

        if (!searchOptions.isAscending()) {
            comparator = comparator.reversed();
        }
        entries.sort(comparator);
        return entries;
    }

    private void toggleMultiSelectMode() {
        multiSelectMode = !multiSelectMode;
        selectColumn.setVisible(multiSelectMode);
        selectAllButton.setVisible(multiSelectMode);
        selectAllButton.setManaged(multiSelectMode);
        deleteSelectedButton.setVisible(multiSelectMode);
        deleteSelectedButton.setManaged(multiSelectMode);
        multiSelectButton.setText(multiSelectMode ? "Done" : "Multi-select");

        if (!multiSelectMode) {
            clearSelectedRows();
        }
        updateDeleteSelectedState();
    }

    private void toggleSelectAllRows() {
        boolean shouldSelectAll = tableItems.stream().anyMatch(row -> !row.isSelected());
        for (EntryRow row : tableItems) {
            row.setSelected(shouldSelectAll);
        }
        updateDeleteSelectedState();
    }

    private void clearSelectedRows() {
        for (EntryRow row : tableItems) {
            row.setSelected(false);
        }
    }

    private void updateDeleteSelectedState() {
        long selectedCount = tableItems.stream().filter(EntryRow::isSelected).count();
        boolean hasSelection = selectedCount > 0;
        boolean allSelected = !tableItems.isEmpty() && selectedCount == tableItems.size();
        deleteSelectedButton.setDisable(!hasSelection);
        selectAllButton.setDisable(tableItems.isEmpty());
        selectAllButton.setText(allSelected ? "Clear all" : "Select all");
    }

    private void showFolderEditor(String folderName) {
        VBox card = createOverlayCard(folderName == null ? "CREATE FOLDER" : "FOLDER OPTIONS", 430);
        TextField folderField = createOverlayTextField(folderName == null ? "" : folderName);
        folderField.setPromptText("Folder name");
        Label errorLabel = createOverlayErrorLabel();

        Button cancelButton = createOverlayButton("Cancel", "secondary");
        cancelButton.setOnAction(event -> hideOverlay());

        HBox actions = createCenteredActions();

        if (folderName == null) {
            Button saveButton = createOverlayButton("Create", "success");
            saveButton.setOnAction(event -> {
                String trimmed = folderField.getText().trim();
                if (trimmed.isEmpty()) {
                    errorLabel.setText("Folder name is required.");
                    return;
                }
                if (account.hasFolder(trimmed)) {
                    errorLabel.setText("That folder already exists.");
                    return;
                }
                account.addFolder(trimmed);
                selectedFolder = trimmed;
                app.persistCurrentUser();
                refreshFolderList();
                refreshPasswordTable();
                hideOverlay();
                showToast("Folder created.");
            });
            actions.getChildren().addAll(cancelButton, saveButton);
        } else {
            Button saveButton = createOverlayButton("Save", "success");
            saveButton.setOnAction(event -> {
                String trimmed = folderField.getText().trim();
                if (trimmed.isEmpty()) {
                    errorLabel.setText("Folder name is required.");
                    return;
                }
                if (!trimmed.equals(folderName) && account.hasFolder(trimmed)) {
                    errorLabel.setText("That folder already exists.");
                    return;
                }
                account.renameFolder(folderName, trimmed);
                if (folderName.equals(selectedFolder)) {
                    selectedFolder = trimmed;
                }
                app.persistCurrentUser();
                refreshFolderList();
                refreshPasswordTable();
                hideOverlay();
                showToast("Folder updated.");
            });

            Button deleteButton = createOverlayButton("Delete", "danger");
            deleteButton.setOnAction(event -> showDeleteFolderPanel(folderName));
            actions.getChildren().addAll(cancelButton, saveButton, deleteButton);
        }

        card.getChildren().addAll(folderField, errorLabel, actions);
        showOverlay(card);
    }

    private void showDeleteFolderPanel(String folderName) {
        VBox card = createOverlayCard("DELETE FOLDER", 500);
        Label message = new Label("Delete folder \"" + folderName + "\" and all passwords inside it?");
        message.getStyleClass().add("overlay-message");
        message.setWrapText(true);
        message.setMaxWidth(430);

        HBox actions = createCenteredActions();
        Button backButton = createOverlayButton("Back", "secondary");
        backButton.setOnAction(event -> showFolderEditor(folderName));
        Button deleteButton = createOverlayButton("Delete", "danger");
        deleteButton.setOnAction(event -> {
            account.deleteFolder(folderName);
            if (folderName.equals(selectedFolder)) {
                selectedFolder = null;
            }
            app.persistCurrentUser();
            refreshFolderList();
            refreshPasswordTable();
            hideOverlay();
            showToast("Folder deleted.");
        });
        actions.getChildren().addAll(backButton, deleteButton);

        card.getChildren().addAll(message, actions);
        showOverlay(card);
    }

    private void showEntryEditor(EntryContext context) {
        if (context == null && account.getFolders().isEmpty()) {
            showToast("Create a folder first before adding passwords.");
            showFolderEditor(null);
            return;
        }

        VBox card = createOverlayCard(context == null ? "ADD PASSWORD" : "EDIT PASSWORD", 620);

        TextField titleField = createOverlayTextField(context == null ? "" : context.entry.getTitle());
        TextField usernameField = createOverlayTextField(context == null ? "" : context.entry.getUsername());
        TextField passwordField = createOverlayTextField(context == null ? "" : context.entry.getPassword());
        TextArea notesArea = new TextArea(context == null ? "" : context.entry.getNotes());
        notesArea.getStyleClass().add("overlay-textarea");
        notesArea.setPrefRowCount(3);
        notesArea.setPrefWidth(OVERLAY_INPUT_WIDTH);
        notesArea.setMaxWidth(OVERLAY_INPUT_WIDTH);

        ComboBox<String> folderChoice = new ComboBox<>();
        folderChoice.getItems().addAll(account.getFolders());
        if (context != null) {
            folderChoice.setValue(context.folderName);
        } else if (selectedFolder != null && account.hasFolder(selectedFolder)) {
            folderChoice.setValue(selectedFolder);
        }
        folderChoice.getStyleClass().add("overlay-combo");
        folderChoice.setPrefWidth(OVERLAY_INPUT_WIDTH);
        folderChoice.setMaxWidth(OVERLAY_INPUT_WIDTH);

        GridPane form = createOverlayForm();
        form.add(createFormLabel("App/Website Name:"), 0, 0);
        form.add(titleField, 1, 0);
        form.add(createFormLabel("Username:"), 0, 1);
        form.add(usernameField, 1, 1);
        form.add(createFormLabel("Password:"), 0, 2);
        form.add(passwordField, 1, 2);
        form.add(createFormLabel("Folder:"), 0, 3);
        form.add(folderChoice, 1, 3);
        form.add(createFormLabel("Notes:"), 0, 4);
        form.add(notesArea, 1, 4);

        Label errorLabel = createOverlayErrorLabel();
        HBox actions = createCenteredActions();
        Button cancelButton = createOverlayButton("Cancel", "secondary");
        cancelButton.setOnAction(event -> hideOverlay());
        Button submitButton = createOverlayButton(context == null ? "Submit" : "Save", "success");
        submitButton.setOnAction(event -> {
            String title = titleField.getText().trim();
            String username = usernameField.getText().trim();
            String password = passwordField.getText().trim();
            String folderName = folderChoice.getValue();

            if (title.isEmpty() || username.isEmpty() || password.isEmpty() || folderName == null) {
                errorLabel.setText("Please complete every required field.");
                return;
            }

            if (context != null) {
                account.getEntriesForFolder(context.folderName).removeIf(entry -> entry.getId().equals(context.entry.getId()));
            }

            PasswordEntry entry = context == null
                    ? new PasswordEntry(title, username, password, notesArea.getText().trim())
                    : new PasswordEntry(context.entry.getId(), title, username, password, notesArea.getText().trim());

            account.getEntriesForFolder(folderName).add(entry);
            selectedFolder = folderName;
            app.persistCurrentUser();
            refreshFolderList();
            refreshPasswordTable();
            hideOverlay();
            showToast(context == null ? "Password added." : "Password updated.");
        });
        actions.getChildren().addAll(cancelButton, submitButton);

        card.getChildren().addAll(form, errorLabel, actions);
        showOverlay(card);
    }

    private void showDetailsPanel(EntryContext context) {
        VBox card = createOverlayCard("PASSWORD DETAILS", 620);
        GridPane grid = createOverlayForm();
        grid.getStyleClass().add("details-grid");
        addDetailRow(grid, 0, "App/Website Name:", context.entry.getTitle());
        addDetailRow(grid, 1, "Username:", context.entry.getUsername());
        addDetailRow(grid, 2, "Password:", context.entry.getPassword());
        addDetailRow(grid, 3, "Folder:", context.folderName);
        addDetailRow(grid, 4, "Notes:", context.entry.getNotes().isBlank() ? "-" : context.entry.getNotes());

        HBox actions = createCenteredActions();
        Button backButton = createOverlayButton("Back", "secondary");
        backButton.setOnAction(event -> hideOverlay());
        Button editButton = createOverlayButton("Edit", "primary");
        editButton.setOnAction(event -> showEntryEditor(context));
        Button deleteButton = createOverlayButton("Delete", "danger");
        deleteButton.setOnAction(event -> showDeleteEntryPanel(context));
        actions.getChildren().addAll(backButton, editButton, deleteButton);

        card.getChildren().addAll(grid, actions);
        showOverlay(card);
    }

    private void showDeleteEntryPanel(EntryContext context) {
        VBox card = createOverlayCard("DELETE CONFIRMATION", 500);
        Label message = new Label("Are you sure you want to delete \"" + context.entry.getTitle() + "\"?");
        message.getStyleClass().add("overlay-message");
        message.setWrapText(true);
        message.setMaxWidth(430);

        HBox actions = createCenteredActions();
        Button backButton = createOverlayButton("Back", "secondary");
        backButton.setOnAction(event -> hideOverlay());
        Button deleteButton = createOverlayButton("Delete", "danger");
        deleteButton.setOnAction(event -> {
            account.getEntriesForFolder(context.folderName).removeIf(entry -> entry.getId().equals(context.entry.getId()));
            app.persistCurrentUser();
            refreshPasswordTable();
            hideOverlay();
            showToast("Password deleted.");
        });
        actions.getChildren().addAll(backButton, deleteButton);

        card.getChildren().addAll(message, actions);
        showOverlay(card);
    }

    private void showDeleteSelectedPanel() {
        List<EntryRow> selectedRows = tableItems.stream().filter(EntryRow::isSelected).toList();
        if (selectedRows.isEmpty()) {
            return;
        }

        VBox card = createOverlayCard("DELETE SELECTED", 500);
        Label message = new Label("Delete " + selectedRows.size() + " selected password" + (selectedRows.size() > 1 ? "s" : "") + "?");
        message.getStyleClass().add("overlay-message");
        message.setWrapText(true);
        message.setMaxWidth(430);

        HBox actions = createCenteredActions();
        Button backButton = createOverlayButton("Back", "secondary");
        backButton.setOnAction(event -> hideOverlay());
        Button deleteButton = createOverlayButton("Delete", "danger");
        deleteButton.setOnAction(event -> {
            for (EntryRow row : selectedRows) {
                account.getEntriesForFolder(row.getFolderName()).removeIf(entry -> entry.getId().equals(row.getEntry().getId()));
            }
            app.persistCurrentUser();
            hideOverlay();
            clearSelectedRows();
            refreshPasswordTable();
            showToast("Selected passwords deleted.");
        });
        actions.getChildren().addAll(backButton, deleteButton);

        card.getChildren().addAll(message, actions);
        showOverlay(card);
    }

    private void showFilterPanel() {
        VBox card = createOverlayCard("FILTER", 520);
        ComboBox<String> folderChoice = new ComboBox<>();
        folderChoice.getItems().add("All folders");
        folderChoice.getItems().addAll(account.getFolders());
        folderChoice.setValue(searchOptions.getFolderFilter());
        folderChoice.getStyleClass().add("overlay-combo");
        folderChoice.setPrefWidth(OVERLAY_INPUT_WIDTH);
        folderChoice.setMaxWidth(OVERLAY_INPUT_WIDTH);

        ComboBox<SearchOptions.SortBy> sortChoice = new ComboBox<>();
        sortChoice.getItems().addAll(SearchOptions.SortBy.values());
        sortChoice.setValue(searchOptions.getSortBy());
        sortChoice.getStyleClass().add("overlay-combo");
        sortChoice.setPrefWidth(OVERLAY_INPUT_WIDTH);
        sortChoice.setMaxWidth(OVERLAY_INPUT_WIDTH);

        ComboBox<String> directionChoice = new ComboBox<>();
        directionChoice.getItems().addAll("Ascending", "Descending");
        directionChoice.setValue(searchOptions.isAscending() ? "Ascending" : "Descending");
        directionChoice.getStyleClass().add("overlay-combo");
        directionChoice.setPrefWidth(OVERLAY_INPUT_WIDTH);
        directionChoice.setMaxWidth(OVERLAY_INPUT_WIDTH);

        GridPane form = createOverlayForm();
        form.add(createFormLabel("Folder:"), 0, 0);
        form.add(folderChoice, 1, 0);
        form.add(createFormLabel("Sort By:"), 0, 1);
        form.add(sortChoice, 1, 1);
        form.add(createFormLabel("Direction:"), 0, 2);
        form.add(directionChoice, 1, 2);

        HBox actions = createCenteredActions();
        Button backButton = createOverlayButton("Back", "secondary");
        backButton.setOnAction(event -> hideOverlay());
        Button resetButton = createOverlayButton("Reset", "primary");
        resetButton.setOnAction(event -> {
            updateSearchOptions(searchField.getText(), "All folders", SearchOptions.SortBy.NAME, true);
            hideOverlay();
        });
        Button applyButton = createOverlayButton("Apply", "success");
        applyButton.setOnAction(event -> {
            updateSearchOptions(
                    searchField.getText(),
                    folderChoice.getValue(),
                    sortChoice.getValue(),
                    "Ascending".equals(directionChoice.getValue())
            );
            hideOverlay();
        });
        actions.getChildren().addAll(backButton, resetButton, applyButton);

        card.getChildren().addAll(form, actions);
        showOverlay(card);
    }

    private GridPane createOverlayForm() {
        GridPane form = new GridPane();
        form.getStyleClass().add("overlay-form");
        form.setAlignment(Pos.CENTER);
        form.setHgap(14);
        form.setVgap(12);

        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setMinWidth(OVERLAY_LABEL_WIDTH);
        labelColumn.setPrefWidth(OVERLAY_LABEL_WIDTH);
        labelColumn.setHalignment(HPos.RIGHT);

        ColumnConstraints inputColumn = new ColumnConstraints();
        inputColumn.setMinWidth(OVERLAY_INPUT_WIDTH);
        inputColumn.setPrefWidth(OVERLAY_INPUT_WIDTH);
        inputColumn.setHgrow(Priority.ALWAYS);

        form.getColumnConstraints().setAll(labelColumn, inputColumn);
        return form;
    }

    private HBox createCenteredActions() {
        HBox actions = new HBox(14);
        actions.setAlignment(Pos.CENTER);
        return actions;
    }

    private VBox createOverlayCard(String title, double maxWidth) {
        VBox card = new VBox(22);
        card.getStyleClass().add("overlay-card");
        card.setAlignment(Pos.CENTER);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        card.setPrefHeight(Region.USE_COMPUTED_SIZE);
        card.setMaxWidth(maxWidth);
        card.setPrefWidth(maxWidth);
        card.setMinWidth(360);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("overlay-title");
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        titleLabel.setAlignment(Pos.CENTER);
        card.getChildren().add(titleLabel);
        return card;
    }

    private TextField createOverlayTextField(String value) {
        TextField field = new TextField(value);
        field.getStyleClass().add("overlay-textfield");
        field.setPrefWidth(OVERLAY_INPUT_WIDTH);
        field.setMaxWidth(OVERLAY_INPUT_WIDTH);
        return field;
    }

    private Label createOverlayErrorLabel() {
        Label label = new Label();
        label.getStyleClass().add("overlay-error");
        label.setWrapText(true);
        label.setMaxWidth(OVERLAY_INPUT_WIDTH + OVERLAY_LABEL_WIDTH);
        label.setVisible(false);
        label.managedProperty().bind(label.visibleProperty());
        label.textProperty().addListener((observable, oldValue, newValue) ->
                label.setVisible(newValue != null && !newValue.isBlank()));
        return label;
    }

    private Label createFormLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("overlay-form-label");
        return label;
    }

    private Button createOverlayButton(String text, String variant) {
        Button button = new Button(text);
        button.getStyleClass().addAll("overlay-button", "overlay-button-" + variant);
        return button;
    }

    private void addDetailRow(GridPane grid, int rowIndex, String labelText, String valueText) {
        Label label = createFormLabel(labelText);
        Label value = new Label(valueText);
        value.getStyleClass().add("detail-value");
        value.setWrapText(true);
        value.setMaxWidth(OVERLAY_INPUT_WIDTH);
        grid.add(label, 0, rowIndex);
        grid.add(value, 1, rowIndex);
    }

    private void showPinOverlay() {
        VBox card = createOverlayCard("Pin-Code", 460);
        card.getStyleClass().add("pin-overlay-card");

        HBox pinRow = new HBox(12);
        pinRow.setAlignment(Pos.CENTER);
        List<TextField> pinFields = createPinFields(PIN_LENGTH, pinRow);

        Label errorLabel = createOverlayErrorLabel();
        errorLabel.setAlignment(Pos.CENTER);
        errorLabel.setMaxWidth(280);
        for (TextField field : pinFields) {
            field.textProperty().addListener((observable, oldValue, newValue) -> errorLabel.setText(""));
        }

        HBox actions = createCenteredActions();
        Button submitButton = createOverlayButton("Submit", "primary");
        submitButton.setOnAction(event -> {
            attemptPinUnlock(pinFields, errorLabel);
        });
        actions.getChildren().add(submitButton);
        pinFields.get(PIN_LENGTH - 1).textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.isBlank() && readPin(pinFields).length() == PIN_LENGTH) {
                Platform.runLater(submitButton::fire);
            }
        });

        card.getChildren().addAll(pinRow, errorLabel, actions);
        showOverlay(card, false);
        Platform.runLater(() -> pinFields.get(0).requestFocus());
    }

    private void attemptPinUnlock(List<TextField> pinFields, Label errorLabel) {
        String pin = readPin(pinFields);
        if (pin.length() != PIN_LENGTH) {
            errorLabel.setText("Enter your 5-digit pin code.");
            return;
        }
        if (!app.verifyCurrentUserPin(pin)) {
            errorLabel.setText("Incorrect pin. Try again.");
            clearPinFields(pinFields);
            Platform.runLater(() -> pinFields.get(0).requestFocus());
            return;
        }

        hideOverlay();
        showToast("Vault unlocked.");
    }

    private List<TextField> createPinFields(int count, HBox container) {
        List<TextField> fields = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            final int currentIndex = index;
            TextField pinField = new TextField();
            pinField.getStyleClass().add("pin-box");
            pinField.setPrefSize(48, 48);
            pinField.setMaxSize(48, 48);
            pinField.setAlignment(Pos.CENTER);
            pinField.textProperty().addListener((observable, oldValue, newValue) -> {
                String sanitized = newValue.replaceAll("[^0-9]", "");
                if (sanitized.length() > 1) {
                    sanitized = sanitized.substring(0, 1);
                }
                if (!sanitized.equals(newValue)) {
                    pinField.setText(sanitized);
                    return;
                }
                if (!sanitized.isEmpty() && !sanitized.equals(oldValue) && currentIndex < count - 1) {
                    TextField nextField = fields.get(currentIndex + 1);
                    nextField.requestFocus();
                    nextField.selectAll();
                }
            });
            pinField.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.BACK_SPACE && pinField.getText().isEmpty() && currentIndex > 0) {
                    TextField previousField = fields.get(currentIndex - 1);
                    previousField.requestFocus();
                    previousField.end();
                } else if (event.getCode() == KeyCode.LEFT && currentIndex > 0) {
                    fields.get(currentIndex - 1).requestFocus();
                } else if (event.getCode() == KeyCode.RIGHT && currentIndex < count - 1) {
                    fields.get(currentIndex + 1).requestFocus();
                }
            });
            pinField.focusedProperty().addListener((observable, oldValue, focused) -> {
                if (focused) {
                    pinField.selectAll();
                }
            });
            fields.add(pinField);
            container.getChildren().add(pinField);
        }
        return fields;
    }

    private void clearPinFields(List<TextField> fields) {
        for (TextField field : fields) {
            field.clear();
        }
    }

    private String readPin(List<TextField> fields) {
        StringBuilder builder = new StringBuilder();
        for (TextField field : fields) {
            builder.append(field.getText().trim());
        }
        return builder.toString();
    }

    private void showOverlay(VBox card) {
        showOverlay(card, true);
    }

    private void showOverlay(VBox card, boolean dismissible) {
        StackPane container = new StackPane(card);
        container.getStyleClass().add("overlay-backdrop");
        if (dismissible) {
            container.setOnMouseClicked(event -> {
                if (event.getTarget() == container) {
                    hideOverlay();
                }
            });
        }
        overlayLayer.getChildren().setAll(container);
        overlayLayer.setVisible(true);
        overlayLayer.setManaged(true);
    }

    private void hideOverlay() {
        overlayLayer.getChildren().clear();
        overlayLayer.setVisible(false);
        overlayLayer.setManaged(false);
    }

    private void showToast(String message) {
        toastTimer.stop();
        toastLabel.setText(message);
        toastLabel.setVisible(true);
        toastLabel.setManaged(true);
        toastTimer.playFromStart();
    }

    private String mask(String password) {
        return "*".repeat(Math.max(password.length(), 6));
    }

    private Pane createDecorationLayer() {
        Pane pane = new Pane();
        pane.setMouseTransparent(true);
        pane.getChildren().addAll(
                createCircuit(24, 92, false),
                createCircuit(870, 82, true),
                createCircuit(44, 640, false),
                createCircuit(960, 610, true)
        );
        return pane;
    }

    private Pane createCircuit(double startX, double startY, boolean mirrored) {
        Pane group = new Pane();
        group.setLayoutX(startX);
        group.setLayoutY(startY);
        group.setOpacity(0.18);
        group.getChildren().addAll(
                createBranch(0, 0, 78, 0, 100, -36),
                createBranch(0, 0, 102, 0, 126, 30),
                createBranch(0, 0, 84, 0, 104, 64)
        );
        if (mirrored) {
            group.setScaleX(-1);
        }
        return group;
    }

    private Pane createBranch(double x1, double y1, double x2, double y2, double x3, double y3) {
        Pane branch = new Pane();
        Line first = new Line(x1, y1, x2, y2);
        Line second = new Line(x2, y2, x3, y3);
        Circle joint = new Circle(x2, y2, 5);
        Circle end = new Circle(x3, y3, 5);
        first.getStyleClass().add("circuit-line");
        second.getStyleClass().add("circuit-line");
        joint.getStyleClass().add("circuit-node");
        end.getStyleClass().add("circuit-node");
        branch.getChildren().addAll(first, second, joint, end);
        return branch;
    }

    private static final class EntryContext {
        private final String folderName;
        private final PasswordEntry entry;

        private EntryContext(String folderName, PasswordEntry entry) {
            this.folderName = folderName;
            this.entry = entry;
        }
    }

    private final class EntryRow {
        private final String folderName;
        private final PasswordEntry entry;
        private final BooleanProperty selected = new SimpleBooleanProperty(false);

        private EntryRow(String folderName, PasswordEntry entry) {
            this.folderName = folderName;
            this.entry = entry;
            selected.addListener((observable, oldValue, newValue) -> updateDeleteSelectedState());
        }

        public String getFolderName() {
            return folderName;
        }

        public PasswordEntry getEntry() {
            return entry;
        }

        public BooleanProperty selectedProperty() {
            return selected;
        }

        public boolean isSelected() {
            return selected.get();
        }

        public void setSelected(boolean value) {
            selected.set(value);
        }
    }
}
