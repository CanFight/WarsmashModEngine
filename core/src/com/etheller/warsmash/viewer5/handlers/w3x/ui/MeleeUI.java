package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import java.io.IOException;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.etheller.warsmash.WarsmashGdxMapGame;
import com.etheller.warsmash.datasources.DataSource;
import com.etheller.warsmash.parsers.fdf.GameUI;
import com.etheller.warsmash.parsers.fdf.datamodel.AnchorDefinition;
import com.etheller.warsmash.parsers.fdf.datamodel.FramePoint;
import com.etheller.warsmash.parsers.fdf.frames.SetPoint;
import com.etheller.warsmash.parsers.fdf.frames.StringFrame;
import com.etheller.warsmash.parsers.fdf.frames.TextureFrame;
import com.etheller.warsmash.parsers.fdf.frames.UIFrame;
import com.etheller.warsmash.parsers.jass.Jass2.RootFrameListener;
import com.etheller.warsmash.util.FastNumberFormat;
import com.etheller.warsmash.viewer5.Scene;
import com.etheller.warsmash.viewer5.handlers.mdx.MdxComplexInstance;
import com.etheller.warsmash.viewer5.handlers.mdx.MdxModel;
import com.etheller.warsmash.viewer5.handlers.w3x.StandSequence;
import com.etheller.warsmash.viewer5.handlers.w3x.War3MapViewer;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.RenderUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitClassification;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CAttackType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CDefenseType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CodeKeyType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.CUnitAttack;

public class MeleeUI {
	private final DataSource dataSource;
	private final Viewport uiViewport;
	private final FreeTypeFontGenerator fontGenerator;
	private final Scene uiScene;
	private final War3MapViewer war3MapViewer;
	private final RootFrameListener rootFrameListener;
	private GameUI rootFrame;
	private UIFrame consoleUI;
	private UIFrame resourceBar;
	private UIFrame timeIndicator;
	private UIFrame unitPortrait;
	private StringFrame unitLifeText;
	private StringFrame unitManaText;
	private Portrait portrait;
	private final Rectangle tempRect = new Rectangle();
	private final Vector2 projectionTemp1 = new Vector2();
	private final Vector2 projectionTemp2 = new Vector2();
	private UIFrame simpleInfoPanelUnitDetail;
	private StringFrame simpleNameValue;
	private StringFrame simpleClassValue;
	private StringFrame simpleBuildingActionLabel;
	private UIFrame attack1Icon;
	private TextureFrame attack1IconBackdrop;
	private StringFrame attack1InfoPanelIconValue;
	private StringFrame attack1InfoPanelIconLevel;
	private UIFrame attack2Icon;
	private TextureFrame attack2IconBackdrop;
	private StringFrame attack2InfoPanelIconValue;
	private StringFrame attack2InfoPanelIconLevel;
	private UIFrame armorIcon;
	private TextureFrame armorIconBackdrop;
	private StringFrame armorInfoPanelIconValue;
	private StringFrame armorInfoPanelIconLevel;
	private InfoPanelIconBackdrops damageBackdrops;
	private InfoPanelIconBackdrops defenseBackdrops;

	public MeleeUI(final DataSource dataSource, final Viewport uiViewport, final FreeTypeFontGenerator fontGenerator,
			final Scene uiScene, final War3MapViewer war3MapViewer, final RootFrameListener rootFrameListener) {
		this.dataSource = dataSource;
		this.uiViewport = uiViewport;
		this.fontGenerator = fontGenerator;
		this.uiScene = uiScene;
		this.war3MapViewer = war3MapViewer;
		this.rootFrameListener = rootFrameListener;

	}

	/**
	 * Called "main" because this was originally written in JASS so that maps could
	 * override it, and I may convert it back to the JASS at some point.
	 */
	public void main() {
		// =================================
		// Load skins and templates
		// =================================
		this.rootFrame = new GameUI(this.dataSource, GameUI.loadSkin(this.dataSource, 1), this.uiViewport,
				this.fontGenerator, this.uiScene, this.war3MapViewer);
		try {
			this.rootFrame.loadTOCFile("UI\\FrameDef\\FrameDef.toc");
		}
		catch (final IOException exc) {
			throw new IllegalStateException("Unable to load FrameDef.toc", exc);
		}
		try {
			this.rootFrame.loadTOCFile("UI\\FrameDef\\SmashFrameDef.toc");
		}
		catch (final IOException exc) {
			throw new IllegalStateException("Unable to load SmashFrameDef.toc", exc);
		}
		this.damageBackdrops = new InfoPanelIconBackdrops(CAttackType.values(), this.rootFrame, "Damage", "Neutral");
		this.defenseBackdrops = new InfoPanelIconBackdrops(CDefenseType.values(), this.rootFrame, "Armor", "Neutral");

		// =================================
		// Load major UI components
		// =================================
		// Console UI is the background with the racial theme
		this.consoleUI = this.rootFrame.createSimpleFrame("ConsoleUI", this.rootFrame, 0);
		this.consoleUI.setSetAllPoints(true);

		// Resource bar is a 3 part bar with Gold, Lumber, and Food.
		// Its template does not specify where to put it, so we must
		// put it in the "TOPRIGHT" corner.
		this.resourceBar = this.rootFrame.createSimpleFrame("ResourceBarFrame", this.consoleUI, 0);
		this.resourceBar.addSetPoint(new SetPoint(FramePoint.TOPRIGHT, this.consoleUI, FramePoint.TOPRIGHT, 0, 0));

		// Create the Time Indicator (clock)
		this.timeIndicator = this.rootFrame.createFrame("TimeOfDayIndicator", this.rootFrame, 0, 0);

		// Create the unit portrait stuff
		this.portrait = new Portrait(this.war3MapViewer);
		positionPortrait();
		this.unitPortrait = this.rootFrame.createSimpleFrame("UnitPortrait", this.consoleUI, 0);
		this.unitLifeText = (StringFrame) this.rootFrame.getFrameByName("UnitPortraitHitPointText", 0);
		this.unitManaText = (StringFrame) this.rootFrame.getFrameByName("UnitPortraitManaPointText", 0);

		this.simpleInfoPanelUnitDetail = this.rootFrame.createSimpleFrame("SimpleInfoPanelUnitDetail", this.consoleUI,
				0);
		this.simpleInfoPanelUnitDetail
				.addAnchor(new AnchorDefinition(FramePoint.BOTTOM, 0, GameUI.convertY(this.uiViewport, 0.0f)));
		this.simpleInfoPanelUnitDetail.setWidth(GameUI.convertY(this.uiViewport, 0.180f));
		this.simpleInfoPanelUnitDetail.setHeight(GameUI.convertY(this.uiViewport, 0.105f));
		this.simpleNameValue = (StringFrame) this.rootFrame.getFrameByName("SimpleNameValue", 0);
		this.simpleClassValue = (StringFrame) this.rootFrame.getFrameByName("SimpleClassValue", 0);
		this.simpleBuildingActionLabel = (StringFrame) this.rootFrame.getFrameByName("SimpleBuildingActionLabel", 0);

		this.attack1Icon = this.rootFrame.createSimpleFrame("SimpleInfoPanelIconDamage", this.simpleInfoPanelUnitDetail,
				0);
		this.attack1Icon.addSetPoint(new SetPoint(FramePoint.TOPLEFT, this.simpleInfoPanelUnitDetail,
				FramePoint.TOPLEFT, 0, GameUI.convertY(this.uiViewport, -0.030125f)));
		this.attack1IconBackdrop = (TextureFrame) this.rootFrame.getFrameByName("InfoPanelIconBackdrop", 0);
		this.attack1InfoPanelIconValue = (StringFrame) this.rootFrame.getFrameByName("InfoPanelIconValue", 0);
		this.attack1InfoPanelIconLevel = (StringFrame) this.rootFrame.getFrameByName("InfoPanelIconLevel", 0);

		this.attack2Icon = this.rootFrame.createSimpleFrame("SimpleInfoPanelIconDamage", this.simpleInfoPanelUnitDetail,
				1);
		this.attack2Icon
				.addSetPoint(new SetPoint(FramePoint.TOPLEFT, this.simpleInfoPanelUnitDetail, FramePoint.TOPLEFT,
						GameUI.convertX(this.uiViewport, 0.1f), GameUI.convertY(this.uiViewport, -0.030125f)));
		this.attack2IconBackdrop = (TextureFrame) this.rootFrame.getFrameByName("InfoPanelIconBackdrop", 1);
		this.attack2InfoPanelIconValue = (StringFrame) this.rootFrame.getFrameByName("InfoPanelIconValue", 1);
		this.attack2InfoPanelIconLevel = (StringFrame) this.rootFrame.getFrameByName("InfoPanelIconLevel", 1);

		this.armorIcon = this.rootFrame.createSimpleFrame("SimpleInfoPanelIconArmor", this.simpleInfoPanelUnitDetail,
				1);
		this.armorIcon.addSetPoint(new SetPoint(FramePoint.TOPLEFT, this.simpleInfoPanelUnitDetail, FramePoint.TOPLEFT,
				GameUI.convertX(this.uiViewport, 0f), GameUI.convertY(this.uiViewport, -0.06025f)));
		this.armorIconBackdrop = (TextureFrame) this.rootFrame.getFrameByName("InfoPanelIconBackdrop", 0);
		this.armorInfoPanelIconValue = (StringFrame) this.rootFrame.getFrameByName("InfoPanelIconValue", 0);
		this.armorInfoPanelIconLevel = (StringFrame) this.rootFrame.getFrameByName("InfoPanelIconLevel", 0);

		this.rootFrame.positionBounds(this.uiViewport);
		selectUnit(null);
	}

	public void updatePortrait() {
		this.portrait.update();
	}

	public void render(final SpriteBatch batch, final BitmapFont font20, final GlyphLayout glyphLayout) {
		this.rootFrame.render(batch, font20, glyphLayout);
	}

	public void portraitTalk() {
		this.portrait.talk();
	}

	private static final class Portrait {
		private MdxComplexInstance modelInstance;
		private final WarsmashGdxMapGame.CameraManager portraitCameraManager;
		private final Scene portraitScene;

		public Portrait(final War3MapViewer war3MapViewer) {
			this.portraitScene = war3MapViewer.addSimpleScene();
			this.portraitCameraManager = new WarsmashGdxMapGame.CameraManager();
			this.portraitCameraManager.setupCamera(this.portraitScene);
			this.portraitScene.camera.viewport(new Rectangle(100, 0, 6400, 48));
		}

		public void update() {
			this.portraitCameraManager.updateCamera();
			if ((this.modelInstance != null)
					&& (this.modelInstance.sequenceEnded || (this.modelInstance.sequence == -1))) {
				StandSequence.randomPortraitSequence(this.modelInstance);
			}
		}

		public void talk() {
			StandSequence.randomPortraitTalkSequence(this.modelInstance);
		}

		public void setSelectedUnit(final RenderUnit unit) {
			if (unit == null) {
				if (this.modelInstance != null) {
					this.portraitScene.removeInstance(this.modelInstance);
				}
				this.modelInstance = null;
				this.portraitCameraManager.setModelInstance(null, null);
			}
			else {
				final MdxModel portraitModel = unit.portraitModel;
				if (portraitModel != null) {
					if (this.modelInstance != null) {
						this.portraitScene.removeInstance(this.modelInstance);
					}
					this.modelInstance = (MdxComplexInstance) portraitModel.addInstance();
					this.portraitCameraManager.setModelInstance(this.modelInstance, portraitModel);
					this.modelInstance.setSequenceLoopMode(1);
					this.modelInstance.setScene(this.portraitScene);
					this.modelInstance.setVertexColor(unit.instance.vertexColor);
					this.modelInstance.setTeamColor(unit.playerIndex);
				}
			}
		}
	}

	public void selectUnit(final RenderUnit unit) {
		this.portrait.setSelectedUnit(unit);
		if (unit == null) {
			this.simpleNameValue.setText("");
			this.unitLifeText.setText("");
			this.unitManaText.setText("");
			this.simpleClassValue.setText("");
			this.simpleBuildingActionLabel.setText("");
			this.attack1Icon.setVisible(false);
			this.attack2Icon.setVisible(false);
			this.attack1InfoPanelIconLevel.setText("");
			this.attack2InfoPanelIconLevel.setText("");
			this.armorIcon.setVisible(false);
			this.armorInfoPanelIconLevel.setText("");
		}
		else {
			this.simpleNameValue.setText(unit.getSimulationUnit().getUnitType().getName());
			String classText = null;
			for (final CUnitClassification classification : unit.getSimulationUnit().getClassifications()) {
				if (classification.getDisplayName() != null) {
					classText = classification.getDisplayName();
				}
			}
			if (classText != null) {
				this.simpleClassValue.setText(classText);
			}
			else {
				this.simpleClassValue.setText("");
			}
			this.unitLifeText.setText(FastNumberFormat.formatWholeNumber(unit.getSimulationUnit().getLife()) + " / "
					+ unit.getSimulationUnit().getMaximumLife());
			final int maximumMana = unit.getSimulationUnit().getMaximumMana();
			if (maximumMana > 0) {
				this.unitManaText.setText(
						FastNumberFormat.formatWholeNumber(unit.getSimulationUnit().getMana()) + " / " + maximumMana);
			}
			else {
				this.unitManaText.setText("");
			}
			this.simpleBuildingActionLabel.setText("");

			if (unit.getSimulationUnit().getUnitType().getAttacks().size() > 0) {
				final CUnitAttack attackOne = unit.getSimulationUnit().getUnitType().getAttacks().get(0);
				this.attack1Icon.setVisible(attackOne.isShowUI());
				this.attack1IconBackdrop.setTexture(this.damageBackdrops.getTexture(attackOne.getAttackType()));
				this.attack1InfoPanelIconValue.setText(attackOne.getMinDamage() + " - " + attackOne.getMaxDamage());
				if (unit.getSimulationUnit().getUnitType().getAttacks().size() > 1) {
					final CUnitAttack attackTwo = unit.getSimulationUnit().getUnitType().getAttacks().get(1);
					this.attack2Icon.setVisible(attackTwo.isShowUI());
					this.attack2IconBackdrop.setTexture(this.damageBackdrops.getTexture(attackTwo.getAttackType()));
					this.attack2InfoPanelIconValue.setText(attackTwo.getMinDamage() + " - " + attackTwo.getMaxDamage());
				}
				else {
					this.attack2Icon.setVisible(false);
				}
			}
			else {
				this.attack1Icon.setVisible(false);
				this.attack2Icon.setVisible(false);
			}

			this.armorIcon.setVisible(true);
			this.armorIconBackdrop.setTexture(
					this.defenseBackdrops.getTexture(unit.getSimulationUnit().getUnitType().getDefenseType()));
			this.armorInfoPanelIconValue.setText(Integer.toString(unit.getSimulationUnit().getDefense()));
		}
	}

	public void resize() {
		positionPortrait();
	}

	public void positionPortrait() {
		this.projectionTemp1.x = 422;
		this.projectionTemp1.y = 57;
		this.projectionTemp2.x = 422 + 167;
		this.projectionTemp2.y = 57 + 170;
		this.uiViewport.project(this.projectionTemp1);
		this.uiViewport.project(this.projectionTemp2);

		this.tempRect.x = this.projectionTemp1.x;
		this.tempRect.y = this.projectionTemp1.y;
		this.tempRect.width = this.projectionTemp2.x - this.projectionTemp1.x;
		this.tempRect.height = this.projectionTemp2.y - this.projectionTemp1.y;
		this.portrait.portraitScene.camera.viewport(this.tempRect);
	}

	private static final class InfoPanelIconBackdrops {
		private final Texture[] damageBackdropTextures;

		public InfoPanelIconBackdrops(final CodeKeyType[] attackTypes, final GameUI gameUI, final String prefix,
				final String suffix) {
			this.damageBackdropTextures = new Texture[attackTypes.length];
			for (int index = 0; index < attackTypes.length; index++) {
				final CodeKeyType attackType = attackTypes[index];
				String skinLookupKey = "InfoPanelIcon" + prefix + attackType.getCodeKey() + suffix;
				try {
					this.damageBackdropTextures[index] = gameUI.loadTexture(gameUI.getSkinField(skinLookupKey));
				}
				catch (final Exception exc) {
					skinLookupKey = "InfoPanelIcon" + prefix + attackType.getCodeKey();
					this.damageBackdropTextures[index] = gameUI.loadTexture(gameUI.getSkinField(skinLookupKey));
				}
			}
		}

		public Texture getTexture(final CodeKeyType attackType) {
			if (attackType != null) {
				final int ordinal = attackType.ordinal();
				if ((ordinal >= 0) && (ordinal < this.damageBackdropTextures.length)) {
					return this.damageBackdropTextures[ordinal];
				}
			}
			return this.damageBackdropTextures[0];
		}

		private static String getSuffix(final CAttackType attackType) {
			switch (attackType) {
			case CHAOS:
				return "Chaos";
			case HERO:
				return "Hero";
			case MAGIC:
				return "Magic";
			case NORMAL:
				return "Normal";
			case PIERCE:
				return "Pierce";
			case SIEGE:
				return "Siege";
			case SPELLS:
				return "Magic";
			case UNKNOWN:
				return "Unknown";
			default:
				throw new IllegalArgumentException("Unknown attack type: " + attackType);
			}

		}
	}
}