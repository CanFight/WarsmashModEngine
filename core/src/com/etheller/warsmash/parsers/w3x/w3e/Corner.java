package com.etheller.warsmash.parsers.w3x.w3e;

import java.io.IOException;

import com.etheller.warsmash.util.ParseUtils;
import com.google.common.io.LittleEndianDataInputStream;
import com.google.common.io.LittleEndianDataOutputStream;

/**
 * A tile corner.
 */
public class Corner {
	private int groundHeight;
	private int waterHeight;
	private int mapEdge;
	private int ramp;
	private int blight;
	private int water;
	private int boundary;
	private int groundTexture;
	private int cliffVariation;
	private int groundVariation;
	private int cliffTexture;
	private int layerHeight;

	public void load(final LittleEndianDataInputStream stream) throws IOException {
		this.groundHeight = (stream.readShort() - 8192) / 512;

		final short waterAndEdge = stream.readShort();
		this.waterHeight = ((waterAndEdge & 0x3FFF) - 8192) / 512;
		this.mapEdge = waterAndEdge & 0x4000;

		final short textureAndFlags = ParseUtils.readUInt8(stream);

		this.ramp = textureAndFlags & 0b00010000;
		this.blight = textureAndFlags & 0b00100000;
		this.water = textureAndFlags & 0b01000000;
		this.boundary = textureAndFlags & 0b10000000;

		this.groundTexture = textureAndFlags & 0b00001111;

		final short variation = ParseUtils.readUInt8(stream);

		this.cliffVariation = (variation & 0b11100000) >>> 5;
		this.groundVariation = variation & 0b00011111;

		final short cliffTextureAndLayer = ParseUtils.readUInt8(stream);

		this.cliffTexture = (cliffTextureAndLayer & 0b11110000) >>> 4;
		this.layerHeight = cliffTextureAndLayer & 0b00001111;

	}

	public void save(final LittleEndianDataOutputStream stream) throws IOException {
		stream.writeShort((this.groundHeight * 512) + 8192);
		stream.writeShort((this.waterHeight + 8192 + this.mapEdge) << 14);
		ParseUtils.writeUInt8(stream, (short) ((this.ramp << 4) | (this.blight << 5) | (this.water << 6)
				| (this.boundary << 7) | this.groundTexture));
		ParseUtils.writeUInt8(stream, (short) ((this.cliffVariation << 5) | this.groundVariation));
		ParseUtils.writeUInt8(stream, (short) ((this.cliffTexture << 4) + this.layerHeight));
	}

	public int getGroundHeight() {
		return this.groundHeight;
	}

	public int getWaterHeight() {
		return this.waterHeight;
	}

	public int getMapEdge() {
		return this.mapEdge;
	}

	public int getRamp() {
		return this.ramp;
	}

	public int getBlight() {
		return this.blight;
	}

	public int getWater() {
		return this.water;
	}

	public int getBoundary() {
		return this.boundary;
	}

	public int getGroundTexture() {
		return this.groundTexture;
	}

	public int getCliffVariation() {
		return this.cliffVariation;
	}

	public int getGroundVariation() {
		return this.groundVariation;
	}

	public int getCliffTexture() {
		return this.cliffTexture;
	}

	public int getLayerHeight() {
		return this.layerHeight;
	}
}