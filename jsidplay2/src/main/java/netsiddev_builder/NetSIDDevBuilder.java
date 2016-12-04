package netsiddev_builder;

import java.util.ArrayList;
import java.util.List;

import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidplay.common.EventScheduler;
import libsidplay.common.Mixer;
import libsidplay.common.SIDBuilder;
import libsidplay.common.SIDChip;
import libsidplay.common.SIDEmu;
import libsidplay.config.IAudioSection;
import libsidplay.config.IConfig;
import libsidplay.config.IEmulationSection;
import libsidplay.sidtune.SidTune;

public class NetSIDDevBuilder implements SIDBuilder, Mixer {

	private EventScheduler context;
	private IConfig config;

	private NetSIDConnection connection;
	private CPUClock cpuClock;
	private List<NetSIDDev> sids = new ArrayList<>();

	public NetSIDDevBuilder(EventScheduler context, IConfig config, SidTune tune, CPUClock cpuClock) {
		this.context = context;
		this.config = config;
		this.cpuClock = cpuClock;
		IEmulationSection emulationSection = config.getEmulationSection();
		this.connection = new NetSIDConnection(context, emulationSection.getNetSIDDevHost(),
				emulationSection.getNetSIDDevPort());
	}

	@Override
	public SIDEmu lock(SIDEmu sidEmu, int sidNum, SidTune tune) {
		IEmulationSection emulationSection = config.getEmulationSection();
		final NetSIDDev sid = createSID(emulationSection, sidEmu, tune, sidNum);
		connection.setSampling(config.getAudioSection().getSampling());
		sid.setChipModel(ChipModel.getChipModel(emulationSection, tune, sidNum));
		sid.setFilter(config, sidNum);
		sid.setFilterEnable(emulationSection, sidNum);
		sid.input(emulationSection.isDigiBoosted8580() ? sid.getInputDigiBoost() : 0);
		// this triggers refreshParams on the server side, therefore the last:
		sid.setClockFrequency(cpuClock.getCpuFrequency());
		connection.setMute(config.getEmulationSection());
		for (byte addr = 0; sidEmu != null && addr < SIDChip.REG_COUNT; addr++) {
			sid.write(addr, sidEmu.readInternalRegister(addr));
		}
		if (sidNum < sids.size())
			sids.set(sidNum, sid);
		else
			sids.add(sid);
		updateMixer(config.getAudioSection());
		return sid;
	}

	@Override
	public void unlock(SIDEmu device) {
		NetSIDDev sid = (NetSIDDev) device;
		byte sidNum = (byte) sids.indexOf(sid);
		connection.flush();
		for (byte reg = 0; reg < SIDChip.REG_COUNT; reg++) {
			connection.write(sidNum, reg, (byte) 0);
		}
		sids.remove(sid);
		updateMixer(config.getAudioSection());
	}

	private void updateMixer(IAudioSection audioSection) {
		for (int sidNum = 0; sidNum < sids.size(); sidNum++) {
			setVolume(sidNum, audioSection.getVolume(sidNum));
			setBalance(sidNum, audioSection.getBalance(sidNum));
		}
	}

	@Override
	public void reset() {
		connection.reset((byte) 0xf);
	}

	@Override
	public void start() {
		connection.start();
	}

	@Override
	public void fadeIn(int fadeIn) {
		// XXX unsupported by JSIDDevice
	}

	@Override
	public void fadeOut(int fadeOut) {
		// XXX unsupported by JSIDDevice
	}

	@Override
	public void setVolume(int sidNum, float volume) {
		// -6db..6db (client)
		// 0..30 (server)
		float level = (volume + 6f) * 30f / 12f;
		connection.setVolume((byte) sidNum, (byte) level);
	}

	@Override
	public void setBalance(int sidNum, float balance) {
		// 0..1 (client)
		// -100..100 (server)
		float position = sids.size() == 1 ? 0 : 200f * balance - 100f;
		connection.setBalance((byte) sidNum, (byte) position);
	}

	@Override
	public void fastForward() {
		connection.fastForward();
	}

	@Override
	public void normalSpeed() {
		connection.normalSpeed();
	}

	@Override
	public boolean isFastForward() {
		return connection.isFastForward();
	}

	@Override
	public int getFastForwardBitMask() {
		return connection.getFastForwardBitMask();
	}

	@Override
	public void pause() {
		connection.flush();
	}

	/**
	 * Create NetworkSIDDevice.
	 * 
	 * @param oldSIDEmu
	 *            currently used NetworkSIDDevice
	 * @param tune
	 *            current tune
	 * @param sidNum
	 *            current SID number
	 * 
	 * @return new NetworkSIDDevice
	 */
	private NetSIDDev createSID(IEmulationSection emulationSection, SIDEmu sidEmu, SidTune tune, int sidNum) {
		final ChipModel chipModel = ChipModel.getChipModel(emulationSection, tune, sidNum);
		if (SidTune.isFakeStereoSid(emulationSection, tune, sidNum)) {
			return new NetSIDDev.FakeStereo(context, connection, sidNum, chipModel, config, sids);
		} else {
			return new NetSIDDev(context, connection, sidNum, chipModel);
		}
	}

}
