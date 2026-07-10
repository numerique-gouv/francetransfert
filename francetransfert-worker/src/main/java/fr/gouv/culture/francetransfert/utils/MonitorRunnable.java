/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.utils;

import fr.gouv.culture.francetransfert.core.utils.MdcScope;

public class MonitorRunnable implements Runnable {

	private final Runnable runnable;

	private final String queue;

	private final String data;

	private final String enclosureId;

	public Runnable getRunnable() {
		return runnable;
	}

	public String getQueue() {
		return queue;
	}

	public String getData() {
		return data;
	}

	public String getEnclosureId() {
		return enclosureId;
	}

	public MonitorRunnable(Runnable runnable, String queue, String data) {
		this(runnable, queue, data, null);
	}

	public MonitorRunnable(Runnable runnable, String queue, String data, String enclosureId) {
		this.runnable = runnable;
		this.queue = queue;
		this.data = data;
		this.enclosureId = enclosureId;
		WorkerUtils.activeTasks.add(this);
	}

	@Override
	public void run() {
		try (MdcScope ignored = MdcScope.enclosure(enclosureId)) {
			runnable.run();
		} finally {
			WorkerUtils.activeTasks.remove(this);
		}
	}
}
