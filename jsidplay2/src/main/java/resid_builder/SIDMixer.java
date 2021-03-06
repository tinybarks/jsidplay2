package resid_builder;

import static libsidplay.components.pla.PLA.MAX_SIDS;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import libsidplay.common.CPUClock;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.Mixer;
import libsidplay.common.SamplingMethod;
import libsidplay.config.IAudioSection;
import libsidplay.config.IConfig;
import libsidplay.config.ISidPlay2Section;
import resid_builder.SampleMixer.LinearFadingSampleMixer;
import resid_builder.resample.Resampler;
import sidplay.audio.AudioDriver;

/**
 * Mixer to mix SIDs sample data into the audio buffer.
 * 
 * @author ken
 *
 */
public class SIDMixer implements Mixer {
	/**
	 * Scaler to use fast int multiplication while setting volume.
	 */
	private static final int VOLUME_SCALER = 10;

	/**
	 * The mixer mixes the generated sound samples into the drivers audio
	 * buffer.
	 * 
	 * @author ken
	 *
	 */
	private final class MixerEvent extends Event {
		private MixerEvent(String name) {
			super(name);
		}

		/**
		 * Random source for triangular dithering
		 */
		private final Random RANDOM = new Random();
		/**
		 * State of HP-TPDF.
		 */
		private int oldRandomValue;

		/**
		 * Fast forward factor:<BR>
		 * fastForwardShift=1<<(VOLUME_SCALER+fastForwardFactor)
		 */
		private int fastForwardShift, fastForwardBitMask;

		/**
		 * The mixer mixes the generated sound samples into the drivers audio
		 * buffer.
		 * <OL>
		 * <LI>Clock SIDs to fill audio buffer.
		 * <LI>Accumulate samples to implement fast forwarding.
		 * <LI>Resample the SID output, because the sample frequency is
		 * different to the clock frequency.
		 * <LI>Add dithering to reduce quantization noise, when moving to a
		 * format with less precision.
		 * <LI>Cut-off overflow samples.
		 * </OL>
		 * <B>Note:</B><BR>
		 * Audio buffer is cleared afterwards to get refilled during next event.
		 */
		@Override
		public void event() throws InterruptedException {
			// Clock SIDs to fill the audio buffer
			for (ReSIDBase sid : sids) {
				SampleMixer sampler = (SampleMixer) sid.getSampler();
				// clock SID to the present moment
				sid.clock();
				sampler.clear();
			}
			// Read from audio buffers
			int valL = 0, valR = 0;
			for (int i = 0; i < bufferSize; i++) {
				// Accumulate sample data with respect to fast forward factor
				valL += audioBufferL.get();
				valR += audioBufferR.get();

				// once enough samples have been accumulated, write output
				if ((i & fastForwardBitMask) == fastForwardBitMask) {
					int dither = triangularDithering();

					if (resamplerL.input(valL >> fastForwardShift)) {
						buffer.putShort((short) Math.max(Math.min(resamplerL.output() + dither, Short.MAX_VALUE),
								Short.MIN_VALUE));
					}
					if (resamplerR.input(valR >> fastForwardShift)) {
						if (!buffer.putShort((short) Math.max(Math.min(resamplerR.output() + dither, Short.MAX_VALUE),
								Short.MIN_VALUE)).hasRemaining()) {
							driver.write();
							buffer.clear();
						}
					}
					// zero accumulator
					valL = valR = 0;
				}
			}
			// Erase audio buffers
			audioBufferL.flip();
			audioBufferR.flip();
			for (int i = 0; i < bufferSize; i++) {
				audioBufferL.put(0);
				audioBufferR.put(0);
			}
			audioBufferL.clear();
			audioBufferR.clear();
			context.schedule(this, bufferSize);
		}

		/**
		 * Triangularly shaped noise source for audio applications. Output of
		 * this PRNG is between ]-1, 1[.
		 * 
		 * @return triangular noise sample
		 */
		private int triangularDithering() {
			int prevValue = oldRandomValue;
			oldRandomValue = RANDOM.nextInt() & 0x1;
			return oldRandomValue - prevValue;
		}

	}

	/**
	 * System event context.
	 */
	protected final EventScheduler context;

	/**
	 * Configuration
	 */
	protected final IConfig config;

	/**
	 * CPU clock.
	 */
	protected final CPUClock cpuClock;

	/**
	 * SIDs to mix their sound output.
	 */
	protected final List<ReSIDBase> sids = new ArrayList<ReSIDBase>(MAX_SIDS);

	/**
	 * Mixer clocking SID chips and producing audio output.
	 */
	private final MixerEvent mixerAudio = new MixerEvent("MixerAudio");

	/**
	 * Audio buffers for two channels (stereo).
	 */
	private final IntBuffer audioBufferL, audioBufferR;

	/**
	 * Capacity of the Audio buffers audioBufferL and audioBufferR.
	 */
	private final int bufferSize;

	/**
	 * Resampler of sample output for two channels (stereo).
	 */
	private final Resampler resamplerL, resamplerR;

	/**
	 * Audio driver
	 */
	private final AudioDriver driver;

	/**
	 * Volume of all SIDs.
	 */
	private final int[] volume = new int[MAX_SIDS];
	/**
	 * SID audibility on the left speaker of all SIDs 0(silent)..1(loud).
	 */
	private final float[] positionL = new float[MAX_SIDS];
	/**
	 * SID audibility on the right speaker of all SIDs 0(silent)..1(loud).
	 */
	private final float[] positionR = new float[MAX_SIDS];

	/**
	 * Fade-in/fade-out enabled.
	 */
	private final boolean fadeInFadeOutEnabled;

	/**
	 * Audio driver buffer.
	 */
	private final ByteBuffer buffer;

	public SIDMixer(EventScheduler context, IConfig config, CPUClock cpuClock, AudioDriver audioDriver) {
		this.context = context;
		this.config = config;
		this.cpuClock = cpuClock;
		this.driver = audioDriver;
		IAudioSection audioSection = config.getAudioSection();
		this.buffer = driver.buffer();
		this.bufferSize = audioSection.getBufferSize();
		this.audioBufferL = ByteBuffer.allocateDirect(Integer.BYTES * bufferSize).order(ByteOrder.nativeOrder())
				.asIntBuffer();
		this.audioBufferR = ByteBuffer.allocateDirect(Integer.BYTES * bufferSize).order(ByteOrder.nativeOrder())
				.asIntBuffer();
		SamplingMethod samplingMethod = audioSection.getSampling();
		int samplingFrequency = audioSection.getSamplingRate().getFrequency();
		this.resamplerL = Resampler.createResampler(cpuClock.getCpuFrequency(), samplingMethod, samplingFrequency,
				20000);
		this.resamplerR = Resampler.createResampler(cpuClock.getCpuFrequency(), samplingMethod, samplingFrequency,
				20000);
		ISidPlay2Section sidplay2Section = config.getSidplay2Section();
		this.fadeInFadeOutEnabled = sidplay2Section.getFadeInTime() != 0 || sidplay2Section.getFadeOutTime() != 0;
		normalSpeed();
	}

	/**
	 * Starts mixing the outputs of several SIDs.
	 */
	public void start() {
		context.schedule(mixerAudio, 0, Event.Phase.PHI2);
	}

	/**
	 * Fade-in start time reached, audio volume should be increased to the max.
	 * 
	 * @param fadeIn
	 *            Fade-in time in seconds
	 */
	public void fadeIn(int fadeIn) {
		for (ReSIDBase sid : sids) {
			LinearFadingSampleMixer sampler = (LinearFadingSampleMixer) sid.getSampler();
			sampler.setFadeIn((long) (fadeIn * cpuClock.getCpuFrequency()));
		}
	}

	/**
	 * Fade-out start time reached, audio volume should be lowered to zero.
	 * 
	 * @param fadeOut
	 *            Fade-out time in seconds
	 */
	public void fadeOut(int fadeOut) {
		for (ReSIDBase sid : sids) {
			LinearFadingSampleMixer sampler = (LinearFadingSampleMixer) sid.getSampler();
			sampler.setFadeOut((long) (fadeOut * cpuClock.getCpuFrequency()));
		}
	}

	/**
	 * Add a SID to the mix.
	 * 
	 * @param sidNum
	 *            SID chip number
	 * @param sid
	 *            SID to add
	 */
	public void add(int sidNum, ReSIDBase sid) {
		if (sidNum < sids.size()) {
			sids.set(sidNum, sid);
		} else {
			sids.add(sid);
		}
		createSampleMixer(sid);
		setVolume(sidNum, config.getAudioSection().getVolume(sidNum));
		setBalance(sidNum, config.getAudioSection().getBalance(sidNum));
	}

	/**
	 * Remove SID from the mix.
	 * 
	 * @param sid
	 *            SID to remove
	 */
	public void remove(ReSIDBase sid) {
		sids.remove(sid);
		sid.setSampler(null);
		updateSampleMixerVolume();
	}

	/**
	 * Volume of the SID chip.
	 * 
	 * @param sidNum
	 *            SID chip number
	 * @param volume
	 *            volume in DB -6(-6db)..6(+6db)
	 */
	public void setVolume(int sidNum, float volumeInDB) {
		assert volumeInDB >= -6 && volumeInDB <= 6;

		volume[sidNum] = (int) (DECIBEL_TO_LINEAR(volumeInDB) * (1 << VOLUME_SCALER));
		updateSampleMixerVolume();
	}

	/**
	 * db-to-linear(x) = 10^(x / 20)
	 * 
	 * @param decibel
	 *            decibel value to convert
	 * @return converted linear value
	 */
	private static double DECIBEL_TO_LINEAR(float decibel) {
		return Math.pow(10., decibel / 20.);
	}

	/**
	 * Set left/right speaker balance for each SID.
	 * 
	 * @param sidNum
	 *            SID chip number
	 * @param balance
	 *            balance 0(left speaker)..0.5(centered)..1(right speaker)
	 */
	public void setBalance(int sidNum, float balance) {
		assert balance >= 0 && balance <= 1;

		positionL[sidNum] = 1 - balance;
		positionR[sidNum] = balance;
		updateSampleMixerVolume();
	}

	/**
	 * Create a new sample value mixer and assign to SID chip.
	 * 
	 * @param sid
	 *            SID chip that requires a sample mixer.
	 */
	private void createSampleMixer(ReSIDBase sid) {
		IntBuffer intBufferL = audioBufferL.duplicate();
		IntBuffer intBufferR = audioBufferR.duplicate();
		if (fadeInFadeOutEnabled) {
			sid.setSampler(new LinearFadingSampleMixer(intBufferL, intBufferR));
		} else {
			sid.setSampler(new SampleMixer(intBufferL, intBufferR));
		}
	}

	/**
	 * Set the sample mixer volume to the calculated balanced volume level.<BR>
	 * Mono output: Use volume.<BR>
	 * Stereo or 3-SID output: Use speaker audibility and volume.
	 */
	private void updateSampleMixerVolume() {
		boolean mono = sids.size() == 1;
		int sidNum = 0;
		for (ReSIDBase sid : sids) {
			SampleMixer sampler = (SampleMixer) sid.getSampler();
			if (mono) {
				sampler.setVolume(volume[sidNum], volume[sidNum]);
			} else {
				float leftFraction = positionL[sidNum];
				float rightFraction = positionR[sidNum];
				int volumeL = (int) (volume[sidNum] * leftFraction);
				int volumeR = (int) (volume[sidNum] * rightFraction);
				sampler.setVolume(volumeL, volumeR);
			}
			sidNum++;
		}
	}

	/**
	 * Doubles speed factor.
	 */
	public void fastForward() {
		mixerAudio.fastForwardShift = Math.min(mixerAudio.fastForwardShift + 1, MAX_FAST_FORWARD + VOLUME_SCALER);
		mixerAudio.fastForwardBitMask = (1 << mixerAudio.fastForwardShift - VOLUME_SCALER) - 1;
	}

	/**
	 * Use normal speed factor.
	 */
	public void normalSpeed() {
		mixerAudio.fastForwardShift = VOLUME_SCALER;
		mixerAudio.fastForwardBitMask = 0;
	}

	public boolean isFastForward() {
		return mixerAudio.fastForwardShift - VOLUME_SCALER != 0;
	}

	public int getFastForwardBitMask() {
		return mixerAudio.fastForwardBitMask;
	}
}