/***************************************************************************

IREM "M72" sound hardware

used by:

Game                          Year  Sound program ID string
----------------------------  ----  -----------------------
R-Type                        1987  - (earlier version, no sample player)
Battle Chopper / Mr. Heli     1987  Rev 2.20
Vigilante                     1988  Rev 2.20
Ninja Spirit                  1988  Rev 2.20
Image Fight                   1988  Rev 2.20
Legend of Hero Tonma          1989  Rev 2.20
X Multiply                    1989  Rev 2.20
Dragon Breed                  1989  Rev 2.20
Kickle Cubicle                1988  Rev 2.21
Shisensho                     1989  Rev 2.21
R-Type II                     1989  Rev 2.21
Major Title                   1990  Rev 2.21
Daiku no Gensan               1990  Rev 3.14 M81
Hammerin' Harry               1990  Rev 3.15 M81
Ken-Go	                      1991  Rev 3.15 M81
Gallop - Armed Police Unit    1991  Rev 3.15 M72
Pound for Pound               ????  Rev 3.15 M83
Bomber Man World              1992  Rev 3.31 M81
Atomic Punk                   1992  Rev 3.31 M99
Quiz F-1 1,2finish            1992  Rev 3.33 M81
Risky Challenge               1993  Rev 3.34 M81
Shisensho II                  1993  Rev 3.34 M81

***************************************************************************/
/*
 * ported to v0.36
 * using automatic conversion tool v0.10
 *
 *
 *
 */ 
package sndhrdw;
import static mame.timer.*;
import static mame.timerH.*;
import static mame.cpuintrf.*;
import static mame.cpuintrfH.*;
import static mame.driverH.*;
import static mame.sndintrf.*;
import static mame.common.*;
import static mame.commonH.*;
import static sound.dac.*;

public class m72
{
	
	
	/*
	
	  The sound CPU runs in interrup mode 0. IRQ is shared by two sources: the
	  YM2151 (bit 4 of the vector), and the main CPU (bit 5).
	  Since the vector can be changed from different contexts (the YM2151 timer
	  callback, the main CPU context, and the sound CPU context), it's important
	  to accurately arbitrate the changes to avoid out-of-order execution. We do
	  that by handling all vector changes in a single timer callback.
	
	*/
	
	
	
	public static final int	VECTOR_INIT=0;
	public static final int	YM2151_ASSERT=1;
	public static final int	YM2151_CLEAR=2;
	public static final int	Z80_ASSERT=3;
	public static final int Z80_CLEAR=4;

	static int irqvector;
        public static timer_callback setvector_callback = new timer_callback(){ public void handler(int param)
        {
		switch(param)
		{
			case VECTOR_INIT:
				irqvector = 0xff;
				break;
	
			case YM2151_ASSERT:
				irqvector &= 0xef;
				break;
	
			case YM2151_CLEAR:
				irqvector |= 0x10;
				break;
	
			case Z80_ASSERT:
				irqvector &= 0xdf;
				break;
	
			case Z80_CLEAR:
				irqvector |= 0x20;
				break;
		}
	
		cpu_irq_line_vector_w(1,0,irqvector);
		if (irqvector == 0xff)	/* no IRQs pending */
			cpu_set_irq_line(1,0,CLEAR_LINE);
		else	/* IRQ pending */
			cpu_set_irq_line(1,0,ASSERT_LINE);
	}};
	public static InitMachinePtr m72_init_sound = new InitMachinePtr() {
        public void handler() {
		setvector_callback.handler(VECTOR_INIT);
	}};
	public static WriteYmHandlerPtr m72_ym2151_irq_handler = new WriteYmHandlerPtr() { public void handler(int irq)
	{
		if (irq != 0)
			timer_set(TIME_NOW,YM2151_ASSERT,setvector_callback);
		else
			timer_set(TIME_NOW,YM2151_CLEAR,setvector_callback);
	}};
	
	public static WriteHandlerPtr m72_sound_command_w = new WriteHandlerPtr() { public void handler(int offset, int data)
	{
		if (offset == 0)
		{
			soundlatch_w.handler(offset,data);
			timer_set(TIME_NOW,Z80_ASSERT,setvector_callback);
		}
	} };
	
	public static WriteHandlerPtr m72_sound_irq_ack_w = new WriteHandlerPtr() { public void handler(int offset, int data)
	{
		timer_set(TIME_NOW,Z80_CLEAR,setvector_callback);
	} };
	
	
	
	static int sample_addr;
	
	public static void m72_set_sample_start(int start)
	{
		sample_addr = start;
	}
	
	public static WriteHandlerPtr vigilant_sample_addr_w = new WriteHandlerPtr() { public void handler(int offset, int data)
	{
		if (offset == 1)
			sample_addr = (sample_addr & 0x00ff) | ((data << 8) & 0xff00);
		else
			sample_addr = (sample_addr & 0xff00) | ((data << 0) & 0x00ff);
	} };
	
	public static WriteHandlerPtr shisen_sample_addr_w = new WriteHandlerPtr() { public void handler(int offset, int data)
	{
		sample_addr >>= 2;
	
		if (offset == 1)
			sample_addr = (sample_addr & 0x00ff) | ((data << 8) & 0xff00);
		else
			sample_addr = (sample_addr & 0xff00) | ((data << 0) & 0x00ff);
	
		sample_addr <<= 2;
	} };
	
	public static WriteHandlerPtr rtype2_sample_addr_w = new WriteHandlerPtr() { public void handler(int offset, int data)
	{
		sample_addr >>= 5;
	
		if (offset == 1)
			sample_addr = (sample_addr & 0x00ff) | ((data << 8) & 0xff00);
		else
			sample_addr = (sample_addr & 0xff00) | ((data << 0) & 0x00ff);
	
		sample_addr <<= 5;
	} };
	
	public static ReadHandlerPtr m72_sample_r = new ReadHandlerPtr() { public int handler(int offset)
	{
		return memory_region(REGION_SOUND1).read(sample_addr);
	} };
	
	public static WriteHandlerPtr m72_sample_w = new WriteHandlerPtr() { public void handler(int offset, int data)
	{
		DAC_signed_data_w.handler(0,data);
		sample_addr = (sample_addr + 1) & (memory_region_length(REGION_SOUND1) - 1);
	} };
}
