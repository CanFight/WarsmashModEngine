package com.etheller.warsmash.viewer5.handlers.w3x.simulation;

import com.etheller.warsmash.util.SubscriberSetNotifier;

public interface CPlayerStateListener {
	void goldChanged();

	void lumberChanged();

	void foodChanged();

	void upkeepChanged();

	public static final class CPlayerStateNotifier extends SubscriberSetNotifier<CPlayerStateListener>
			implements CPlayerStateListener {
		@Override
		public void goldChanged() {
			for (final CPlayerStateListener listener : set) {
				listener.goldChanged();
			}
		}

		@Override
		public void lumberChanged() {
			for (final CPlayerStateListener listener : set) {
				listener.lumberChanged();
			}
		}

		@Override
		public void foodChanged() {
			for (final CPlayerStateListener listener : set) {
				listener.foodChanged();
			}
		}

		@Override
		public void upkeepChanged() {
			for (final CPlayerStateListener listener : set) {
				listener.upkeepChanged();
			}
		}
	}
}