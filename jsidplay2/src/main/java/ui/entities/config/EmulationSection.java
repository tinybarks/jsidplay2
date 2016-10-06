package ui.entities.config;

import static sidplay.ini.IniDefaults.DEFAULT_3SID_EMULATION;
import static sidplay.ini.IniDefaults.DEFAULT_3SID_FILTER_6581;
import static sidplay.ini.IniDefaults.DEFAULT_3SID_FILTER_8580;
import static sidplay.ini.IniDefaults.DEFAULT_3SID_MODEL;
import static sidplay.ini.IniDefaults.DEFAULT_CLOCK_SPEED;
import static sidplay.ini.IniDefaults.DEFAULT_DIGI_BOOSTED_8580;
import static sidplay.ini.IniDefaults.DEFAULT_DUAL_SID_BASE;
import static sidplay.ini.IniDefaults.DEFAULT_EMULATION;
import static sidplay.ini.IniDefaults.DEFAULT_ENGINE;
import static sidplay.ini.IniDefaults.DEFAULT_FAKE_STEREO;
import static sidplay.ini.IniDefaults.DEFAULT_FILTER_6581;
import static sidplay.ini.IniDefaults.DEFAULT_FILTER_8580;
import static sidplay.ini.IniDefaults.DEFAULT_FORCE_3SID_TUNE;
import static sidplay.ini.IniDefaults.DEFAULT_FORCE_STEREO_TUNE;
import static sidplay.ini.IniDefaults.DEFAULT_HARD_SID_6581;
import static sidplay.ini.IniDefaults.DEFAULT_HARD_SID_8580;
import static sidplay.ini.IniDefaults.DEFAULT_ReSIDfp_3SID_FILTER_6581;
import static sidplay.ini.IniDefaults.DEFAULT_ReSIDfp_3SID_FILTER_8580;
import static sidplay.ini.IniDefaults.DEFAULT_ReSIDfp_FILTER_6581;
import static sidplay.ini.IniDefaults.DEFAULT_ReSIDfp_FILTER_8580;
import static sidplay.ini.IniDefaults.DEFAULT_ReSIDfp_STEREO_FILTER_6581;
import static sidplay.ini.IniDefaults.DEFAULT_ReSIDfp_STEREO_FILTER_8580;
import static sidplay.ini.IniDefaults.DEFAULT_SID_MODEL;
import static sidplay.ini.IniDefaults.DEFAULT_SID_NUM_TO_READ;
import static sidplay.ini.IniDefaults.DEFAULT_STEREO_EMULATION;
import static sidplay.ini.IniDefaults.DEFAULT_STEREO_FILTER_6581;
import static sidplay.ini.IniDefaults.DEFAULT_STEREO_FILTER_8580;
import static sidplay.ini.IniDefaults.DEFAULT_STEREO_MODEL;
import static sidplay.ini.IniDefaults.DEFAULT_THIRD_SID_BASE;
import static sidplay.ini.IniDefaults.DEFAULT_USER_EMULATION;
import static sidplay.ini.IniDefaults.DEFAULT_USER_MODEL;
import static sidplay.ini.IniDefaults.DEFAULT_USE_3SID_FILTER;
import static sidplay.ini.IniDefaults.DEFAULT_USE_FILTER;
import static sidplay.ini.IniDefaults.DEFAULT_USE_STEREO_FILTER;

import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidplay.common.Emulation;
import libsidplay.common.Engine;
import libsidplay.config.IEmulationSection;

@Embeddable
public class EmulationSection implements IEmulationSection {

	private ObjectProperty<Engine> engine = new SimpleObjectProperty<Engine>(DEFAULT_ENGINE);

	public ObjectProperty<Engine> engineProperty() {
		return engine;
	}

	@Enumerated(EnumType.STRING)
	@Override
	public Engine getEngine() {
		return this.engine.get();
	}

	@Override
	public void setEngine(Engine engine) {
		this.engine.set(engine);
	}

	private ObjectProperty<Emulation> defaultEmulation = new SimpleObjectProperty<Emulation>(DEFAULT_EMULATION);

	public ObjectProperty<Emulation> defaultEmulationProperty() {
		return defaultEmulation;
	}

	@Enumerated(EnumType.STRING)
	@Override
	public Emulation getDefaultEmulation() {
		return this.defaultEmulation.get();
	}

	@Override
	public void setDefaultEmulation(Emulation emulation) {
		this.defaultEmulation.set(emulation);
	}

	private ObjectProperty<Emulation> userEmulation = new SimpleObjectProperty<Emulation>(DEFAULT_USER_EMULATION);

	public ObjectProperty<Emulation> userEmulationProperty() {
		return userEmulation;
	}

	@Enumerated(EnumType.STRING)
	@Override
	public Emulation getUserEmulation() {
		return this.userEmulation.get();
	}

	@Override
	public void setUserEmulation(Emulation userEmulation) {
		this.userEmulation.set(userEmulation);
	}

	private ObjectProperty<Emulation> stereoEmulation = new SimpleObjectProperty<Emulation>(DEFAULT_STEREO_EMULATION);

	public ObjectProperty<Emulation> stereoEmulationProperty() {
		return stereoEmulation;
	}

	@Enumerated(EnumType.STRING)
	@Override
	public Emulation getStereoEmulation() {
		return this.stereoEmulation.get();
	}

	@Override
	public void setStereoEmulation(Emulation stereoEmulation) {
		this.stereoEmulation.set(stereoEmulation);
	}

	private ObjectProperty<Emulation> thirdEmulation = new SimpleObjectProperty<Emulation>(DEFAULT_3SID_EMULATION);

	public ObjectProperty<Emulation> thirdEmulationProperty() {
		return thirdEmulation;
	}

	@Enumerated(EnumType.STRING)
	@Override
	public Emulation getThirdEmulation() {
		return this.thirdEmulation.get();
	}

	@Override
	public void setThirdEmulation(Emulation thirdEmulation) {
		this.thirdEmulation.set(thirdEmulation);
	}

	private ObjectProperty<CPUClock> defaultClockSpeed = new SimpleObjectProperty<CPUClock>(DEFAULT_CLOCK_SPEED);

	public ObjectProperty<CPUClock> defaultClockSpeedProperty() {
		return defaultClockSpeed;
	}

	@Enumerated(EnumType.STRING)
	@Override
	public CPUClock getDefaultClockSpeed() {
		return this.defaultClockSpeed.get();
	}

	@Override
	public void setDefaultClockSpeed(CPUClock speed) {
		this.defaultClockSpeed.set(speed);
	}

	private ObjectProperty<CPUClock> userClockSpeed = new SimpleObjectProperty<CPUClock>();

	public ObjectProperty<CPUClock> userClockSpeedProperty() {
		return userClockSpeed;
	}

	@Enumerated(EnumType.STRING)
	@Override
	public CPUClock getUserClockSpeed() {
		return userClockSpeed.get();
	}

	@Override
	public void setUserClockSpeed(CPUClock userClockSpeed) {
		this.userClockSpeed.set(userClockSpeed);
	}

	private ObjectProperty<ChipModel> defaultSidModel = new SimpleObjectProperty<ChipModel>(DEFAULT_SID_MODEL);

	public ObjectProperty<ChipModel> defaultSidModelProperty() {
		return defaultSidModel;
	}

	@Enumerated(EnumType.STRING)
	@Override
	public ChipModel getDefaultSidModel() {
		return defaultSidModel.get();
	}

	@Override
	public void setDefaultSidModel(ChipModel defaultSidModel) {
		this.defaultSidModel.set(defaultSidModel);
	}

	private ObjectProperty<ChipModel> userSidModel = new SimpleObjectProperty<ChipModel>(DEFAULT_USER_MODEL);

	public ObjectProperty<ChipModel> userSidModelProperty() {
		return userSidModel;
	}

	@Enumerated(EnumType.STRING)
	@Override
	public ChipModel getUserSidModel() {
		return userSidModel.get();
	}

	@Override
	public void setUserSidModel(ChipModel userSidModel) {
		this.userSidModel.set(userSidModel);
	}

	private ObjectProperty<ChipModel> stereoSidModel = new SimpleObjectProperty<ChipModel>(DEFAULT_STEREO_MODEL);

	public ObjectProperty<ChipModel> stereoSidModelProperty() {
		return stereoSidModel;
	}

	@Enumerated(EnumType.STRING)
	@Override
	public ChipModel getStereoSidModel() {
		return stereoSidModel.get();
	}

	@Override
	public void setStereoSidModel(ChipModel stereoSidModel) {
		this.stereoSidModel.set(stereoSidModel);
	}

	private ObjectProperty<ChipModel> thirdSIDModel = new SimpleObjectProperty<ChipModel>(DEFAULT_3SID_MODEL);

	public ObjectProperty<ChipModel> thirdSIDModelProperty() {
		return thirdSIDModel;
	}

	@Enumerated(EnumType.STRING)
	@Override
	public ChipModel getThirdSIDModel() {
		return thirdSIDModel.get();
	}

	@Override
	public void setThirdSIDModel(ChipModel stereoSidModel) {
		this.thirdSIDModel.set(stereoSidModel);
	}

	private ObjectProperty<Integer> hardsid6581 = new SimpleObjectProperty<Integer>(DEFAULT_HARD_SID_6581);

	public ObjectProperty<Integer> hardsid6581Property() {
		return hardsid6581;
	}
	
	@Override
	public int getHardsid6581() {
		return hardsid6581.get();
	}

	@Override
	public void setHardsid6581(int hardsid6581) {
		this.hardsid6581.set(hardsid6581);
	}

	private ObjectProperty<Integer> hardsid8580 = new SimpleObjectProperty<Integer>(DEFAULT_HARD_SID_8580);

	public ObjectProperty<Integer> hardsid8580Property() {
		return hardsid8580;
	}
	
	@Override
	public int getHardsid8580() {
		return hardsid8580.get();
	}

	@Override
	public void setHardsid8580(int hardsid8580) {
		this.hardsid8580.set(hardsid8580);
	}

	private BooleanProperty filter = new SimpleBooleanProperty(DEFAULT_USE_FILTER);

	public BooleanProperty filterProperty() {
		return filter;
	}

	@Override
	public boolean isFilter() {
		return filter.get();
	}

	@Override
	public void setFilter(boolean isFilter) {
		this.filter.set(isFilter);
	}

	private BooleanProperty stereoFilter = new SimpleBooleanProperty(DEFAULT_USE_STEREO_FILTER);

	public BooleanProperty stereoFilterProperty() {
		return stereoFilter;
	}

	@Override
	public boolean isStereoFilter() {
		return stereoFilter.get();
	}

	@Override
	public void setStereoFilter(boolean isFilter) {
		this.stereoFilter.set(isFilter);
	}

	private BooleanProperty thirdSIDFilter = new SimpleBooleanProperty(DEFAULT_USE_3SID_FILTER);

	public BooleanProperty thirdSIDFilterProperty() {
		return thirdSIDFilter;
	}

	@Override
	public boolean isThirdSIDFilter() {
		return thirdSIDFilter.get();
	}

	@Override
	public void setThirdSIDFilter(boolean isFilter) {
		this.thirdSIDFilter.set(isFilter);
	}

	private ObjectProperty<Integer> sidNumToRead = new SimpleObjectProperty<Integer>(DEFAULT_SID_NUM_TO_READ);

	public ObjectProperty<Integer> sidNumToReadProperty() {
		return sidNumToRead;
	}
	
	@Override
	public int getSidNumToRead() {
		return sidNumToRead.get();
	}

	@Override
	public void setSidNumToRead(int sidNumToRead) {
		this.sidNumToRead.set(sidNumToRead);
	}

	private BooleanProperty digiBoosted8580 = new SimpleBooleanProperty(DEFAULT_DIGI_BOOSTED_8580);

	public BooleanProperty digiBoosted8580Property() {
		return digiBoosted8580;
	}

	@Override
	public boolean isDigiBoosted8580() {
		return digiBoosted8580.get();
	}

	@Override
	public void setDigiBoosted8580(boolean isDigiBoosted8580) {
		this.digiBoosted8580.set(isDigiBoosted8580);
	}

	private BooleanProperty fakeStereo = new SimpleBooleanProperty(DEFAULT_FAKE_STEREO);

	public BooleanProperty fakeStereoProperty() {
		return fakeStereo;
	}

	@Override
	public boolean isFakeStereo() {
		return fakeStereo.get();
	}

	public void setFakeStereo(boolean fakeStereo) {
		this.fakeStereo.set(fakeStereo);
	}

	private IntegerProperty dualSidBase = new SimpleIntegerProperty(DEFAULT_DUAL_SID_BASE);

	public IntegerProperty dualSidBaseProperty() {
		return dualSidBase;
	}

	@Override
	public int getDualSidBase() {
		return dualSidBase.get();
	}

	@Override
	public void setDualSidBase(int dualSidBase) {
		this.dualSidBase.set(dualSidBase);
	}

	private IntegerProperty thirdSIDBase = new SimpleIntegerProperty(DEFAULT_THIRD_SID_BASE);

	public IntegerProperty thirdSIDBaseProperty() {
		return thirdSIDBase;
	}

	@Override
	public int getThirdSIDBase() {
		return thirdSIDBase.get();
	}

	@Override
	public void setThirdSIDBase(int dualSidBase) {
		this.thirdSIDBase.set(dualSidBase);
	}

	private boolean forceStereoTune = DEFAULT_FORCE_STEREO_TUNE;

	@Override
	public boolean isForceStereoTune() {
		return forceStereoTune;
	}

	@Override
	public void setForceStereoTune(boolean isForceStereoTune) {
		this.forceStereoTune = isForceStereoTune;
	}

	private boolean force3SIDTune = DEFAULT_FORCE_3SID_TUNE;

	@Override
	public boolean isForce3SIDTune() {
		return force3SIDTune;
	}

	@Override
	public void setForce3SIDTune(boolean isForceStereoTune) {
		this.force3SIDTune = isForceStereoTune;
	}

	private String filter6581 = DEFAULT_FILTER_6581;

	@Override
	public String getFilter6581() {
		return filter6581;
	}

	@Override
	public void setFilter6581(String filter6581) {
		this.filter6581 = filter6581;
	}

	private String stereoFilter6581 = DEFAULT_STEREO_FILTER_6581;

	@Override
	public String getStereoFilter6581() {
		return stereoFilter6581;
	}

	@Override
	public void setStereoFilter6581(String filter6581) {
		this.stereoFilter6581 = filter6581;
	}

	private String thirdSIDFilter6581 = DEFAULT_3SID_FILTER_6581;

	@Override
	public String getThirdSIDFilter6581() {
		return thirdSIDFilter6581;
	}

	@Override
	public void setThirdSIDFilter6581(String filter6581) {
		this.thirdSIDFilter6581 = filter6581;
	}

	private String filter8580 = DEFAULT_FILTER_8580;

	@Override
	public String getFilter8580() {
		return filter8580;
	}

	@Override
	public void setFilter8580(String filter8580) {
		this.filter8580 = filter8580;
	}

	private String stereoFilter8580 = DEFAULT_STEREO_FILTER_8580;

	@Override
	public String getStereoFilter8580() {
		return stereoFilter8580;
	}

	@Override
	public void setStereoFilter8580(String filter8580) {
		this.stereoFilter8580 = filter8580;
	}

	private String thirdSIDFilter8580 = DEFAULT_3SID_FILTER_8580;

	@Override
	public String getThirdSIDFilter8580() {
		return thirdSIDFilter8580;
	}

	@Override
	public void setThirdSIDFilter8580(String filter8580) {
		this.thirdSIDFilter8580 = filter8580;
	}

	private String reSIDfpFilter6581 = DEFAULT_ReSIDfp_FILTER_6581;

	@Override
	public String getReSIDfpFilter6581() {
		return reSIDfpFilter6581;
	}

	@Override
	public void setReSIDfpFilter6581(String reSIDfpFilter6581) {
		this.reSIDfpFilter6581 = reSIDfpFilter6581;
	}

	private String reSIDfpStereoFilter6581 = DEFAULT_ReSIDfp_STEREO_FILTER_6581;

	@Override
	public String getReSIDfpStereoFilter6581() {
		return reSIDfpStereoFilter6581;
	}

	@Override
	public void setReSIDfpStereoFilter6581(String reSIDfpFilter6581) {
		this.reSIDfpStereoFilter6581 = reSIDfpFilter6581;
	}

	private String reSIDfp3rdSIDFilter6581 = DEFAULT_ReSIDfp_3SID_FILTER_6581;

	@Override
	public String getReSIDfpThirdSIDFilter6581() {
		return reSIDfp3rdSIDFilter6581;
	}

	@Override
	public void setReSIDfpThirdSIDFilter6581(String reSIDfpFilter6581) {
		this.reSIDfp3rdSIDFilter6581 = reSIDfpFilter6581;
	}

	private String reSIDfpFilter8580 = DEFAULT_ReSIDfp_FILTER_8580;

	@Override
	public String getReSIDfpFilter8580() {
		return reSIDfpFilter8580;
	}

	@Override
	public void setReSIDfpFilter8580(String reSIDfpFilter8580) {
		this.reSIDfpFilter8580 = reSIDfpFilter8580;
	}

	private String reSIDfpStereoFilter8580 = DEFAULT_ReSIDfp_STEREO_FILTER_8580;

	@Override
	public String getReSIDfpStereoFilter8580() {
		return reSIDfpStereoFilter8580;
	}

	@Override
	public void setReSIDfpStereoFilter8580(String reSIDfpFilter8580) {
		this.reSIDfpStereoFilter8580 = reSIDfpFilter8580;
	}

	private String reSIDfp3rdSIDFilter8580 = DEFAULT_ReSIDfp_3SID_FILTER_8580;

	@Override
	public String getReSIDfpThirdSIDFilter8580() {
		return reSIDfp3rdSIDFilter8580;
	}

	@Override
	public void setReSIDfpThirdSIDFilter8580(String reSIDfpFilter8580) {
		this.reSIDfp3rdSIDFilter8580 = reSIDfpFilter8580;
	}

}
