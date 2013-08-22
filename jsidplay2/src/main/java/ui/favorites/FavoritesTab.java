package ui.favorites;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.WindowEvent;

import javax.persistence.metamodel.SingularAttribute;

import libpsid64.Psid64;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.PathUtils;
import libsidutils.STIL.STILEntry;
import ui.common.C64Tab;
import ui.entities.collection.HVSCEntry;
import ui.entities.collection.HVSCEntry_;
import ui.entities.config.Configuration;
import ui.entities.config.FavoriteColumn;
import ui.entities.config.FavoritesSection;
import ui.entities.config.SidPlay2Section;
import ui.events.IPlayTune;
import ui.events.favorites.IGetFavoritesTabs;
import ui.filefilter.FavoritesExtension;
import ui.filefilter.TuneFileFilter;
import ui.stil.STIL;

import com.sun.javafx.scene.control.skin.TableColumnHeader;

public class FavoritesTab extends C64Tab {

	@FXML
	private TextField filterField;
	@FXML
	private TableView<HVSCEntry> favoritesTable;
	@FXML
	private ContextMenu contextMenuHeader, contextMenu;
	@FXML
	private Menu addColumnMenu, moveToTab, copyToTab;
	@FXML
	private MenuItem showStil, removeColumn;
	@FXML
	private Button moveUp, moveDown;

	private ObservableList<HVSCEntry> allFavorites = FXCollections
			.<HVSCEntry> observableArrayList();

	private ObservableList<HVSCEntry> filteredFavorites = FXCollections
			.<HVSCEntry> observableArrayList();

	private final FileFilter tuneFilter = new TuneFileFilter();
	private FavoritesSection favoritesSection;

	private Object component;
	private File file;

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		if (getPlayer() == null) {
			// wait for second initialization, where properties have been set!
			return;
		}
		favoritesTable.setItems(filteredFavorites);
		favoritesTable.getSelectionModel().setSelectionMode(
				SelectionMode.MULTIPLE);
		favoritesTable.getColumns().addListener(
				new ListChangeListener<TableColumn<HVSCEntry, ?>>() {
					@Override
					public void onChanged(
							Change<? extends TableColumn<HVSCEntry, ?>> c) {
						moveColumn();
					}
				});
		favoritesTable.getSelectionModel().selectedIndexProperty()
				.addListener(new ChangeListener<Number>() {
					@Override
					public void changed(
							ObservableValue<? extends Number> observable,
							Number oldValue, Number newValue) {
						if (newValue != null && newValue.intValue() != -1) {
							// Save last selected row
							favoritesSection.setSelectedRowFrom(newValue
									.intValue());
							favoritesSection.setSelectedRowTo(newValue
									.intValue());
						}
						moveUp.setDisable(newValue == null
								|| newValue.intValue() == 0
								|| favoritesTable.getSortOrder().size() > 0);
						moveDown.setDisable(newValue == null
								|| newValue.intValue() == allFavorites.size() - 1
								|| favoritesTable.getSortOrder().size() > 0);
					}
				});
		favoritesTable.setOnMousePressed(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				final HVSCEntry hvscEntry = favoritesTable.getSelectionModel()
						.getSelectedItem();
				if (hvscEntry != null
						&& getFile(hvscEntry.getPath()) != null
						&& event.isPrimaryButtonDown()
						&& event.getClickCount() > 1) {
					playTune(hvscEntry);
				}
			}
		});
		favoritesTable.setOnKeyPressed(new EventHandler<KeyEvent>() {

			@Override
			public void handle(KeyEvent event) {
				final HVSCEntry hvscEntry = favoritesTable.getSelectionModel()
						.getSelectedItem();
				if (event.getCode() == KeyCode.ENTER && hvscEntry != null
						&& getFile(hvscEntry.getPath()) != null) {
					playTune(hvscEntry);
				}
				if (event.getCode() == KeyCode.DELETE) {
					removeSelectedFavorites();
				}
			}
		});
		// Initially select last selected row
		Integer from = favoritesSection.getSelectedRowFrom();
		if (from != null && from != -1) {
			favoritesTable.getSelectionModel().select(from);
		}

		filterField.setOnKeyReleased(new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				filter(filterField.getText());
			}

		});

		for (Field field : HVSCEntry_.class.getDeclaredFields()) {
			if (field.getName().equals(HVSCEntry_.id.getName())
					|| !(SingularAttribute.class.isAssignableFrom(field
							.getType()))) {
				continue;
			}
			try {
				SingularAttribute<?, ?> singleAttribute = (SingularAttribute<?, ?>) field
						.get(null);
				addAddColumnHeaderMenuItem(addColumnMenu, singleAttribute);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		contextMenuHeader.setOnShown(new EventHandler<WindowEvent>() {

			@SuppressWarnings("rawtypes")
			@Override
			public void handle(WindowEvent event) {
				TableColumn tableColumn = getContextMenuColumn();
				// never remove the first column
				removeColumn.setDisable(favoritesTable.getColumns().indexOf(
						tableColumn) == 0);
			}
		});

		contextMenu.setOnShown(new EventHandler<WindowEvent>() {

			@Override
			public void handle(WindowEvent event) {
				HVSCEntry hvscEntry = favoritesTable.getSelectionModel()
						.getSelectedItem();
				showStil.setDisable(hvscEntry == null
						|| getStilEntry(hvscEntry.getPath()) == null);
				getUiEvents().fireEvent(IGetFavoritesTabs.class,
						new IGetFavoritesTabs() {
							@Override
							public void setFavoriteTabs(
									List<FavoritesTab> tabs, String selected) {
								moveToTab.getItems().clear();
								copyToTab.getItems().clear();
								for (final FavoritesTab tab : tabs) {
									if (tab.equals(FavoritesTab.this)) {
										continue;
									}
									final String name = tab.getText();
									MenuItem moveToTabItem = new MenuItem(name);
									moveToTabItem
											.setOnAction(new EventHandler<ActionEvent>() {

												@Override
												public void handle(
														ActionEvent arg0) {
													ObservableList<HVSCEntry> selectedItems = favoritesTable
															.getSelectionModel()
															.getSelectedItems();
													copyToTab(selectedItems,
															tab);
													removeFavorites(selectedItems);
												}
											});
									moveToTab.getItems().add(moveToTabItem);
									MenuItem copyToTabItem = new MenuItem(name);
									copyToTabItem
											.setOnAction(new EventHandler<ActionEvent>() {

												@Override
												public void handle(
														ActionEvent arg0) {
													ObservableList<HVSCEntry> selectedItems = favoritesTable
															.getSelectionModel()
															.getSelectedItems();
													copyToTab(selectedItems,
															tab);
												}
											});
									copyToTab.getItems().add(copyToTabItem);
								}
								moveToTab.setDisable(moveToTab.getItems()
										.size() == 0);
								copyToTab.setDisable(copyToTab.getItems()
										.size() == 0);
							}
						});
			}
		});

		// XXX JavaFX: better initialization support using constructor
		// arguments?
		for (@SuppressWarnings("rawtypes")
		TableColumn column : favoritesTable.getColumns()) {
			FavoritesCellFactory cellFactory = (FavoritesCellFactory) column
					.getCellFactory();
			cellFactory.setFavoritesTab(this);
		}
	}

	@FXML
	private void doMoveUp() {
		int from = favoritesTable.getSelectionModel().getSelectedIndex();
		if (from == -1) {
			return;
		}
		moveRow(from, from - 1);
	}

	@FXML
	private void doMoveDown() {
		int from = favoritesTable.getSelectionModel().getSelectedIndex();
		if (from == -1) {
			return;
		}
		moveRow(from, from + 1);
	}

	@SuppressWarnings({ "rawtypes" })
	@FXML
	private void removeColumn() {
		TableColumn tableColumn = getContextMenuColumn();
		FavoriteColumn favoriteColumn = (FavoriteColumn) tableColumn
				.getUserData();
		favoritesTable.getColumns().remove(tableColumn);
		favoritesSection.getColumns().remove(favoriteColumn);
	}

	@FXML
	private void exportToDir() {
		final DirectoryChooser fileDialog = new DirectoryChooser();
		fileDialog.setInitialDirectory(((SidPlay2Section) (getConfig()
				.getSidplay2())).getLastDirectoryFile());
		File directory = fileDialog.showDialog(favoritesTable.getScene()
				.getWindow());
		if (directory != null) {
			getConfig().getSidplay2().setLastDirectory(
					directory.getAbsolutePath());
			for (HVSCEntry hvscEntry : favoritesTable.getSelectionModel()
					.getSelectedItems()) {
				File file = getFile(hvscEntry.getPath());
				try {
					PathUtils.copyFile(file,
							new File(directory, file.getName()));
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	@FXML
	private void showStil() {
		HVSCEntry hvscEntry = favoritesTable.getSelectionModel()
				.getSelectedItem();
		if (hvscEntry == null) {
			return;
		}
		STILEntry stilEntry = getStilEntry(hvscEntry.getPath());
		if (stilEntry != null) {
			STIL stil = new STIL();
			stil.setPlayer(getPlayer());
			stil.setConfig(getConfig());
			stil.setEntry(stilEntry);
			try {
				stil.open();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@FXML
	private void convertToPsid64() {
		final DirectoryChooser fileDialog = new DirectoryChooser();
		fileDialog.setInitialDirectory(((SidPlay2Section) (getConfig()
				.getSidplay2())).getLastDirectoryFile());
		File directory = fileDialog.showDialog(favoritesTable.getScene()
				.getWindow());
		if (directory != null) {
			getConfig().getSidplay2().setLastDirectory(
					directory.getAbsolutePath());
			final ArrayList<File> files = new ArrayList<File>();
			for (HVSCEntry hvscEntry : favoritesTable.getSelectionModel()
					.getSelectedItems()) {
				File file = getFile(hvscEntry.getPath());
				files.add(file);
			}
			Psid64 c = new Psid64(getConfig().getSidplay2()
					.getTmpDir());
			File hvscRoot = ((SidPlay2Section) getConfig().getSidplay2())
					.getHvscFile();
			c.convertFiles(hvscRoot, files.toArray(new File[0]), directory);
		}

	}

	public Object getComponent() {
		return component;
	}

	public void setComponent(Object component) {
		this.component = component;
	}

	public FavoritesSection getFavoritesSection() {
		return favoritesSection;
	}

	public void addFavorites(List<File> files) {
		for (int i = 0; files != null && i < files.size(); i++) {
			final File file = files.get(i);
			if (file.isDirectory()) {
				addFavorites(Arrays.asList(file.listFiles()));
			} else {
				if (tuneFilter.accept(file)) {
					addFavorite(file);
				}
			}
		}
	}

	void restoreColumns(FavoritesSection favoritesSection) {
		this.favoritesSection = favoritesSection;
		setText(favoritesSection.getName());
		allFavorites.addAll(favoritesSection.getFavorites());
		filteredFavorites.addAll(allFavorites);

		// Restore persisted columns
		for (FavoriteColumn favoriteColumn : favoritesSection.getColumns()) {
			try {
				String columnProperty = favoriteColumn.getColumnProperty();
				SingularAttribute<?, ?> attribute = getAttribute(columnProperty);
				addColumn(attribute, columnProperty, favoriteColumn);
			} catch (NoSuchFieldException | SecurityException
					| IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}

	void removeSelectedFavorites() {
		removeFavorites(favoritesTable.getSelectionModel().getSelectedItems());
	}

	void removeAllFavorites() {
		allFavorites.clear();
		filteredFavorites.clear();
		favoritesSection.getFavorites().clear();
		((Configuration) getConfig()).getFavorites().remove(favoritesSection);
	}

	void filter(String filterText) {
		filteredFavorites.clear();
		if (filterText.trim().length() == 0) {
			filteredFavorites.addAll(allFavorites);
		} else {
			outer: for (HVSCEntry hvscEntry : allFavorites) {
				for (TableColumn<HVSCEntry, ?> tableColumn : favoritesTable
						.getColumns()) {
					FavoriteColumn favoriteColumn = (FavoriteColumn) tableColumn
							.getUserData();
					String columnProperty = favoriteColumn != null ? favoriteColumn
							.getColumnProperty() : HVSCEntry_.path.getName();
					try {
						SingularAttribute<?, ?> singleAttribute = getAttribute(columnProperty);
						Object value = ((Field) singleAttribute.getJavaMember())
								.get(hvscEntry);
						String text = value != null ? value.toString() : "";
						if (text.contains(filterText)) {
							filteredFavorites.add(hvscEntry);
							continue outer;
						}
					} catch (IllegalArgumentException | IllegalAccessException
							| NoSuchFieldException | SecurityException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	void selectAllFavorites() {
		favoritesTable.getSelectionModel().selectAll();
	}

	void clearSelection() {
		favoritesTable.getSelectionModel().clearSelection();
	}

	void loadFavorites(File favoritesFile) throws IOException {
		favoritesFile = addFileExtension(favoritesFile);
		try (BufferedReader r = new BufferedReader(new InputStreamReader(
				new FileInputStream(favoritesFile), "ISO-8859-1"))) {
			String line;
			while ((line = r.readLine()) != null) {
				if (line.startsWith("<HVSC>/") || line.startsWith("<CGSC>/")) {
					// backward compatibility
					line = line.substring(7);
				}
				File file = getFile(line);
				if (file != null) {
					addFavorite(file);
				}
			}
		}
	}

	void saveFavorites(File favoritesFile) throws IOException {
		favoritesFile = addFileExtension(favoritesFile);
		try (PrintStream p = new PrintStream(favoritesFile)) {
			for (HVSCEntry hvscEntry : allFavorites) {
				p.println(hvscEntry.getPath());
			}
		}

	}

	void playNext(File file) {
		boolean recentlyPlayedFound = false;
		for (HVSCEntry hvscEntry : allFavorites) {
			if (recentlyPlayedFound) {
				playTune(hvscEntry);
				break;
			}
			File hvscFile = getFile(hvscEntry.getPath());
			if (hvscFile != null && hvscFile.equals(file)) {
				recentlyPlayedFound = true;
			}
		}
	}

	void playNextRandom() {
		HVSCEntry hvscEntry = allFavorites.get(Math.abs(new Random()
				.nextInt(Integer.MAX_VALUE)) % allFavorites.size());
		playTune(hvscEntry);
	}

	private File addFileExtension(File favoritesFile) {
		String extension = FavoritesExtension.EXTENSION;
		if (extension.startsWith("*")) {
			extension = extension.substring(1);
		}
		if (!favoritesFile.getName().endsWith(extension)) {
			favoritesFile = new File(favoritesFile.getName() + extension);
		}
		return favoritesFile;
	}

	private void addFavorite(File file) {
		SidTune sidTune;
		try {
			sidTune = SidTune.load(file);
			HVSCEntry entry = HVSCEntry.create(file, file.getAbsolutePath(),
					file, sidTune);
			allFavorites.add(entry);
			filteredFavorites.add(entry);
			favoritesSection.getFavorites().add(entry);
		} catch (IOException | SidTuneError e) {
			e.printStackTrace();
		}
	}

	private void removeFavorites(ObservableList<HVSCEntry> selectedItems) {
		allFavorites.removeAll(selectedItems);
		favoritesSection.getFavorites().removeAll(selectedItems);
		filter(filterField.getText());
	}

	private SingularAttribute<?, ?> getAttribute(String columnProperty)
			throws NoSuchFieldException, IllegalAccessException {
		Field field = HVSCEntry_.class.getDeclaredField(columnProperty);
		SingularAttribute<?, ?> singleAttribute = (SingularAttribute<?, ?>) field
				.get(null);
		return singleAttribute;
	}

	STILEntry getStilEntry(String path) {
		File file = getFile(path);
		if (file != null) {
			File hvscRoot = ((SidPlay2Section) getConfig().getSidplay2())
					.getHvscFile();
			STILEntry stilEntry = libsidutils.STIL.getSTIL(hvscRoot, file);
			return stilEntry;
		}
		return null;
	}

	/**
	 * @param config
	 * @param path
	 * @return file handle of the given entry
	 */
	File getFile(String path) {
		File file = new File(path);
		if (file.isAbsolute()) {
			// absolute path name?
			return file;
		}
		List<File> files;
		File hvscRoot = ((SidPlay2Section) getConfig().getSidplay2()).getHvscFile();
		files = PathUtils.getFiles(path, hvscRoot, null);
		if (files.size() > 0) {
			// relative path name of HVSC?
			return files.get(files.size() - 1);
		}
		File cgscRoot = ((SidPlay2Section) getConfig().getSidplay2()).getCgscFile();
		files = PathUtils.getFiles(path, cgscRoot, null);
		if (files.size() > 0) {
			// relative path name of CGSC?
			return files.get(files.size() - 1);
		}
		return null;
	}

	@SuppressWarnings({ "rawtypes" })
	private TableColumn getContextMenuColumn() {
		TableColumnHeader columnHeader = (TableColumnHeader) contextMenuHeader
				.getOwnerNode();
		return columnHeader.getTableColumn();
	}

	private void addAddColumnHeaderMenuItem(Menu addColumnMenu,
			final SingularAttribute<?, ?> attribute) {
		MenuItem menuItem = new MenuItem();
		menuItem.setText(attribute.getName());
		menuItem.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent arg0) {
				FavoriteColumn favoriteColumn = new FavoriteColumn();
				favoriteColumn.setColumnProperty(attribute.getName());
				favoritesSection.getColumns().add(favoriteColumn);
				addColumn(attribute, attribute.getName(), favoriteColumn);
			}
		});
		addColumnMenu.getItems().add(menuItem);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void addColumn(SingularAttribute<?, ?> attribute,
			String columnProperty, FavoriteColumn favoriteColumn) {
		String text = getBundle().getString(
				attribute.getDeclaringType().getJavaType().getSimpleName()
						+ "." + columnProperty);
		TableColumn tableColumn = new TableColumn();
		tableColumn.setUserData(favoriteColumn);
		tableColumn.setText(text);
		tableColumn
				.setCellValueFactory(new PropertyValueFactory(columnProperty));
		FavoritesCellFactory favoritesCellFactory = new FavoritesCellFactory();
		favoritesCellFactory.setFavoritesTab(this);
		tableColumn.setCellFactory(favoritesCellFactory);
		tableColumn.setContextMenu(contextMenuHeader);
		favoritesTable.getColumns().add(tableColumn);
	}

	private void moveColumn() {
		Collection<FavoriteColumn> newOrderList = new ArrayList<FavoriteColumn>();
		for (TableColumn<HVSCEntry, ?> tableColumn : favoritesTable
				.getColumns()) {
			FavoriteColumn favoriteColumn = (FavoriteColumn) tableColumn
					.getUserData();
			if (favoriteColumn != null) {
				newOrderList.add(favoriteColumn);
			}
		}
		favoritesSection.getColumns().clear();
		favoritesSection.getColumns().addAll(newOrderList);
	}

	private void moveRow(int from, int to) {
		Collections.swap(allFavorites, from, to);
		Collections.swap(favoritesSection.getFavorites(), from, to);
		filter(filterField.getText());
		favoritesTable.getSelectionModel().select(to);
	}

	private void copyToTab(final List<HVSCEntry> toCopy, final FavoritesTab tab) {
		for (HVSCEntry hvscEntry : toCopy) {
			tab.addFavorite(getFile(hvscEntry.getPath()));
		}
	}

	private void playTune(final HVSCEntry hvscEntry) {
		file = getFile(hvscEntry.getPath());
		if (file!=null) {
			getUiEvents().fireEvent(IPlayTune.class, new IPlayTune() {
				@Override
				public boolean switchToVideoTab() {
					return false;
				}
				
				@Override
				public Object getComponent() {
					return component;
				}
				
				@Override
				public String getCommand() {
					return null;
				}
				
				@Override
				public SidTune getSidTune() {
					try {
						return SidTune.load(file);
					} catch (IOException | SidTuneError e) {
						e.printStackTrace();
						return null;
					}
				}
			});
		}
	}

	public File getCurrentlyPlayedFile() {
		return file;
	}

}
