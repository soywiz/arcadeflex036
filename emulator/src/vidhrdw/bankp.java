/*
 * ported to v0.36
 * using automatic conversion tool v0.08
 *
 *
 *
 */ 
package vidhrdw;

import static arcadeflex.libc.*;
import static mame.drawgfxH.*;
import static mame.drawgfx.*;
import static vidhrdw.generic.*;
import static mame.driverH.*;
import static mame.osdependH.*;
import static mame.mame.*;
import static arcadeflex.libc_old.*;
import static arcadeflex.video.*;
import static mame.cpuintrf.*;

public class bankp
{
	
	
	
	public static CharPtr bankp_videoram2=new CharPtr();
	public static CharPtr bankp_colorram2=new CharPtr();
	static char[] dirtybuffer2;
	static osd_bitmap tmpbitmap2;
	static int scroll_x;
	static int flipscreen;
	static int priority;
	
	
	
	/***************************************************************************
	
	  Convert the color PROMs into a more useable format.
	
	  Bank Panic has a 32x8 palette PROM (I'm not sure whether the second 16
	  bytes are used - they contain the same colors as the first 16 with only
	  one different) and two 256x4 lookup table PROMs (one for charset #1, one
	  for charset #2 - only the first 128 nibbles seem to be used).
	
	  I don't know for sure how the palette PROM is connected to the RGB output,
	  but it's probably the usual:
	
	  bit 7 -- 220 ohm resistor  -- BLUE
	        -- 470 ohm resistor  -- BLUE
	        -- 220 ohm resistor  -- GREEN
	        -- 470 ohm resistor  -- GREEN
	        -- 1  kohm resistor  -- GREEN
	        -- 220 ohm resistor  -- RED
	        -- 470 ohm resistor  -- RED
	  bit 0 -- 1  kohm resistor  -- RED
	
	***************************************************************************/
        static int TOTAL_COLORS(int gfxn) 
        {
            return Machine.gfx[gfxn].total_colors * Machine.gfx[gfxn].color_granularity;
        }
	public static VhConvertColorPromPtr bankp_vh_convert_color_prom = new VhConvertColorPromPtr() { public void handler(UByte []palette, char []colortable, UBytePtr color_prom) 
	{
		int i;
		//#define TOTAL_COLORS(gfxn) (Machine.gfx[gfxn].total_colors * Machine.gfx[gfxn].color_granularity)
		//#define COLOR(gfxn,offs) (colortable[Machine.drv.gfxdecodeinfo[gfxn].color_codes_start + offs])
	
                int p_inc=0;
		for (i = 0;i < Machine.drv.total_colors;i++)
		{
			int bit0,bit1,bit2;
	
	
			/* red component */
			bit0 = (color_prom.read() >> 0) & 0x01;
			bit1 = (color_prom.read() >> 1) & 0x01;
			bit2 = (color_prom.read() >> 2) & 0x01;
			palette[p_inc++].set((char)(0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2));
			/* green component */
			bit0 = (color_prom.read() >> 3) & 0x01;
			bit1 = (color_prom.read() >> 4) & 0x01;
			bit2 = (color_prom.read() >> 5) & 0x01;
			palette[p_inc++].set((char)(0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2));
			/* blue component */
			bit0 = 0;
			bit1 = (color_prom.read() >> 6) & 0x01;
			bit2 = (color_prom.read() >> 7) & 0x01;
			palette[p_inc++].set((char)(0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2));
	
			color_prom.inc();
		}
	
		/* color_prom now points to the beginning of the lookup table */
	
		/* charset #1 lookup table */
		for (i = 0;i < TOTAL_COLORS(0);i++)
                    colortable[Machine.drv.gfxdecodeinfo[0].color_codes_start + i] = (char) ((color_prom.readinc()) & 0x0f);

	
		color_prom.inc(128);	/* skip the bottom half of the PROM - seems to be not used */
	
		/* charset #2 lookup table */
		for (i = 0;i < TOTAL_COLORS(1);i++)
			colortable[Machine.drv.gfxdecodeinfo[1].color_codes_start + i] = (char) ((color_prom.readinc()) & 0x0f);

		/* the bottom half of the PROM seems to be not used */
	} };
	
	
	
	/***************************************************************************
	
	  Start the video hardware emulation.
	
	***************************************************************************/
	public static VhStartPtr bankp_vh_start = new VhStartPtr() { public int handler() 
	{
		if (generic_vh_start.handler()!= 0)
			return 1;
	
		if ((dirtybuffer2 = new char[videoram_size[0]]) == null)
		{
			generic_vh_stop.handler();
			return 1;
		}
		memset(dirtybuffer2,1,videoram_size[0]);
	
		if ((tmpbitmap2 = osd_create_bitmap(Machine.drv.screen_width,Machine.drv.screen_height)) == null)
		{
			dirtybuffer2=null;
			generic_vh_stop.handler();
			return 1;
		}
	
		return 0;
	} };
	
	
	
	public static WriteHandlerPtr bankp_scroll_w = new WriteHandlerPtr() { public void handler(int offset, int data)
	{
		scroll_x = data;
	} };
	
	
	
	/***************************************************************************
	
	  Stop the video hardware emulation.
	
	***************************************************************************/
	public static VhStopPtr bankp_vh_stop = new VhStopPtr() { public void handler() 
	{
		dirtybuffer2=null;
		osd_free_bitmap(tmpbitmap2);
		generic_vh_stop.handler();
	} };
	
	
	
	public static WriteHandlerPtr bankp_videoram2_w = new WriteHandlerPtr() { public void handler(int offset, int data)
	{
		if (bankp_videoram2.read(offset) != data)
		{
			dirtybuffer2[offset] = 1;
	
			bankp_videoram2.write(offset,data);
		}
	} };
	
	
	
	public static WriteHandlerPtr bankp_colorram2_w = new WriteHandlerPtr() { public void handler(int offset, int data)
	{
		if (bankp_colorram2.read(offset) != data)
		{
			dirtybuffer2[offset] = 1;
	
			bankp_colorram2.write(offset,data);
		}
	} };
	
	
	
	public static WriteHandlerPtr bankp_out_w = new WriteHandlerPtr() { public void handler(int offset, int data)
	{
		/* bits 0-1 are playfield priority */
		/* TODO: understand how this works, currently the only thing I do is */
		/* invert the layer order when priority == 2 */
		priority = data & 0x03;
	
		/* bits 2-3 unknown (2 is used) */
	
		/* bit 4 controls NMI */
		if ((data & 0x10) != 0) interrupt_enable_w.handler(0,1);
		else interrupt_enable_w.handler(0,0);
	
		/* bit 5 controls screen flip */
		if ((data & 0x20) != flipscreen)
		{
			flipscreen = data & 0x20;
			memset(dirtybuffer,1,videoram_size[0]);
			memset(dirtybuffer2,1,videoram_size[0]);
		}
	
		/* bits 6-7 unknown */
	} };
	
	
	
	/***************************************************************************
	
	  Draw the game screen in the given osd_bitmap.
	  Do NOT call osd_update_display() from this function, it will be called by
	  the main emulation engine.
	
	***************************************************************************/
	public static VhUpdatePtr bankp_vh_screenrefresh = new VhUpdatePtr() { public void handler(osd_bitmap bitmap,int full_refresh) 
	{
		int offs;
	
	
		/* for every character in the Video RAM, check if it has been modified */
		/* since last time and update it accordingly. */
		for (offs = videoram_size[0] - 1;offs >= 0;offs--)
		{
			if (dirtybuffer[offs]!=0)
			{
				int sx,sy,flipx;
	
	
				dirtybuffer[offs] = 0;
	
				sx = offs % 32;
				sy = offs / 32;
				flipx = colorram.read(offs) & 0x04;
				if (flipscreen != 0)
				{
					sx = 31 - sx;
					sy = 31 - sy;
					flipx = NOT(flipx);
				}
	
				drawgfx(tmpbitmap,Machine.gfx[0],
						videoram.read(offs) + 256 * ((colorram.read(offs) & 3) >> 0),
						colorram.read(offs) >> 3,
						flipx,flipscreen,
						8*sx,8*sy,
						null,TRANSPARENCY_NONE,0);
			}
	
			if (dirtybuffer2[offs]!=0)
			{
				int sx,sy,flipx;
	
	
				dirtybuffer2[offs] = 0;
	
				sx = offs % 32;
				sy = offs / 32;
				flipx = bankp_colorram2.read(offs) & 0x08;
				if (flipscreen != 0)
				{
					sx = 31 - sx;
					sy = 31 - sy;
					flipx = NOT(flipx);
				}
	
				drawgfx(tmpbitmap2,Machine.gfx[1],
						bankp_videoram2.read(offs) + 256 * (bankp_colorram2.read(offs) & 0x07),
						bankp_colorram2.read(offs) >> 4,
						flipx,flipscreen,
						8*sx,8*sy,
						Machine.drv.visible_area,TRANSPARENCY_NONE,0);
			}
		}
	
	
	
	
		/* copy the temporary bitmaps to the screen */
		{
			int scroll;
	
	
			scroll = -scroll_x;
	
			if (priority == 2)
			{
				copyscrollbitmap(bitmap,tmpbitmap,1,new int[] {scroll},0,null,Machine.drv.visible_area,TRANSPARENCY_NONE,0);
				copybitmap(bitmap,tmpbitmap2,0,0,0,0,Machine.drv.visible_area,TRANSPARENCY_COLOR,0);
			}
			else
			{
				copybitmap(bitmap,tmpbitmap2,0,0,0,0,Machine.drv.visible_area,TRANSPARENCY_NONE,0);
				copyscrollbitmap(bitmap,tmpbitmap,1,new int[]{scroll},0,null,Machine.drv.visible_area,TRANSPARENCY_COLOR,0);
			}
		}
	} };
}
