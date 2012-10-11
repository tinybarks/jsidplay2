package applet.emulationsettings;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import libsidplay.C64;
import libsidplay.Player;
import libsidplay.common.SIDEmu;

import org.swixml.SwingEngine;
import org.swixml.XDialog;

import resid_builder.ReSID;
import resid_builder.resid.ISIDDefs.ChipModel;
import resid_builder.resid.SID;
import sidplay.ConsolePlayer;
import sidplay.ini.intf.IConfig;
import sidplay.ini.intf.IFilterSection;
import applet.events.IChangeFilter;
import applet.events.IReplayTune;
import applet.events.UIEvent;
import applet.events.UIEventFactory;
import applet.events.UIEventListener;
import applet.ui.DKnob2;

public class EmulationSettings extends XDialog implements ChangeListener,
		UIEventListener {

	protected UIEventFactory uiEvents = UIEventFactory.getInstance();
	private SwingEngine swix;

	protected DKnob2 leftVolume, rightVolume;
	protected JComboBox sid1Model, sid2Model, filter;
	protected JTextField baseAddress;
	protected JCheckBox forceStereo, boosted8580;
	protected CurveFit filterCurve;

	protected ConsolePlayer consolePl;
	protected Player player;
	protected IConfig config;

	public Action setSid1Model = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			if (sid1Model.getSelectedIndex() == 0) {
				config.getEmulation().setUserSidModel(null);
				if (player.getTune() != null) {
					switch (player.getTune().getInfo().sid1Model) {
					case MOS6581:
						setSIDModel(ChipModel.MOS6581);
						break;
					case MOS8580:
						setSIDModel(ChipModel.MOS8580);
						break;
					default:
						setSIDModel(config.getEmulation().getDefaultSidModel());
						break;
					}
				} else {
					ChipModel defaultModel = config.getEmulation()
							.getDefaultSidModel();
					setSIDModel(defaultModel);
				}
			} else {
				ChipModel m = (ChipModel) sid1Model.getSelectedItem();
				config.getEmulation().setUserSidModel(m);
				setSIDModel(m);
			}
			consolePl.updateSidEmulation();
		}
	};

	public Action setSid2Model = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			if (sid2Model.getSelectedIndex() == 0) {
				config.getEmulation().setStereoSidModel(null);
			} else {
				ChipModel stereoSidModel = (ChipModel) sid2Model
						.getSelectedItem();
				config.getEmulation().setStereoSidModel(stereoSidModel);
			}
			consolePl.updateSidEmulation();
		}
	};

	public Action setBaseAddress = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			config.getEmulation().setDualSidBase(
					Integer.decode(baseAddress.getText()));
			uiEvents.fireEvent(IReplayTune.class, new IReplayTune() {
			});
		}
	};

	public Action doForceStereo = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			config.getEmulation().setForceStereoTune(forceStereo.isSelected());
			uiEvents.fireEvent(IReplayTune.class, new IReplayTune() {
			});
		}
	};

	public Action setDigiBoost = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			setDigiBoost(boosted8580.isSelected());
		}
	};

	public Action setFilter = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			final String filterName = (String) filter.getSelectedItem();
			setFilter(filterName);
			setChipModel();
		}
	};

	public EmulationSettings(ConsolePlayer cp, Player pl, IConfig cfg) {
		this.consolePl = cp;
		this.player = pl;
		this.config = cfg;
		uiEvents.addListener(this);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				uiEvents.removeListener(EmulationSettings.this);
				uiEvents.removeListener(filterCurve);
			}
		});
		try {
			swix = new SwingEngine(this);
			swix.getTaglib().registerTag("dknob", DKnob2.class);
			swix.getTaglib().registerTag("curvefit", CurveFit.class);
			swix.insert(EmulationSettings.class
					.getResource("EmulationSettings.xml"), this);

			fillComboBoxes();
			setDefaultsAndActions();

			Dimension d = getToolkit().getScreenSize();
			Dimension s = getSize();
			setLocation(new Point((d.width - s.width) / 2,
					(d.height - s.height) / 2));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void setDefaultsAndActions() {
		leftVolume.setValue(config.getAudio().getLeftVolume());
		leftVolume.addChangeListener(this);
		rightVolume.setValue(config.getAudio().getRightVolume());
		rightVolume.addChangeListener(this);

		sid1Model.removeActionListener(setSid1Model);
		sid2Model.removeActionListener(setSid2Model);
		filter.removeActionListener(setFilter);
		if (config.getEmulation().isForceStereoTune()) {
			ChipModel stereoSidModel = config.getEmulation()
					.getStereoSidModel();
			if (stereoSidModel != null) {
				sid2Model.setSelectedItem(stereoSidModel);
			} else {
				// default: same as 1st SID model
				sid2Model.setSelectedIndex(sid1Model.getSelectedIndex());
			}
		} else {
			sid2Model.setSelectedIndex(0);
		}
		sid1Model.addActionListener(setSid1Model);
		sid2Model.addActionListener(setSid2Model);
		filter.addActionListener(setFilter);

		baseAddress.setText(String.format("0x%4x", config.getEmulation()
				.getDualSidBase()));
		forceStereo.setSelected(config.getEmulation().isForceStereoTune());
		setDigiBoost(boosted8580.isSelected());

		uiEvents.addListener(filterCurve);
		filterCurve.setConfig(config);
		setChipModel();
	}

	protected void setDigiBoost(final boolean selected) {
		// set settings
		config.getEmulation().setDigiBoosted8580(selected);
		final int input = selected ? 0x7FF : 0;
		for (int i = 0; i < C64.MAX_SIDS; i++) {
			SID sid = getSID(i);
			if (sid != null) {
				sid.input(input);
			}
		}
	}

	private SID getSID(final int num) {
		final SIDEmu sid = player.getC64().getSID(num);
		if (sid instanceof ReSID) {
			return ((ReSID) sid).sid();
		} else {
			return null;
		}
	}

	protected void setChipModel() {
		ChipModel lockModel = config.getEmulation().getUserSidModel();
		if (lockModel != null) {
			setSIDModel(lockModel);
			sid1Model.setSelectedItem(lockModel);
		} else {
			if (player.getTune() != null) {
				switch (player.getTune().getInfo().sid1Model) {
				case MOS6581:
					setSIDModel(ChipModel.MOS6581);
					break;
				case MOS8580:
					setSIDModel(ChipModel.MOS8580);
					break;
				default:
					setSIDModel(config.getEmulation().getDefaultSidModel());
					break;
				}
			} else {
				ChipModel defaultModel = config.getEmulation()
						.getDefaultSidModel();
				setSIDModel(defaultModel);
				sid1Model.setSelectedIndex(0);
			}
		}
	}

	protected void setFilter(final String filterName) {
		if (filterName == null) {
			return;
		}

		final boolean enable;
		if ("".equals(filterName)) {
			enable = false;
		} else {
			enable = true;
			saveCurrentFilter(filterName);
		}
		config.getEmulation().setFilter(enable);

		IFilterSection f6581 = null;
		List<? extends IFilterSection> filters = config.getFilter();
		for (IFilterSection iFilterSection : filters) {
			if (iFilterSection.getName().equals(
					config.getEmulation().getFilter6581())) {
				if (iFilterSection.getFilter8580CurvePosition() == 0) {
					f6581 = iFilterSection;
				}
			}
		}
		IFilterSection f8580 = null;
		for (IFilterSection iFilterSection : filters) {
			if (iFilterSection.getName().equals(
					config.getEmulation().getFilter8580())) {
				if (iFilterSection.getFilter8580CurvePosition() != 0) {
					f8580 = iFilterSection;
				}
			}
		}

		for (int i = 0; i < C64.MAX_SIDS; i++) {
			final SIDEmu resid = player.getC64().getSID(i);
			if (resid != null) {
				resid.setFilter(enable);
				if (resid instanceof ReSID) {
					((ReSID) resid).filter(f6581, f8580);
				}
			}
		}

		uiEvents.fireEvent(IChangeFilter.class, new IChangeFilter() {

			@Override
			public String getFilterName() {
				return filterName;
			}
		});
	}

	private void saveCurrentFilter(final String filterName) {
		final SIDEmu sid = player.getC64().getSID(0);
		if (sid != null) {
			final ChipModel model = sid.getChipModel();
			if (model == ChipModel.MOS6581) {
				config.getEmulation().setFilter6581(filterName);
			} else {
				config.getEmulation().setFilter8580(filterName);
			}
		}
	}

	private void fillComboBoxes() {
		sid1Model.removeActionListener(setSid1Model);
		sid2Model.removeActionListener(setSid2Model);
		filter.removeActionListener(setFilter);
		sid1Model.addItem(swix.getLocalizer().getString("AUTO"));
		sid1Model.addItem(ChipModel.MOS6581);
		sid1Model.addItem(ChipModel.MOS8580);
		sid2Model.addItem(swix.getLocalizer().getString("LIKE_1ST_SID"));
		sid2Model.addItem(ChipModel.MOS6581);
		sid2Model.addItem(ChipModel.MOS8580);
		sid1Model.addActionListener(setSid1Model);
		sid2Model.addActionListener(setSid2Model);
		filter.addActionListener(setFilter);
	}

	@Override
	public void stateChanged(final ChangeEvent event) {
		final Object src = event.getSource();
		if (src == leftVolume) {
			config.getAudio().setLeftVolume(leftVolume.getValue());
			consolePl.setSIDVolume(0, dB2Factor(leftVolume.getValue()));
		} else if (src == rightVolume) {
			config.getAudio().setRightVolume(rightVolume.getValue());
			consolePl.setSIDVolume(1, dB2Factor(rightVolume.getValue()));
		}
	}

	private float dB2Factor(final float dB) {
		return (float) Math.pow(10, dB / 20);
	}

	/**
	 * @param m
	 */
	public void setSIDModel(ChipModel m) {
		for (int i = 0; i < C64.MAX_SIDS; i++) {
			SID sid = getSID(i);
			if (sid != null) {
				sid.setChipModel(m);
			}
		}
		setFilter(m);
	}

	private void setFilter(final ChipModel model) {
		final boolean enable = config.getEmulation().isFilter();
		String item = null;
		if (enable) {
			if (model == ChipModel.MOS6581) {
				item = config.getEmulation().getFilter6581();
			} else if (model == ChipModel.MOS8580) {
				item = config.getEmulation().getFilter8580();
			}
		}
		filter.removeActionListener(setFilter);
		filter.removeAllItems();
		filter.addItem("");

		if (model != null) {
			List<? extends IFilterSection> filters = config.getFilter();
			for (IFilterSection iFilterSection : filters) {
				if (iFilterSection.getFilter8580CurvePosition() != 0
						^ model == ChipModel.MOS6581) {
					filter.addItem(iFilterSection.getName());
				}
			}
		}
		filter.addActionListener(setFilter);

		if (enable) {
			filter.setSelectedItem(item);
		} else {
			filter.setSelectedIndex(0);
		}
	}

	/**
	 * If some settings of the GUI changes, some settings of this panel must be
	 * set accordingly.
	 */
	@Override
	public void notify(final UIEvent event) {
		if (event.isOfType(IChangeFilter.class)) {
			setChipModel();
		}
	}

}