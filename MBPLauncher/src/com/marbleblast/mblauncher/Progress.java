package com.marbleblast.mblauncher;

import java.util.ArrayList;

public class Progress {
	class Region {
		public int count;
		public int current;

		public boolean weighted;
		public float[] weights;
		
		public Region(int count) {
			this.count = count;
			current = 0;
			weighted = false;
		}

		public void setWeight(int section, float weight) {
			if (!weighted) {
				this.weights = new float[this.count];
				for (int i = 0; i < this.count; i ++) {
					this.weights[i] = 1.0f;
				}
				this.weighted = true;
			}
			this.weights[section] = weight;
		}

		public float getSectionStart(int section) {
			if (!this.weighted) {
				return (float)section / (float)count;
			}
			if (section >= this.count) {
				return 1.0f;
			}
			if (section <= 0) {
				return 0.0f;
			}

			float weightSum = 0.0f;
			for (int i = 0; i < section; i ++) {
				weightSum += this.weights[i];
			}
			return weightSum;
		}
	}
	ArrayList<Region> activeRegions;
	
	public Progress() {
		activeRegions = new ArrayList<Progress.Region>();
		startProgressRegion(1);
	}

	/**
	 * Start a region of the progress, with a max section count
	 * @param sections Number of sub-sections in this region
	 */
	public void startProgressRegion(int sections) {
		activeRegions.add(new Region(sections));
	}
	/**
	 * Set the weight of a section in the active progress region
	 * @param section Section index
	 * @param weight Section weight
	 */
	public void setSectionWeight(int section, float weight) {
		activeRegions.get(activeRegions.size() - 1).setWeight(section, weight);
	}
	/**
	 * End a region of the progress, popping the region stack
	 */
	public void endProgressRegion() {
		activeRegions.remove(activeRegions.get(activeRegions.size() - 1));
	}
	/**
	 * Update the progress of the current region of the region stack
	 * @param value New progress for the current region
	 */
	public void updateProgress(int value) {
		activeRegions.get(activeRegions.size() - 1).current = value;
	}
	/**
	 * Get the current progress of this progress stack
	 * @return
	 */
	public float getProgress() {
		float max = 1.0f;
		float min = 0.0f;

		for (int i = 0; i < activeRegions.size(); i ++) {
			Region region = activeRegions.get(i);
			float total = max - min;

			float regionStart = region.getSectionStart(region.current);
			float regionEnd = region.getSectionStart(region.current + 1);

			float current = min + (regionStart * total);
			float next = min + (regionEnd * total);

			min = current;
			max = next;
		}
		return min;
	}
}
