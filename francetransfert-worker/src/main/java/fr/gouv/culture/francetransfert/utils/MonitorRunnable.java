/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.utils;

public class MonitorRunnable implements Runnable {

	private final Runnable runnable;

	private final String queue;

	private final String data;

	public Runnable getRunnable() {
		return runnable;
	}

	public String getQueue() {
		return queue;
	}

	public String getData() {
		return data;
	}

	public MonitorRunnable(Runnable runnable, String queue, String data) {
		this.runnable = runnable;
		this.queue = queue;
		this.data = data;
		WorkerUtils.activeTasks.add(this);
	}

	@Override
	public void run() {
		try {
			runnable.run();
		} finally {
			WorkerUtils.activeTasks.remove(this);
		}
	}
}
