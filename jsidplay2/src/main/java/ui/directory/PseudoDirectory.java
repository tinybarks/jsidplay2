package ui.directory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Locale;

import libsidplay.config.IConfig;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.PathUtils;
import libsidutils.directory.CartridgeDirectory;
import libsidutils.directory.DirEntry;
import libsidutils.directory.Directory;
import libsidutils.directory.DiskDirectory;
import libsidutils.directory.T64Directory;
import sidplay.Player;
import ui.entities.collection.HVSCEntry;
import ui.entities.config.SidPlay2Section;
import ui.filefilter.DiskFileFilter;
import ui.filefilter.TuneFileFilter;

public class PseudoDirectory {

	/**
	 * Char-set for string to byte conversions.
	 */
	private static final Charset ISO88591 = Charset.forName("ISO-8859-1");
	/**
	 * Detect SID tunes for preview.
	 */
	private static TuneFileFilter tuneFilter = new TuneFileFilter();
	private static DiskFileFilter diskFilter = new DiskFileFilter();

	/**
	 * Create a directory preview of the currently selected file
	 * 
	 * @param file
	 *            file to create a directory preview for
	 * @return directory preview
	 * @throws IOException
	 *             can not open file
	 */
	public static final Directory getDirectory(Player player, final File file, final IConfig cfg) throws IOException {
		if (diskFilter.accept(file)) {
			return DiskDirectory.getDirectory(file);
		} else if (file.getName().toLowerCase(Locale.ENGLISH).endsWith(".t64")) {
			return T64Directory.getDirectory(file);
		} else if (file.getName().toLowerCase(Locale.ENGLISH).endsWith(".crt")) {
			return CartridgeDirectory.getDirectory(file);
		} else if (tuneFilter.accept(file)) {
			return getTuneAsDirectory(player, file, cfg);
		}
		return null;
	}

	private static Directory getTuneAsDirectory(Player player, File file, IConfig cfg) throws IOException {
		Directory dir = new Directory();
		SidTune tune;
		try {
			tune = SidTune.load(file);
		} catch (SidTuneError e) {
			throw new IOException();
		}
		SidPlay2Section sidPlay2Section = (SidPlay2Section) player.getConfig().getSidplay2Section();
		String collectionName = PathUtils.getCollectionName(sidPlay2Section.getHvscFile(), file);
		HVSCEntry entry = new HVSCEntry(() -> player.getSidDatabaseInfo(db -> db.getTuneLength(tune), 0),
				collectionName, file, tune);
		final String title = entry.getTitle() != null ? entry.getTitle() : entry.getName();

		// Directory title: tune title or filename
		dir.setTitle(DirEntry.asciiTopetscii(title, 16));
		// Directory id: start song '/' song count
		dir.setId((String.valueOf(entry.getStartSong()) + "/" + String.valueOf(Math.max(1, entry.getNoOfSongs())))
				.getBytes(ISO88591));
		Collection<DirEntry> entries = dir.getDirEntries();

		addProperty(entries, "TITLE", entry.getTitle());
		addProperty(entries, "AUTHOR", entry.getAuthor());
		addProperty(entries, "RELEASED", entry.getReleased());
		addProperty(entries, "FORMAT", entry.getFormat());
		addProperty(entries, "PLAYERID", entry.getPlayerId());
		addProperty(entries, "NO_SONGS", String.valueOf(entry.getNoOfSongs()));
		addProperty(entries, "STARTSONG", String.valueOf(entry.getStartSong()));
		addProperty(entries, "CLOCKFREQ", String.valueOf(entry.getClockFreq()));
		addProperty(entries, "SPEED", String.valueOf(entry.getSpeed()));
		addProperty(entries, "SIDMODEL1", String.valueOf(entry.getSidModel1()));
		addProperty(entries, "SIDMODEL2", String.valueOf(entry.getSidModel2()));
		addProperty(entries, "SIDMODEL3", String.valueOf(entry.getSidModel3()));
		addProperty(entries, "COMPAT", String.valueOf(entry.getCompatibility()));
		addProperty(entries, "TUNE_LGTH", String.valueOf(entry.getTuneLength()));
		addProperty(entries, "AUDIO", entry.getAudio());
		addProperty(entries, "CHIP_BASE1", String.valueOf(entry.getSidChipBase1()));
		addProperty(entries, "CHIP_BASE2", String.valueOf(entry.getSidChipBase2()));
		addProperty(entries, "CHIP_BASE3", String.valueOf(entry.getSidChipBase3()));
		addProperty(entries, "DRV_ADDR", String.valueOf(entry.getDriverAddress()));
		addProperty(entries, "LOAD_ADDR", String.valueOf(entry.getLoadAddress()));
		addProperty(entries, "LOAD_LGTH", String.valueOf(entry.getLoadLength()));
		addProperty(entries, "INIT_ADDR", String.valueOf(entry.getInitAddress()));
		addProperty(entries, "PLY_ADDR", String.valueOf(entry.getPlayerAddress()));
		addProperty(entries, "FILE_DATE", String.valueOf(entry.getFileDate()));
		addProperty(entries, "SIZE_KB", String.valueOf(entry.getFileSizeKb()));
		addProperty(entries, "SIZE_B", String.valueOf(entry.getTuneSizeB()));
		addProperty(entries, "RELOC_PAGE", String.valueOf(entry.getRelocStartPage()));
		addProperty(entries, "RELOC_PAGES", String.valueOf(entry.getRelocNoPages()));

		return dir;
	}

	private static void addProperty(Collection<DirEntry> entries, String property, String value) {
		byte[] filename = DirEntry.asciiTopetscii(property + "=" + value, 20);
		// Pseudo directory entry: tune property '=' value
		entries.add(new DirEntry(0, filename, (byte) -1) {
			@Override
			public void save(File autostartFile) throws IOException {
			}
		});
	}

}
