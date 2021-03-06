/**
 *                             Cycle Accurate 6510 Emulation
 *                             -----------------------------
 *  begin                : Thu May 11 06:22:40 BST 2000
 *  copyright            : (C) 2000 by Simon White
 *  email                : s_a_white@email.com
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 * @author Ken Händel
 *
 */
package libsidutils.vicesync;

import java.io.IOException;

import libsidplay.common.EventScheduler;
import libsidplay.common.Event.Phase;
import libsidutils.debug.MOS6510Debug;

/**
 * Alpha: Feature to compare VICE and JSIDPlay2 implementation by running both
 * and communicating via socket connection after each instruction.<BR>
 * This has been added to find differences and improve emulation quality.
 * Currently for Edge of Disgrace disk 1 hung up.
 * 
 * @author Ken Händel
 */
public class MOS6510ViceSync extends MOS6510Debug {

	private ViceSync sync;

	private long syncClk;
	private boolean connectedToJava = false;

	public MOS6510ViceSync(final EventScheduler context) {
		super(context);
	}

	@Override
	protected void fetchNextOpcode() {
		if (!connectedToJava && Register_ProgramCounter == 3746 && Register_Accumulator == (byte) 25
				&& Register_X == (byte) 126 && Register_Y == (byte) 1 && Register_StackPointer == (byte) 232) {
			// VICE and JSIDPlay2 have reached the synchronization starting
			// point!
			sync = new ViceSync();
			try {
				sync.connect(6510);
				connectedToJava = true;
				String received = sync.receive();
				ViceSync.MOS6510State viceState = sync.getState(received);
				System.out.println(viceState);
				sync.send("start\n");
				syncClk = context.getTime(Phase.PHI2);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(0);
			}
		} else if (connectedToJava) {
			try {
				String received = sync.receive();
				ViceSync.MOS6510State jsidplay2State = new ViceSync.MOS6510State(context.getTime(Phase.PHI2) - syncClk,
						Register_ProgramCounter, Register_Accumulator, Register_X, Register_Y, Register_StackPointer);
				ViceSync.MOS6510State viceState = sync.getState(received);
				if (viceState.getClk() == 55292330 || (viceState.getClk() == 55288505)) {
					// 55292330 - 16457: pc=0d99(3481), a=b7, x=20, y=04, sp=e8
					sync.send("break\n");
					System.out.println("Until here we have exactly the same state!!!");
				}
				if (!jsidplay2State.equals(viceState)) {
					// clk=55292332
					// (clk=55288509: pc=1e04, a=01(is 0), x=7e, y=01, sp=ea)
					System.out.println("This is the first point where they differ, do some analysis here!!!");
					System.err.println("Differs: " + jsidplay2State.equals(viceState));
				}
				sync.send("thanks!\n");

			} catch (IOException e) {
				e.printStackTrace();
				System.exit(0);
			}
		}
		super.fetchNextOpcode();
	}
}
