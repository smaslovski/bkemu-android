/*
 * Created: 19.04.2012
 *
 * Copyright (C) 2012 Victor Antonovich (v.antonovich@gmail.com)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package su.comp.bk.arch.cpu.opcode;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import android.util.Log;

import su.comp.bk.arch.Computer;
import su.comp.bk.arch.cpu.Cpu;
import su.comp.bk.arch.io.Device;
import su.comp.bk.arch.io.Sel1RegisterSystemBits;
import su.comp.bk.arch.memory.ReadOnlyMemory;

/**
 * Control opcodes (RESET/WAIT/HALT) tests.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(value=Log.class)
public class ControlOpcodesTest {

    private Computer computer;

    @Before
    public void setUp() throws Exception {
        computer = new Computer();
        computer.addDevice(new Sel1RegisterSystemBits(0100000));
    }

    @Test
    public void testResetInstructionExecute() {
        computer.addMemory(new ReadOnlyMemory(0100000, new short[] {
                ResetOpcode.OPCODE
        }));
        computer.reset();
        computer.getCpu().setPswState((short) 0377);
        computer.getCpu().executeSingleInstruction();
        assertEquals(0100002, computer.getCpu().readRegister(false, Cpu.PC));
        assertEquals(0340, computer.getCpu().getPswState());
    }

    @Test
    public void testWaitInstructionExecute() {
        computer.addMemory(new ReadOnlyMemory(0100000, new short[] {
                WaitOpcode.OPCODE
        }));
        computer.reset();
        computer.getCpu().executeSingleInstruction();
        assertEquals(0100002, computer.getCpu().readRegister(false, Cpu.PC));
        assertTrue(computer.getCpu().isInterruptWaitMode());
    }

    @Test
    public void testHaltInstructionExecute() {
        PowerMock.mockStatic(Log.class);
        computer.addMemory(new ReadOnlyMemory(0100000, new short[] {
                HaltOpcode.OPCODE,                   // 0100000: HALT
                (short) 0100010,                     // 0100002: <vector - PC>
                0377,                                // 0100004: <vector - PSW>
                ConditionCodeOpcodes.OPCODE_NOP,     // 0100006: NOP
                ConditionCodeOpcodes.OPCODE_NOP      // 0100010: NOP
        }));

        // Halt PC/PSW store registers mock device
        Device haltStateRegisters = new Device() {
            int regHaltPc;
            int regHaltPsw;
            @Override
            public void write(boolean isByteMode, int address, int value) {
                if (address == Cpu.REG_HALT_PC) {
                    regHaltPc = value;
                } else {
                    regHaltPsw = value;
                }
            }
            @Override
            public void reset() {
            }
            @Override
            public int read(int address) {
                if (address == Cpu.REG_HALT_PC) {
                    return regHaltPc;
                }
                return regHaltPsw;
            }
            @Override
            public int[] getAddresses() {
                return new int[] { Cpu.REG_HALT_PC, Cpu.REG_HALT_PSW };
            }
            @Override
            public void init() {
            }
        };

        Device haltBitRegister = new Device() {
            int haltBitValue;
            @Override
            public void write(boolean isByteMode, int address, int value) {
                haltBitValue = value;
            }
            @Override
            public void reset() {
            }
            @Override
            public int read(int address) {
                return haltBitValue;
            }
            @Override
            public int[] getAddresses() {
                return new int[] { Cpu.REG_SEL1 };
            }
            @Override
            public void init() {
            }
        };

        computer.addDevice(haltStateRegisters);
        computer.addDevice(haltBitRegister);

        computer.reset();
        assertTrue((computer.getCpu().readMemory(false, Cpu.REG_SEL1) & 014) == 0);
        // HALT
        computer.getCpu().executeSingleInstruction();
        assertEquals(0100010, computer.getCpu().readRegister(false, Cpu.PC));
        assertEquals(0377, computer.getCpu().getPswState());
        assertTrue((computer.getCpu().readMemory(false, Cpu.REG_SEL1) & 004) != 0);
        assertTrue((computer.getCpu().readMemory(false, Cpu.REG_SEL1) & 010) != 0);
        assertEquals(0100002, computer.getCpu().readMemory(false, Cpu.REG_HALT_PC));
        assertEquals(0340, computer.getCpu().readMemory(false, Cpu.REG_HALT_PSW));
        assertTrue(computer.getCpu().isHaltMode());
    }

}